package com.LogicProjector.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Disabled("Requires a dedicated MySQL test database; do not run against the local development database.")
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserWithHashedPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"teacher","password":"secret-pass"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("teacher"))
                .andExpect(jsonPath("$.creditsBalance").value(300))
                .andExpect(jsonPath("$.frozenCreditsBalance").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        UserAccount savedUser = userAccountRepository.findByUsername("teacher").orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("secret-pass");
        assertThat(passwordEncoder.matches("secret-pass", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    void shouldLoginAndReturnJwtTokenAndProfile() throws Exception {
        userAccountRepository.save(new UserAccount(null, "teacher", passwordEncoder.encode("secret-pass"), 300, 0, "ACTIVE"));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"teacher","password":"secret-pass"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.username").value("teacher"))
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("token").asText()).isNotBlank();
    }

    @Test
    void shouldReturnCurrentUserForValidBearerToken() throws Exception {
        userAccountRepository.save(new UserAccount(null, "teacher", passwordEncoder.encode("secret-pass"), 300, 0, "ACTIVE"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"teacher","password":"secret-pass"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("teacher"))
                .andExpect(jsonPath("$.creditsBalance").value(300));
    }

    @Test
    void shouldRejectProtectedEndpointWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/generation-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceCode":"class Demo {}","language":"java"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectRegistrationForOversizedUsername() throws Exception {
        String oversizedUsername = "x".repeat(80);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"secret-pass"}
                                """.formatted(oversizedUsername)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldThrottleRepeatedFailedLoginAttempts() throws Exception {
        userAccountRepository.save(new UserAccount(null, "teacher", passwordEncoder.encode("secret-pass"), 300, 0, "ACTIVE"));

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"teacher","password":"wrong-pass"}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("INVALID_CREDENTIALS"));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"teacher","password":"secret-pass"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("TOO_MANY_LOGIN_ATTEMPTS"));
    }
}

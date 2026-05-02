package com.LogicProjector.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class AuthenticatedUsersTest {

    @Test
    void shouldReturnAuthenticatedUserPrincipal() {
        AuthenticatedUser user = new AuthenticatedUser(7L, "teacher");

        AuthenticatedUser result = AuthenticatedUsers.current(new UsernamePasswordAuthenticationToken(user, null));

        assertThat(result).isSameAs(user);
    }

    @Test
    void shouldRejectMissingAuthenticatedUserPrincipal() {
        assertThatThrownBy(() -> AuthenticatedUsers.current(null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

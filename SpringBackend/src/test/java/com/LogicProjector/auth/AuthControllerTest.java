package com.LogicProjector.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.server.ResponseStatusException;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final AuthController authController = new AuthController(authService);

    @Test
    void shouldRejectMeWithoutAuthentication() {
        assertThatThrownBy(() -> authController.me(null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturnCurrentUserForAuthenticatedPrincipal() {
        AuthenticatedUser user = new AuthenticatedUser(7L, "teacher");
        UserProfileResponse profile = new UserProfileResponse(7L, "teacher", 300, 0, "ACTIVE");
        when(authService.me(user)).thenReturn(profile);

        UserProfileResponse response = authController.me(new UsernamePasswordAuthenticationToken(
                user,
                null,
                AuthorityUtils.NO_AUTHORITIES));

        assertThat(response).isEqualTo(profile);
        verify(authService).me(user);
    }
}

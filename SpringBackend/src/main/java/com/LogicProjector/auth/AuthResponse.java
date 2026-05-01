package com.LogicProjector.auth;

public record AuthResponse(
        String token,
        UserProfileResponse user
) {
}

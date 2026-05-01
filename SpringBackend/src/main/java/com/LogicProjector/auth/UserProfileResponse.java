package com.LogicProjector.auth;

public record UserProfileResponse(
        Long id,
        String username,
        Integer creditsBalance,
        Integer frozenCreditsBalance,
        String status
) {
}

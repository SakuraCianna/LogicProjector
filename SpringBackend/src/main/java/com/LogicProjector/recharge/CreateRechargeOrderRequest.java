package com.LogicProjector.recharge;

import jakarta.validation.constraints.NotBlank;

public record CreateRechargeOrderRequest(
        @NotBlank String packageCode
) {
}

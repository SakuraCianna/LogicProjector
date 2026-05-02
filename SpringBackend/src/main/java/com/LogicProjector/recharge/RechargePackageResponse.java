package com.LogicProjector.recharge;

public record RechargePackageResponse(
        String code,
        String name,
        int credits,
        int amountCents,
        String description
) {
}

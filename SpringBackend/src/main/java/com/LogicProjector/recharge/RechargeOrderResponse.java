package com.LogicProjector.recharge;

import java.time.LocalDateTime;

public record RechargeOrderResponse(
        Long id,
        String packageCode,
        String packageName,
        int credits,
        int amountCents,
        String status,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {
}

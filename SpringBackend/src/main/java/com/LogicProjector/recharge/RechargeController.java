package com.LogicProjector.recharge;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.LogicProjector.auth.AuthenticatedUsers;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/recharge")
public class RechargeController {

    private final RechargeService rechargeService;

    public RechargeController(RechargeService rechargeService) {
        this.rechargeService = rechargeService;
    }

    @GetMapping("/packages")
    public List<RechargePackageResponse> packages() {
        return rechargeService.packages();
    }

    @GetMapping("/orders/recent")
    public List<RechargeOrderResponse> recentOrders(Authentication authentication) {
        return rechargeService.recentOrders(AuthenticatedUsers.current(authentication).userId());
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public RechargeOrderResponse createOrder(@Valid @RequestBody CreateRechargeOrderRequest request,
            Authentication authentication) {
        return rechargeService.createOrder(AuthenticatedUsers.current(authentication).userId(), request.packageCode());
    }

    @PostMapping("/orders/{orderId}/simulate-payment")
    public RechargeOrderResponse simulatePayment(@PathVariable Long orderId, Authentication authentication) {
        return rechargeService.simulatePayment(AuthenticatedUsers.current(authentication).userId(), orderId);
    }
}

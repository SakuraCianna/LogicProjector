package com.LogicProjector.recharge;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.billing.BillingService;

@Service
public class RechargeService {

    private static final List<RechargePackageResponse> PACKAGES = List.of(
            new RechargePackageResponse("starter", "基础包", 100, 990, "适合体验生成和短视频导出"),
            new RechargePackageResponse("teacher", "教师包", 600, 4990, "适合一周课堂备课使用"),
            new RechargePackageResponse("studio", "工作室包", 2000, 12900, "适合高频生成和批量导出")
    );

    private final UserAccountRepository userAccountRepository;
    private final RechargeOrderRepository rechargeOrderRepository;
    private final BillingService billingService;

    public RechargeService(UserAccountRepository userAccountRepository, RechargeOrderRepository rechargeOrderRepository,
            BillingService billingService) {
        this.userAccountRepository = userAccountRepository;
        this.rechargeOrderRepository = rechargeOrderRepository;
        this.billingService = billingService;
    }

    public List<RechargePackageResponse> packages() {
        return PACKAGES;
    }

    public List<RechargeOrderResponse> recentOrders(Long userId) {
        return rechargeOrderRepository.findRecentByUserId(userId, PageRequest.of(0, 20)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RechargeOrderResponse createOrder(Long userId, String packageCode) {
        RechargePackageResponse selectedPackage = findPackage(packageCode);
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        RechargeOrder order = rechargeOrderRepository.save(new RechargeOrder(
                null,
                user,
                selectedPackage.code(),
                selectedPackage.name(),
                selectedPackage.credits(),
                selectedPackage.amountCents(),
                "PENDING",
                LocalDateTime.now(),
                null
        ));
        return toResponse(order);
    }

    @Transactional
    public RechargeOrderResponse simulatePayment(Long userId, Long orderId) {
        RechargeOrder order = rechargeOrderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("RECHARGE_ORDER_NOT_FOUND"));
        if (order.isPaid()) {
            return toResponse(order);
        }

        UserAccount user = userAccountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        user.creditCredits(order.getCredits());
        order.markPaid();
        billingService.recordRecharge(user, order.getCredits(), order.getId());
        return toResponse(order);
    }

    private RechargePackageResponse findPackage(String packageCode) {
        return PACKAGES.stream()
                .filter(rechargePackage -> rechargePackage.code().equals(packageCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("RECHARGE_PACKAGE_NOT_FOUND"));
    }

    private RechargeOrderResponse toResponse(RechargeOrder order) {
        return new RechargeOrderResponse(
                order.getId(),
                order.getPackageCode(),
                order.getPackageName(),
                order.getCredits(),
                order.getAmountCents(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getPaidAt()
        );
    }
}

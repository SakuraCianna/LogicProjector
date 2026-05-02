package com.LogicProjector.recharge;

import java.time.LocalDateTime;

import com.LogicProjector.account.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recharge_orders")
public class RechargeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false)
    private String packageCode;

    @Column(nullable = false)
    private String packageName;

    @Column(nullable = false)
    private Integer credits;

    @Column(nullable = false)
    private Integer amountCents;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    protected RechargeOrder() {
    }

    public RechargeOrder(Long id, UserAccount user, String packageCode, String packageName, Integer credits,
            Integer amountCents, String status, LocalDateTime createdAt, LocalDateTime paidAt) {
        this.id = id;
        this.user = user;
        this.packageCode = packageCode;
        this.packageName = packageName;
        this.credits = credits;
        this.amountCents = amountCents;
        this.status = status;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getPackageCode() {
        return packageCode;
    }

    public String getPackageName() {
        return packageName;
    }

    public Integer getCredits() {
        return credits;
    }

    public Integer getAmountCents() {
        return amountCents;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public boolean isPaid() {
        return "PAID".equals(status);
    }

    public void markPaid() {
        if (isPaid()) {
            return;
        }
        status = "PAID";
        paidAt = LocalDateTime.now();
    }
}

package com.LogicProjector.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private Integer creditsBalance;

    @Column(nullable = false)
    private Integer frozenCreditsBalance;

    @Column(nullable = false)
    private String status;

    protected UserAccount() {
    }

    public UserAccount(Long id, String username, String passwordHash, Integer creditsBalance, Integer frozenCreditsBalance, String status) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.creditsBalance = creditsBalance;
        this.frozenCreditsBalance = frozenCreditsBalance;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Integer getCreditsBalance() {
        return creditsBalance;
    }

    public Integer getFrozenCreditsBalance() {
        return frozenCreditsBalance;
    }

    public String getStatus() {
        return status;
    }

    public String getEmail() {
        return username;
    }

    public void debitCredits(int amount) {
        requirePositiveAmount(amount);
        if (creditsBalance < amount) {
            throw new IllegalStateException("Insufficient credits to debit: " + amount);
        }
        this.creditsBalance = this.creditsBalance - amount;
    }

    public void freezeCredits(int amount) {
        requirePositiveAmount(amount);
        if (creditsBalance < amount) {
            throw new IllegalStateException("Insufficient credits to freeze: " + amount);
        }
        this.creditsBalance -= amount;
        this.frozenCreditsBalance += amount;
    }

    public void settleFrozenCredits(int actualCharge, int frozenAmount) {
        if (actualCharge < 0) {
            throw new IllegalArgumentException("Credit amount must be non-negative");
        }
        requirePositiveAmount(frozenAmount);
        if (frozenCreditsBalance < frozenAmount) {
            throw new IllegalStateException("Insufficient frozen credits to settle: " + frozenAmount);
        }
        if (actualCharge > frozenAmount && creditsBalance < actualCharge - frozenAmount) {
            throw new IllegalStateException("Insufficient credits to settle: " + actualCharge);
        }

        this.frozenCreditsBalance -= frozenAmount;
        if (frozenAmount > actualCharge) {
            this.creditsBalance += frozenAmount - actualCharge;
        } else if (actualCharge > frozenAmount) {
            this.creditsBalance -= actualCharge - frozenAmount;
        }
    }

    public void releaseFrozenCredits(int amount) {
        requirePositiveAmount(amount);
        if (frozenCreditsBalance < amount) {
            throw new IllegalStateException("Insufficient frozen credits to release: " + amount);
        }
        this.frozenCreditsBalance -= amount;
        this.creditsBalance += amount;
    }

    private void requirePositiveAmount(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
    }
}

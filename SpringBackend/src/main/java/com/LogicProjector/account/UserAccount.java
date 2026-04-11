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

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Integer creditsBalance;

    @Column(nullable = false)
    private String status;

    protected UserAccount() {
    }

    public UserAccount(Long id, String email, Integer creditsBalance, String status) {
        this.id = id;
        this.email = email;
        this.creditsBalance = creditsBalance;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Integer getCreditsBalance() {
        return creditsBalance;
    }

    public String getStatus() {
        return status;
    }

    public void debitCredits(int amount) {
        this.creditsBalance = this.creditsBalance - amount;
    }
}

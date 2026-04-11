package com.LogicProjector.billing;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.generation.GenerationTask;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "billing_records")
public class BillingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private GenerationTask task;

    @Column(nullable = false)
    private String changeType;

    @Column(nullable = false)
    private Integer creditsDelta;

    @Column(nullable = false)
    private Integer balanceAfter;

    @Column(nullable = false)
    private String description;

    protected BillingRecord() {
    }

    public BillingRecord(Long id, UserAccount user, GenerationTask task, String changeType, Integer creditsDelta,
            Integer balanceAfter, String description) {
        this.id = id;
        this.user = user;
        this.task = task;
        this.changeType = changeType;
        this.creditsDelta = creditsDelta;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }
}

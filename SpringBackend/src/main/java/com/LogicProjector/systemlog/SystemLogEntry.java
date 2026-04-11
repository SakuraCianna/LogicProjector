package com.LogicProjector.systemlog;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_logs")
public class SystemLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long taskId;

    @Column
    private Long userId;

    @Column(nullable = false)
    private String logLevel;

    @Column(nullable = false)
    private String module;

    @Column(nullable = false)
    private String message;

    @Lob
    @Column
    private String details;

    @Column(nullable = false)
    private Instant createdAt;

    protected SystemLogEntry() {
    }

    public SystemLogEntry(Long id, Long taskId, Long userId, String logLevel, String module, String message,
            String details, Instant createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.userId = userId;
        this.logLevel = logLevel;
        this.module = module;
        this.message = message;
        this.details = details;
        this.createdAt = createdAt;
    }
}

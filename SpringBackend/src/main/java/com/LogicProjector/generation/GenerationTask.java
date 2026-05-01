package com.LogicProjector.generation;

import java.time.Instant;

import com.LogicProjector.account.UserAccount;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "generation_tasks")
public class GenerationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Lob
    @Column(nullable = false)
    private String sourceCode;

    @Column(nullable = false)
    private String language;

    @Column
    private String detectedAlgorithm;

    @Column
    private Double confidenceScore;

    @Lob
    @Column(name = "visualization_payload_json")
    private String visualizationPayloadJson;

    @Column
    private String summary;

    @Column
    private String errorMessage;

    @Column(nullable = false)
    private Integer retryCount;

    @Column
    private Instant lastProcessedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationTaskStatus status;

    protected GenerationTask() {
    }

    public static GenerationTask pending(UserAccount user, String sourceCode, String language) {
        GenerationTask task = new GenerationTask();
        task.user = user;
        task.sourceCode = sourceCode;
        task.language = language;
        task.status = GenerationTaskStatus.PENDING;
        task.retryCount = 0;
        return task;
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getLanguage() {
        return language;
    }

    public GenerationTaskStatus getStatus() {
        return status;
    }

    public String getDetectedAlgorithm() {
        return detectedAlgorithm;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public String getVisualizationPayloadJson() {
        return visualizationPayloadJson;
    }

    public String getSummary() {
        return summary;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public Instant getLastProcessedAt() {
        return lastProcessedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markAnalyzing() {
        this.status = GenerationTaskStatus.ANALYZING;
        this.retryCount = this.retryCount + 1;
        this.lastProcessedAt = Instant.now();
    }

    public void complete(String detectedAlgorithm, double confidenceScore, JsonNode visualizationPayload, String summary) {
        this.detectedAlgorithm = detectedAlgorithm;
        this.confidenceScore = confidenceScore;
        this.visualizationPayloadJson = visualizationPayload.toString();
        this.summary = summary;
        this.status = GenerationTaskStatus.COMPLETED;
        this.errorMessage = null;
        this.lastProcessedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = GenerationTaskStatus.FAILED;
        this.lastProcessedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

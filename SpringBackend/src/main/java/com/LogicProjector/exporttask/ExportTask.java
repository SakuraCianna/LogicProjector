package com.LogicProjector.exporttask;

import java.time.Instant;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.generation.GenerationTask;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "export_tasks")
public class ExportTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "generation_task_id", nullable = false)
    private GenerationTask generationTask;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExportTaskStatus status;

    @Column(nullable = false)
    private Integer progress;

    @Column(nullable = false)
    private Integer creditsFrozen;

    @Column
    private Integer creditsCharged;

    @Column
    private String videoPath;

    @Column
    private String subtitlePath;

    @Column
    private String audioPath;

    @Column
    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ExportTask() {
    }

    public static ExportTask pending(GenerationTask generationTask, UserAccount user, int creditsFrozen) {
        ExportTask exportTask = new ExportTask();
        exportTask.generationTask = generationTask;
        exportTask.user = user;
        exportTask.status = ExportTaskStatus.PENDING;
        exportTask.progress = 0;
        exportTask.creditsFrozen = creditsFrozen;
        return exportTask;
    }

    public Long getId() {
        return id;
    }

    public GenerationTask getGenerationTask() {
        return generationTask;
    }

    public UserAccount getUser() {
        return user;
    }

    public ExportTaskStatus getStatus() {
        return status;
    }

    public Integer getProgress() {
        return progress;
    }

    public Integer getCreditsFrozen() {
        return creditsFrozen;
    }

    public Integer getCreditsCharged() {
        return creditsCharged;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public String getSubtitlePath() {
        return subtitlePath;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markProcessing() {
        this.status = ExportTaskStatus.PROCESSING;
        this.progress = 15;
    }

    public void complete(String videoPath, String subtitlePath, String audioPath, int creditsCharged) {
        this.status = ExportTaskStatus.COMPLETED;
        this.progress = 100;
        this.videoPath = videoPath;
        this.subtitlePath = subtitlePath;
        this.audioPath = audioPath;
        this.creditsCharged = creditsCharged;
        this.errorMessage = null;
    }

    public void fail(String errorMessage) {
        this.status = ExportTaskStatus.FAILED;
        this.progress = 100;
        this.errorMessage = errorMessage;
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

package com.example.campaigntouch.touch;

import com.example.campaigntouch.campaign.Campaign;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "touch_task")
public class TouchTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audience_rule_json", nullable = false, columnDefinition = "jsonb")
    private AudienceRule audienceRule;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "channels_json", nullable = false, columnDefinition = "jsonb")
    private List<TouchChannel> channels;

    @Column(name = "message_title", nullable = false, length = 200)
    private String messageTitle;

    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TouchTaskStatus status;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TouchTask() {
    }

    private TouchTask(Campaign campaign, String taskName, AudienceRule audienceRule, List<TouchChannel> channels,
            String messageTitle, String messageContent) {
        this.campaign = campaign;
        this.taskName = taskName;
        this.audienceRule = audienceRule;
        this.channels = List.copyOf(channels);
        this.messageTitle = messageTitle;
        this.messageContent = messageContent;
        this.status = TouchTaskStatus.PENDING;
    }

    public static TouchTask pending(Campaign campaign, String taskName, AudienceRule audienceRule,
            List<TouchChannel> channels, String messageTitle, String messageContent) {
        return new TouchTask(campaign, taskName, audienceRule, channels, messageTitle, messageContent);
    }

    public void markProcessing() {
        status = TouchTaskStatus.PROCESSING;
        updatedAt = LocalDateTime.now();
    }

    public void markCompleted(LocalDateTime executedAt) {
        status = TouchTaskStatus.COMPLETED;
        this.executedAt = executedAt;
        updatedAt = executedAt;
    }

    public void markFailed() {
        status = TouchTaskStatus.FAILED;
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public String getTaskName() {
        return taskName;
    }

    public AudienceRule getAudienceRule() {
        return audienceRule;
    }

    public List<TouchChannel> getChannels() {
        return channels;
    }

    public String getMessageTitle() {
        return messageTitle;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public TouchTaskStatus getStatus() {
        return status;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

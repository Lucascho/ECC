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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "touch_delivery",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_touch_delivery_task_member_channel",
                columnNames = { "touch_task_id", "member_id", "channel" }
        )
)
public class TouchDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "touch_task_id", nullable = false)
    private TouchTask touchTask;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "member_id", nullable = false, length = 100)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TouchChannel channel;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TouchDeliveryStatus status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @Column(name = "failed_reason", length = 500)
    private String failedReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TouchDelivery() {
    }

    private TouchDelivery(TouchTask touchTask, String memberId, TouchChannel channel, String title, String content) {
        this.touchTask = touchTask;
        this.campaign = touchTask.getCampaign();
        this.memberId = memberId;
        this.channel = channel;
        this.title = title;
        this.content = content;
        this.status = TouchDeliveryStatus.PENDING;
    }

    public static TouchDelivery pending(TouchTask touchTask, String memberId, TouchChannel channel, String title,
            String content) {
        return new TouchDelivery(touchTask, memberId, channel, title, content);
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

    public void applyResult(DeliveryResult result, LocalDateTime occurredAt) {
        status = result.status();
        if (result.status() == TouchDeliveryStatus.SENT) {
            sentAt = occurredAt;
            failedReason = null;
        } else if (result.status() == TouchDeliveryStatus.FAILED) {
            failedReason = result.failedReason();
        }
        updatedAt = occurredAt;
    }

    public void markClicked(LocalDateTime clickedAt) {
        status = TouchDeliveryStatus.CLICKED;
        this.clickedAt = clickedAt;
        updatedAt = clickedAt;
    }

    public Long getId() {
        return id;
    }

    public TouchTask getTouchTask() {
        return touchTask;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public String getMemberId() {
        return memberId;
    }

    public TouchChannel getChannel() {
        return channel;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public TouchDeliveryStatus getStatus() {
        return status;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getClickedAt() {
        return clickedAt;
    }

    public String getFailedReason() {
        return failedReason;
    }
}

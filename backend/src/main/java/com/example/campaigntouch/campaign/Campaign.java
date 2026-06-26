package com.example.campaigntouch.campaign;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CampaignType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "landing_page_url", length = 500)
    private String landingPageUrl;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Campaign() {
    }

    private Campaign(String name, CampaignType type, String description, String landingPageUrl,
            LocalDateTime startTime, LocalDateTime endTime, CampaignStatus status, String createdBy) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.landingPageUrl = landingPageUrl;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.createdBy = createdBy;
    }

    public static Campaign draft(String name, CampaignType type, String description, String landingPageUrl,
            LocalDateTime startTime, LocalDateTime endTime, String createdBy) {
        return new Campaign(name, type, description, landingPageUrl, startTime, endTime, CampaignStatus.DRAFT,
                createdBy);
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

    public void updateAll(String name, CampaignType type, String description, String landingPageUrl,
            LocalDateTime startTime, LocalDateTime endTime) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.landingPageUrl = landingPageUrl;
        this.startTime = startTime;
        this.endTime = endTime;
        touchUpdatedAt();
    }

    public void updateActiveEditableFields(String description, String landingPageUrl) {
        this.description = description;
        this.landingPageUrl = landingPageUrl;
        touchUpdatedAt();
    }

    public void changeStatus(CampaignStatus status) {
        this.status = status;
        touchUpdatedAt();
    }

    private void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CampaignType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getLandingPageUrl() {
        return landingPageUrl;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

package com.example.campaigntouch.message;

import com.example.campaigntouch.campaign.Campaign;
import com.example.campaigntouch.touch.TouchDelivery;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_message",
        uniqueConstraints = @UniqueConstraint(name = "uq_member_message_delivery", columnNames = "delivery_id")
)
public class MemberMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private TouchDelivery delivery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "member_id", nullable = false, length = 100)
    private String memberId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected MemberMessage() {
    }

    private MemberMessage(TouchDelivery delivery) {
        this.delivery = delivery;
        this.campaign = delivery.getCampaign();
        this.memberId = delivery.getMemberId();
        this.title = delivery.getTitle();
        this.content = delivery.getContent();
        this.read = false;
    }

    public static MemberMessage fromDelivery(TouchDelivery delivery) {
        return new MemberMessage(delivery);
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public TouchDelivery getDelivery() {
        return delivery;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getClickedAt() {
        return clickedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void click(LocalDateTime clickedAt) {
        this.read = true;
        this.clickedAt = clickedAt;
    }
}

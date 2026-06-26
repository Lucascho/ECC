package com.example.campaigntouch.analytics;

import com.example.campaigntouch.campaign.Campaign;
import com.example.campaigntouch.touch.TouchChannel;
import com.example.campaigntouch.touch.TouchDelivery;
import com.example.campaigntouch.touch.TouchTask;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "campaign_event",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_campaign_event_delivery_type",
                columnNames = { "delivery_id", "event_type" }
        )
)
public class CampaignEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "touch_task_id", nullable = false)
    private TouchTask touchTask;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private TouchDelivery delivery;

    @Column(name = "member_id", nullable = false, length = 100)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private CampaignEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TouchChannel channel;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    protected CampaignEvent() {
    }

    private CampaignEvent(TouchDelivery delivery, CampaignEventType eventType, LocalDateTime occurredAt) {
        this.campaign = delivery.getCampaign();
        this.touchTask = delivery.getTouchTask();
        this.delivery = delivery;
        this.memberId = delivery.getMemberId();
        this.eventType = eventType;
        this.channel = delivery.getChannel();
        this.occurredAt = occurredAt;
    }

    public static CampaignEvent fromDelivery(TouchDelivery delivery, CampaignEventType eventType,
            LocalDateTime occurredAt) {
        return new CampaignEvent(delivery, eventType, occurredAt);
    }

    public CampaignEventType getEventType() {
        return eventType;
    }
}

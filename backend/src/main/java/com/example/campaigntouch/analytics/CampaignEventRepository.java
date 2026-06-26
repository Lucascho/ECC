package com.example.campaigntouch.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignEventRepository extends JpaRepository<CampaignEvent, Long> {

    boolean existsByDeliveryIdAndEventType(Long deliveryId, CampaignEventType eventType);
}

package com.example.campaigntouch.touch;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouchDeliveryRepository extends JpaRepository<TouchDelivery, Long> {

    long countDistinctMemberIdByCampaignId(Long campaignId);

    long countByCampaignId(Long campaignId);

    long countByCampaignIdAndStatus(Long campaignId, TouchDeliveryStatus status);

    long countByCampaignIdAndStatusIn(Long campaignId, Collection<TouchDeliveryStatus> statuses);

    long countByCampaignIdAndChannelAndStatusIn(Long campaignId, TouchChannel channel,
            Collection<TouchDeliveryStatus> statuses);

    long countByCampaignIdAndChannelAndStatus(Long campaignId, TouchChannel channel, TouchDeliveryStatus status);
}

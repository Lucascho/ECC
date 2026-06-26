package com.example.campaigntouch.analytics;

import com.example.campaigntouch.campaign.Campaign;
import com.example.campaigntouch.campaign.CampaignRepository;
import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import com.example.campaigntouch.touch.TouchChannel;
import com.example.campaigntouch.touch.TouchDeliveryRepository;
import com.example.campaigntouch.touch.TouchDeliveryStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignAnalyticsService {

    private static final List<TouchDeliveryStatus> SENT_STATUSES = List.of(
            TouchDeliveryStatus.SENT,
            TouchDeliveryStatus.CLICKED
    );

    private final CampaignRepository campaignRepository;
    private final TouchDeliveryRepository touchDeliveryRepository;

    public CampaignAnalyticsService(CampaignRepository campaignRepository,
            TouchDeliveryRepository touchDeliveryRepository) {
        this.campaignRepository = campaignRepository;
        this.touchDeliveryRepository = touchDeliveryRepository;
    }

    @Transactional(readOnly = true)
    public CampaignAnalyticsResponse getAnalytics(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAMPAIGN_NOT_FOUND));

        long targetMemberCount = touchDeliveryRepository.countDistinctMemberIdByCampaignId(campaignId);
        long deliveryCount = touchDeliveryRepository.countByCampaignId(campaignId);
        long sentCount = touchDeliveryRepository.countByCampaignIdAndStatusIn(campaignId, SENT_STATUSES);
        long failedCount = touchDeliveryRepository.countByCampaignIdAndStatus(campaignId, TouchDeliveryStatus.FAILED);
        long inAppSentCount = touchDeliveryRepository.countByCampaignIdAndChannelAndStatusIn(
                campaignId,
                TouchChannel.IN_APP,
                SENT_STATUSES
        );
        long clickCount = touchDeliveryRepository.countByCampaignIdAndChannelAndStatus(
                campaignId,
                TouchChannel.IN_APP,
                TouchDeliveryStatus.CLICKED
        );
        double clickThroughRate = inAppSentCount == 0 ? 0.0 : (double) clickCount / inAppSentCount;

        return new CampaignAnalyticsResponse(
                campaign.getId(),
                campaign.getName(),
                targetMemberCount,
                deliveryCount,
                sentCount,
                failedCount,
                inAppSentCount,
                clickCount,
                clickThroughRate
        );
    }
}

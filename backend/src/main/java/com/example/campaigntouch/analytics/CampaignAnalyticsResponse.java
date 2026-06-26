package com.example.campaigntouch.analytics;

public record CampaignAnalyticsResponse(
        Long campaignId,
        String campaignName,
        long targetMemberCount,
        long deliveryCount,
        long sentCount,
        long failedCount,
        long inAppSentCount,
        long clickCount,
        double clickThroughRate
) {
}

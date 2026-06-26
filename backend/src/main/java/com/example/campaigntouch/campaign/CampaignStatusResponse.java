package com.example.campaigntouch.campaign;

import java.time.LocalDateTime;

public record CampaignStatusResponse(
        Long id,
        CampaignStatus status,
        LocalDateTime updatedAt
) {
    public static CampaignStatusResponse from(Campaign campaign) {
        return new CampaignStatusResponse(campaign.getId(), campaign.getStatus(), campaign.getUpdatedAt());
    }
}

package com.example.campaigntouch.analytics;

import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CampaignAnalyticsController {

    private final CampaignAnalyticsService campaignAnalyticsService;

    public CampaignAnalyticsController(CampaignAnalyticsService campaignAnalyticsService) {
        this.campaignAnalyticsService = campaignAnalyticsService;
    }

    @GetMapping("/api/admin/campaigns/{campaignId}/analytics")
    public CampaignAnalyticsResponse getAnalytics(
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser,
            @PathVariable Long campaignId
    ) {
        requireAdminUser(adminUser);
        return campaignAnalyticsService.getAnalytics(campaignId);
    }

    private void requireAdminUser(String adminUser) {
        if (adminUser == null || adminUser.isBlank()) {
            throw new BusinessException(ErrorCode.ADMIN_USER_REQUIRED);
        }
    }
}

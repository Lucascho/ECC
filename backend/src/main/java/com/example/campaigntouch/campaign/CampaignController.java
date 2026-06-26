package com.example.campaigntouch.campaign;

import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignResponse create(
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser,
            @Valid @RequestBody CampaignRequest request
    ) {
        return campaignService.create(request, requireAdminUser(adminUser));
    }

    @GetMapping
    public List<CampaignListResponse> list(
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser,
            @RequestParam(required = false) CampaignStatus status,
            @RequestParam(required = false) CampaignType type
    ) {
        requireAdminUser(adminUser);
        return campaignService.list(status, type);
    }

    @GetMapping("/{campaignId}")
    public CampaignResponse get(
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser,
            @PathVariable Long campaignId
    ) {
        requireAdminUser(adminUser);
        return campaignService.get(campaignId);
    }

    @PutMapping("/{campaignId}")
    public CampaignResponse update(
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser,
            @PathVariable Long campaignId,
            @Valid @RequestBody CampaignRequest request
    ) {
        requireAdminUser(adminUser);
        return campaignService.update(campaignId, request);
    }

    @PostMapping("/{campaignId}/activate")
    public CampaignStatusResponse activate(
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser,
            @PathVariable Long campaignId
    ) {
        requireAdminUser(adminUser);
        return campaignService.activate(campaignId);
    }

    @PostMapping("/{campaignId}/pause")
    public CampaignStatusResponse pause(
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser,
            @PathVariable Long campaignId
    ) {
        requireAdminUser(adminUser);
        return campaignService.pause(campaignId);
    }

    @PostMapping("/{campaignId}/end")
    public CampaignStatusResponse end(
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser,
            @PathVariable Long campaignId
    ) {
        requireAdminUser(adminUser);
        return campaignService.end(campaignId);
    }

    private String requireAdminUser(String adminUser) {
        if (adminUser == null || adminUser.isBlank()) {
            throw new BusinessException(ErrorCode.ADMIN_USER_REQUIRED);
        }
        return adminUser;
    }
}

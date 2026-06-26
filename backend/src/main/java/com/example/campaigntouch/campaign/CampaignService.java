package com.example.campaigntouch.campaign;

import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final CampaignRepository campaignRepository;

    public CampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Transactional
    public CampaignResponse create(CampaignRequest request, String adminUser) {
        validateTimeRange(request);
        Campaign campaign = Campaign.draft(
                request.name(),
                request.type(),
                request.description(),
                request.landingPageUrl(),
                request.startTime(),
                request.endTime(),
                adminUser
        );
        return CampaignResponse.from(campaignRepository.save(campaign));
    }

    @Transactional(readOnly = true)
    public List<CampaignListResponse> list(CampaignStatus status, CampaignType type) {
        return findCampaigns(status, type).stream()
                .map(CampaignListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CampaignResponse get(Long campaignId) {
        return CampaignResponse.from(findById(campaignId));
    }

    @Transactional
    public CampaignResponse update(Long campaignId, CampaignRequest request) {
        Campaign campaign = findById(campaignId);
        if (campaign.getStatus() == CampaignStatus.DRAFT) {
            validateTimeRange(request);
            campaign.updateAll(
                    request.name(),
                    request.type(),
                    request.description(),
                    request.landingPageUrl(),
                    request.startTime(),
                    request.endTime()
            );
            return CampaignResponse.from(campaignRepository.save(campaign));
        }
        if (campaign.getStatus() == CampaignStatus.ACTIVE) {
            campaign.updateActiveEditableFields(request.description(), request.landingPageUrl());
            return CampaignResponse.from(campaignRepository.save(campaign));
        }
        throw new BusinessException(ErrorCode.INVALID_CAMPAIGN_STATUS);
    }

    @Transactional
    public CampaignStatusResponse activate(Long campaignId) {
        Campaign campaign = findById(campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new BusinessException(ErrorCode.INVALID_CAMPAIGN_STATUS);
        }
        campaign.changeStatus(CampaignStatus.ACTIVE);
        return CampaignStatusResponse.from(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignStatusResponse pause(Long campaignId) {
        Campaign campaign = findById(campaignId);
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_CAMPAIGN_STATUS);
        }
        campaign.changeStatus(CampaignStatus.PAUSED);
        return CampaignStatusResponse.from(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignStatusResponse end(Long campaignId) {
        Campaign campaign = findById(campaignId);
        if (campaign.getStatus() == CampaignStatus.ENDED) {
            throw new BusinessException(ErrorCode.INVALID_CAMPAIGN_STATUS);
        }
        campaign.changeStatus(CampaignStatus.ENDED);
        return CampaignStatusResponse.from(campaignRepository.save(campaign));
    }

    private List<Campaign> findCampaigns(CampaignStatus status, CampaignType type) {
        if (status != null && type != null) {
            return campaignRepository.findByStatusAndType(status, type, DEFAULT_SORT);
        }
        if (status != null) {
            return campaignRepository.findByStatus(status, DEFAULT_SORT);
        }
        if (type != null) {
            return campaignRepository.findByType(type, DEFAULT_SORT);
        }
        return campaignRepository.findAll(DEFAULT_SORT);
    }

    private Campaign findById(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAMPAIGN_NOT_FOUND));
    }

    private void validateTimeRange(CampaignRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "endTime must be after startTime.");
        }
    }
}

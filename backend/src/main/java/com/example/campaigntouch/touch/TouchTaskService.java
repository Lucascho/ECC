package com.example.campaigntouch.touch;

import com.example.campaigntouch.campaign.Campaign;
import com.example.campaigntouch.campaign.CampaignRepository;
import com.example.campaigntouch.campaign.CampaignStatus;
import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TouchTaskService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final Set<String> AUDIENCE_RULE_FIELDS = Set.of(
            "memberLevels",
            "lastLoginDaysLessThan",
            "favoriteCategories",
            "hasCartItems"
    );

    private final TouchTaskRepository touchTaskRepository;
    private final CampaignRepository campaignRepository;

    public TouchTaskService(TouchTaskRepository touchTaskRepository, CampaignRepository campaignRepository) {
        this.touchTaskRepository = touchTaskRepository;
        this.campaignRepository = campaignRepository;
    }

    @Transactional
    public TouchTaskResponse create(Long campaignId, TouchTaskRequest request) {
        Campaign campaign = findCampaign(campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_CAMPAIGN_STATUS);
        }
        AudienceRule audienceRule = validateAudienceRule(request.audienceRule());
        List<TouchChannel> channels = validateChannels(request.channels());
        TouchTask touchTask = TouchTask.pending(
                campaign,
                request.taskName(),
                audienceRule,
                channels,
                request.messageTitle(),
                request.messageContent()
        );
        return TouchTaskResponse.from(touchTaskRepository.save(touchTask));
    }

    @Transactional(readOnly = true)
    public List<TouchTaskListResponse> listByCampaign(Long campaignId) {
        findCampaign(campaignId);
        return touchTaskRepository.findByCampaignId(campaignId, DEFAULT_SORT)
                .stream()
                .map(TouchTaskListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TouchTaskResponse get(Long taskId) {
        return TouchTaskResponse.from(findTouchTask(taskId));
    }

    private Campaign findCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAMPAIGN_NOT_FOUND));
    }

    private TouchTask findTouchTask(Long taskId) {
        return touchTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOUCH_TASK_NOT_FOUND));
    }

    private AudienceRule validateAudienceRule(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new BusinessException(ErrorCode.INVALID_AUDIENCE_RULE, "audienceRule must be an object.");
        }
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!AUDIENCE_RULE_FIELDS.contains(fieldName)) {
                throw new BusinessException(ErrorCode.INVALID_AUDIENCE_RULE,
                        "Unsupported audienceRule field: " + fieldName + ".");
            }
        }
        return new AudienceRule(
                optionalStringArray(node, "memberLevels"),
                optionalPositiveInteger(node, "lastLoginDaysLessThan"),
                optionalStringArray(node, "favoriteCategories"),
                optionalBoolean(node, "hasCartItems")
        );
    }

    private List<String> optionalStringArray(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isArray()) {
            throw new BusinessException(ErrorCode.INVALID_AUDIENCE_RULE, fieldName + " must be an array.");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isTextual() || item.asText().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_AUDIENCE_RULE,
                        fieldName + " must contain non-blank strings.");
            }
            values.add(item.asText());
        }
        return values;
    }

    private Integer optionalPositiveInteger(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isInt() || value.asInt() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AUDIENCE_RULE, fieldName + " must be a positive integer.");
        }
        return value.asInt();
    }

    private Boolean optionalBoolean(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isBoolean()) {
            throw new BusinessException(ErrorCode.INVALID_AUDIENCE_RULE, fieldName + " must be a boolean.");
        }
        return value.asBoolean();
    }

    private List<TouchChannel> validateChannels(List<String> channelValues) {
        Set<TouchChannel> uniqueChannels = EnumSet.noneOf(TouchChannel.class);
        Set<String> normalizedValues = new HashSet<>();
        List<TouchChannel> channels = new ArrayList<>();
        for (String channelValue : channelValues) {
            String normalizedValue = channelValue.trim().toUpperCase();
            TouchChannel channel = parseChannel(normalizedValue);
            if (!normalizedValues.add(normalizedValue) || !uniqueChannels.add(channel)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Duplicated touch channel: " + normalizedValue + ".");
            }
            channels.add(channel);
        }
        return channels;
    }

    private TouchChannel parseChannel(String channelValue) {
        try {
            return TouchChannel.valueOf(channelValue);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_TOUCH_CHANNEL,
                    "Unsupported touch channel: " + channelValue + ".");
        }
    }
}

package com.example.campaigntouch.touch;

import com.example.campaigntouch.analytics.CampaignEvent;
import com.example.campaigntouch.analytics.CampaignEventRepository;
import com.example.campaigntouch.analytics.CampaignEventType;
import com.example.campaigntouch.campaign.Campaign;
import com.example.campaigntouch.campaign.CampaignStatus;
import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import com.example.campaigntouch.external.MemberProfileClient;
import com.example.campaigntouch.external.TouchMessageCommand;
import com.example.campaigntouch.external.TouchProvider;
import com.example.campaigntouch.external.TouchTargetMember;
import com.example.campaigntouch.message.MemberMessage;
import com.example.campaigntouch.message.MemberMessageRepository;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TouchExecutionService {

    private final TouchTaskRepository touchTaskRepository;
    private final TouchDeliveryRepository touchDeliveryRepository;
    private final MemberMessageRepository memberMessageRepository;
    private final CampaignEventRepository campaignEventRepository;
    private final MemberProfileClient memberProfileClient;
    private final Map<TouchChannel, TouchProvider> providers;

    public TouchExecutionService(
            TouchTaskRepository touchTaskRepository,
            TouchDeliveryRepository touchDeliveryRepository,
            MemberMessageRepository memberMessageRepository,
            CampaignEventRepository campaignEventRepository,
            MemberProfileClient memberProfileClient,
            List<TouchProvider> providers
    ) {
        this.touchTaskRepository = touchTaskRepository;
        this.touchDeliveryRepository = touchDeliveryRepository;
        this.memberMessageRepository = memberMessageRepository;
        this.campaignEventRepository = campaignEventRepository;
        this.memberProfileClient = memberProfileClient;
        this.providers = providers.stream()
                .collect(Collectors.toMap(
                        TouchProvider::supportChannel,
                        Function.identity(),
                        (left, right) -> left,
                        () -> new EnumMap<>(TouchChannel.class)
                ));
    }

    @Transactional
    public TouchExecutionResponse execute(Long taskId) {
        TouchTask touchTask = findTouchTask(taskId);
        if (touchTask.getStatus() != TouchTaskStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_TOUCH_TASK_STATUS,
                    "Only PENDING touch task can be executed.");
        }
        Campaign campaign = touchTask.getCampaign();
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_CAMPAIGN_STATUS,
                    "Only ACTIVE campaign can execute touch task.");
        }

        touchTask.markProcessing();
        touchTaskRepository.save(touchTask);

        List<TouchTargetMember> targetMembers = memberProfileClient.queryTargetMembers(touchTask.getAudienceRule());
        int deliveryCount = 0;
        int sentCount = 0;
        int failedCount = 0;

        for (TouchTargetMember targetMember : targetMembers) {
            for (TouchChannel channel : touchTask.getChannels()) {
                LocalDateTime occurredAt = LocalDateTime.now();
                TouchDelivery delivery = TouchDelivery.pending(
                        touchTask,
                        targetMember.memberId(),
                        channel,
                        touchTask.getMessageTitle(),
                        touchTask.getMessageContent()
                );
                delivery = touchDeliveryRepository.save(delivery);

                DeliveryResult result = providerFor(channel).send(commandFor(targetMember, channel, touchTask));
                delivery.applyResult(result, occurredAt);
                delivery = touchDeliveryRepository.save(delivery);

                if (delivery.getStatus() == TouchDeliveryStatus.SENT) {
                    sentCount++;
                    if (channel == TouchChannel.IN_APP) {
                        memberMessageRepository.save(MemberMessage.fromDelivery(delivery));
                    }
                    campaignEventRepository.save(CampaignEvent.fromDelivery(delivery, CampaignEventType.SENT,
                            occurredAt));
                } else if (delivery.getStatus() == TouchDeliveryStatus.FAILED) {
                    failedCount++;
                    campaignEventRepository.save(CampaignEvent.fromDelivery(delivery, CampaignEventType.FAILED,
                            occurredAt));
                }
                deliveryCount++;
            }
        }

        LocalDateTime executedAt = LocalDateTime.now();
        touchTask.markCompleted(executedAt);
        touchTaskRepository.save(touchTask);

        return new TouchExecutionResponse(
                touchTask.getId(),
                campaign.getId(),
                touchTask.getStatus(),
                targetMembers.size(),
                deliveryCount,
                sentCount,
                failedCount,
                executedAt
        );
    }

    private TouchTask findTouchTask(Long taskId) {
        return touchTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOUCH_TASK_NOT_FOUND));
    }

    private TouchProvider providerFor(TouchChannel channel) {
        TouchProvider provider = providers.get(channel);
        if (provider == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_TOUCH_CHANNEL,
                    "Unsupported touch channel: " + channel + ".");
        }
        return provider;
    }

    private TouchMessageCommand commandFor(TouchTargetMember targetMember, TouchChannel channel, TouchTask touchTask) {
        return new TouchMessageCommand(
                targetMember.memberId(),
                targetMember.email(),
                targetMember.pushToken(),
                channel,
                touchTask.getMessageTitle(),
                touchTask.getMessageContent()
        );
    }
}

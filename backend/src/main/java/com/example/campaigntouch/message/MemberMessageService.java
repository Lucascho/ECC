package com.example.campaigntouch.message;

import com.example.campaigntouch.analytics.CampaignEvent;
import com.example.campaigntouch.analytics.CampaignEventRepository;
import com.example.campaigntouch.analytics.CampaignEventType;
import com.example.campaigntouch.campaign.CampaignStatus;
import com.example.campaigntouch.common.BusinessException;
import com.example.campaigntouch.common.ErrorCode;
import com.example.campaigntouch.touch.TouchDelivery;
import com.example.campaigntouch.touch.TouchDeliveryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberMessageService {

    private final MemberMessageRepository memberMessageRepository;
    private final TouchDeliveryRepository touchDeliveryRepository;
    private final CampaignEventRepository campaignEventRepository;

    public MemberMessageService(
            MemberMessageRepository memberMessageRepository,
            TouchDeliveryRepository touchDeliveryRepository,
            CampaignEventRepository campaignEventRepository
    ) {
        this.memberMessageRepository = memberMessageRepository;
        this.touchDeliveryRepository = touchDeliveryRepository;
        this.campaignEventRepository = campaignEventRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberMessageResponse> list(String memberId, Long campaignId, Boolean unreadOnly) {
        boolean onlyUnread = Boolean.TRUE.equals(unreadOnly);
        return findMessages(memberId, campaignId, onlyUnread)
                .stream()
                .map(MemberMessageResponse::from)
                .toList();
    }

    @Transactional
    public MemberMessageClickResponse click(Long messageId, String memberId) {
        MemberMessage message = memberMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
        if (!message.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_OWNED_BY_MEMBER);
        }
        if (message.getCampaign().getStatus() != CampaignStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_CAMPAIGN_STATUS);
        }
        if (message.getClickedAt() != null) {
            return MemberMessageClickResponse.from(message);
        }

        LocalDateTime clickedAt = LocalDateTime.now();
        TouchDelivery delivery = message.getDelivery();
        message.click(clickedAt);
        delivery.markClicked(clickedAt);

        memberMessageRepository.save(message);
        touchDeliveryRepository.save(delivery);
        if (!campaignEventRepository.existsByDeliveryIdAndEventType(delivery.getId(), CampaignEventType.CLICK)) {
            campaignEventRepository.save(CampaignEvent.fromDelivery(delivery, CampaignEventType.CLICK, clickedAt));
        }
        return MemberMessageClickResponse.from(message);
    }

    private List<MemberMessage> findMessages(String memberId, Long campaignId, boolean unreadOnly) {
        if (campaignId != null && unreadOnly) {
            return memberMessageRepository.findByMemberIdAndCampaignIdAndReadFalseOrderByCreatedAtDesc(memberId,
                    campaignId);
        }
        if (campaignId != null) {
            return memberMessageRepository.findByMemberIdAndCampaignIdOrderByCreatedAtDesc(memberId, campaignId);
        }
        if (unreadOnly) {
            return memberMessageRepository.findByMemberIdAndReadFalseOrderByCreatedAtDesc(memberId);
        }
        return memberMessageRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }
}

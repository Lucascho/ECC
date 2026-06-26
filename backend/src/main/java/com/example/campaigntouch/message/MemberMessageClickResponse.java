package com.example.campaigntouch.message;

import com.example.campaigntouch.touch.TouchDeliveryStatus;
import java.time.LocalDateTime;

public record MemberMessageClickResponse(
        Long messageId,
        Long deliveryId,
        Long campaignId,
        String memberId,
        String title,
        LocalDateTime clickedAt,
        TouchDeliveryStatus deliveryStatus
) {
    public static MemberMessageClickResponse from(MemberMessage message) {
        return new MemberMessageClickResponse(
                message.getId(),
                message.getDelivery().getId(),
                message.getCampaign().getId(),
                message.getMemberId(),
                message.getTitle(),
                message.getClickedAt(),
                message.getDelivery().getStatus()
        );
    }
}

package com.example.campaigntouch.message;

import java.time.LocalDateTime;

public record MemberMessageResponse(
        Long id,
        Long deliveryId,
        Long campaignId,
        String title,
        String content,
        boolean isRead,
        LocalDateTime clickedAt,
        LocalDateTime createdAt
) {
    public static MemberMessageResponse from(MemberMessage message) {
        return new MemberMessageResponse(
                message.getId(),
                message.getDelivery().getId(),
                message.getCampaign().getId(),
                message.getTitle(),
                message.getContent(),
                message.isRead(),
                message.getClickedAt(),
                message.getCreatedAt()
        );
    }
}

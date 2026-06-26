package com.example.campaigntouch.touch;

import java.time.LocalDateTime;
import java.util.List;

public record TouchTaskResponse(
        Long id,
        Long campaignId,
        String taskName,
        AudienceRule audienceRule,
        List<TouchChannel> channels,
        String messageTitle,
        String messageContent,
        TouchTaskStatus status,
        LocalDateTime executedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TouchTaskResponse from(TouchTask touchTask) {
        return new TouchTaskResponse(
                touchTask.getId(),
                touchTask.getCampaign().getId(),
                touchTask.getTaskName(),
                touchTask.getAudienceRule(),
                touchTask.getChannels(),
                touchTask.getMessageTitle(),
                touchTask.getMessageContent(),
                touchTask.getStatus(),
                touchTask.getExecutedAt(),
                touchTask.getCreatedAt(),
                touchTask.getUpdatedAt()
        );
    }
}

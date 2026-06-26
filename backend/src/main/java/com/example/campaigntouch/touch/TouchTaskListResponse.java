package com.example.campaigntouch.touch;

import java.time.LocalDateTime;
import java.util.List;

public record TouchTaskListResponse(
        Long id,
        Long campaignId,
        String taskName,
        List<TouchChannel> channels,
        TouchTaskStatus status,
        LocalDateTime executedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TouchTaskListResponse from(TouchTask touchTask) {
        return new TouchTaskListResponse(
                touchTask.getId(),
                touchTask.getCampaign().getId(),
                touchTask.getTaskName(),
                touchTask.getChannels(),
                touchTask.getStatus(),
                touchTask.getExecutedAt(),
                touchTask.getCreatedAt(),
                touchTask.getUpdatedAt()
        );
    }
}

package com.example.campaigntouch.touch;

import java.time.LocalDateTime;

public record TouchExecutionResponse(
        Long touchTaskId,
        Long campaignId,
        TouchTaskStatus status,
        int targetMemberCount,
        int deliveryCount,
        int sentCount,
        int failedCount,
        LocalDateTime executedAt
) {
}

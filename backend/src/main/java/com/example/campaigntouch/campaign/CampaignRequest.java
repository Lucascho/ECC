package com.example.campaigntouch.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CampaignRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        CampaignType type,

        @Size(max = 2000)
        String description,

        @Size(max = 500)
        String landingPageUrl,

        @NotNull
        LocalDateTime startTime,

        @NotNull
        LocalDateTime endTime
) {
}

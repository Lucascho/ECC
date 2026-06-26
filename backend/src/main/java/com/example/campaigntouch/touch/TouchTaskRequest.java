package com.example.campaigntouch.touch;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TouchTaskRequest(
        @NotBlank
        @Size(max = 100)
        String taskName,

        @NotNull
        JsonNode audienceRule,

        @NotEmpty
        List<@NotBlank String> channels,

        @NotBlank
        @Size(max = 200)
        String messageTitle,

        @NotBlank
        @Size(max = 5000)
        String messageContent
) {
}

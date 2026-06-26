package com.example.campaigntouch.external;

import com.example.campaigntouch.touch.TouchChannel;

public record TouchMessageCommand(
        String memberId,
        String email,
        String pushToken,
        TouchChannel channel,
        String title,
        String content
) {
}

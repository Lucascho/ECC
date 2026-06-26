package com.example.campaigntouch.external;

import java.time.LocalDateTime;
import java.util.List;

public record TouchTargetMember(
        String memberId,
        String email,
        String pushToken,
        String memberLevel,
        List<String> favoriteCategories,
        LocalDateTime lastLoginAt,
        boolean hasCartItems
) {
}

package com.example.campaigntouch.touch;

import java.util.List;

public record AudienceRule(
        List<String> memberLevels,
        Integer lastLoginDaysLessThan,
        List<String> favoriteCategories,
        Boolean hasCartItems
) {
}

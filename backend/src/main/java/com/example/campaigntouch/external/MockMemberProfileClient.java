package com.example.campaigntouch.external;

import com.example.campaigntouch.touch.AudienceRule;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class MockMemberProfileClient implements MemberProfileClient {

    private static final String MOCK_MEMBERS_PATH = "mock/members.json";

    private final ObjectMapper objectMapper;

    public MockMemberProfileClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<TouchTargetMember> queryTargetMembers(AudienceRule audienceRule) {
        List<TouchTargetMember> members = loadMembers();
        LocalDateTime now = LocalDateTime.now();
        return members.stream()
                .filter(member -> matchesMemberLevels(member, audienceRule))
                .filter(member -> matchesLastLogin(member, audienceRule, now))
                .filter(member -> matchesFavoriteCategories(member, audienceRule))
                .filter(member -> matchesCartItems(member, audienceRule))
                .toList();
    }

    private List<TouchTargetMember> loadMembers() {
        try {
            return objectMapper.readValue(
                    new ClassPathResource(MOCK_MEMBERS_PATH).getInputStream(),
                    new TypeReference<>() {
                    }
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load mock members.", exception);
        }
    }

    private boolean matchesMemberLevels(TouchTargetMember member, AudienceRule audienceRule) {
        if (audienceRule.memberLevels() == null || audienceRule.memberLevels().isEmpty()) {
            return true;
        }
        return audienceRule.memberLevels().contains(member.memberLevel());
    }

    private boolean matchesLastLogin(TouchTargetMember member, AudienceRule audienceRule, LocalDateTime now) {
        if (audienceRule.lastLoginDaysLessThan() == null) {
            return true;
        }
        return !member.lastLoginAt().isBefore(now.minusDays(audienceRule.lastLoginDaysLessThan()));
    }

    private boolean matchesFavoriteCategories(TouchTargetMember member, AudienceRule audienceRule) {
        if (audienceRule.favoriteCategories() == null || audienceRule.favoriteCategories().isEmpty()) {
            return true;
        }
        return member.favoriteCategories().stream().anyMatch(audienceRule.favoriteCategories()::contains);
    }

    private boolean matchesCartItems(TouchTargetMember member, AudienceRule audienceRule) {
        return audienceRule.hasCartItems() == null || member.hasCartItems() == audienceRule.hasCartItems();
    }
}

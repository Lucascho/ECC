package com.example.campaigntouch.message;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberMessageRepository extends JpaRepository<MemberMessage, Long> {

    List<MemberMessage> findByMemberIdOrderByCreatedAtDesc(String memberId);

    List<MemberMessage> findByMemberIdAndCampaignIdOrderByCreatedAtDesc(String memberId, Long campaignId);

    List<MemberMessage> findByMemberIdAndReadFalseOrderByCreatedAtDesc(String memberId);

    List<MemberMessage> findByMemberIdAndCampaignIdAndReadFalseOrderByCreatedAtDesc(String memberId, Long campaignId);
}

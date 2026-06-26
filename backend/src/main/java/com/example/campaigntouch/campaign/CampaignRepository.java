package com.example.campaigntouch.campaign;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByStatus(CampaignStatus status, Sort sort);

    List<Campaign> findByType(CampaignType type, Sort sort);

    List<Campaign> findByStatusAndType(CampaignStatus status, CampaignType type, Sort sort);
}

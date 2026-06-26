package com.example.campaigntouch.touch;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouchTaskRepository extends JpaRepository<TouchTask, Long> {

    List<TouchTask> findByCampaignId(Long campaignId, Sort sort);
}

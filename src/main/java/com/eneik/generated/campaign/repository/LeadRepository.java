package com.eneik.generated.campaign.repository;

import com.eneik.generated.campaign.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {
    List<Lead> findByCampaignId(Long campaignId);
}

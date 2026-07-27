package com.eneik.generated.campaign.repository;

import com.eneik.generated.campaign.model.SpintaxTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpintaxTemplateRepository extends JpaRepository<SpintaxTemplate, Long> {
    List<SpintaxTemplate> findByCampaignId(Long campaignId);
}

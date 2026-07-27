package com.eneik.generated.campaign.repository;

import com.eneik.generated.campaign.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {
    List<Lead> findByCampaignId(Long campaignId);

    @Modifying
    @Query("UPDATE Lead l SET l.status = :newStatus WHERE l.id = :id AND l.status = :expectedStatus")
    int updateStatusAtomic(
        @Param("id") Long id,
        @Param("expectedStatus") String expectedStatus,
        @Param("newStatus") String newStatus
    );
}

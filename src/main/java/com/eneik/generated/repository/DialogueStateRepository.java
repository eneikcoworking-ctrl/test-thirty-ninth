package com.eneik.generated.repository;

import com.eneik.generated.model.DialogueState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface DialogueStateRepository extends JpaRepository<DialogueState, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE DialogueState d SET d.humanInterventionRequired = :required, d.aiTurnsCount = :aiTurnsCount, d.updatedAt = :updatedAt WHERE d.id = :id AND d.humanInterventionRequired = :oldRequired")
    int updateDialogueStateAtomic(
            @Param("id") Long id,
            @Param("oldRequired") boolean oldRequired,
            @Param("required") boolean required,
            @Param("aiTurnsCount") int aiTurnsCount,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}

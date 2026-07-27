package com.eneik.generated.repository;

import com.eneik.generated.model.DialogueState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DialogueStateRepository extends JpaRepository<DialogueState, Long> {

    @Modifying
    @Query("UPDATE DialogueState d SET d.humanInterventionRequired = :required, d.aiTurnsCount = :turnsCount WHERE d.id = :id AND d.humanInterventionRequired = :expectedRequired AND d.aiTurnsCount = :expectedTurnsCount")
    int updateStateAtomic(
        @Param("id") Long id,
        @Param("expectedRequired") boolean expectedRequired,
        @Param("expectedTurnsCount") int expectedTurnsCount,
        @Param("required") boolean required,
        @Param("turnsCount") int turnsCount
    );
}

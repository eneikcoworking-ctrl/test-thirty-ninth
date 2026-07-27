package com.eneik.generated.repository;

import com.eneik.generated.entity.StopTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StopTriggerRepository extends JpaRepository<StopTrigger, Long> {
    Optional<StopTrigger> findByTriggerWord(String triggerWord);
    void deleteByTriggerWord(String triggerWord);
}

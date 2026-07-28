package com.eneik.generated.repository;

import com.eneik.generated.model.UnifiedInboxItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnifiedInboxItemRepository extends JpaRepository<UnifiedInboxItem, Long> {
    List<UnifiedInboxItem> findAllByOrderByLastActivityAtDesc();
}

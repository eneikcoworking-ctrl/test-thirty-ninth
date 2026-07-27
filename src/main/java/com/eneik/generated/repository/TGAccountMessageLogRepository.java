package com.eneik.generated.repository;

import com.eneik.generated.entity.TGAccountMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface TGAccountMessageLogRepository extends JpaRepository<TGAccountMessageLog, Long> {

    @Query("SELECT COUNT(l) FROM TGAccountMessageLog l WHERE l.tgAccount.id = :accountId AND l.sentAt >= :since")
    long countMessagesSentSince(@Param("accountId") Long accountId, @Param("since") Instant since);
}

package com.eneik.generated.repository;

import com.eneik.generated.entity.TGAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TGAccountRepository extends JpaRepository<TGAccount, Long> {
    Optional<TGAccount> findByPhoneNumber(String phoneNumber);

    @Modifying
    @Query("UPDATE TGAccount a SET a.status = :newStatus WHERE a.id = :id AND a.status = :oldStatus")
    int updateStatusAtomic(
        @Param("id") Long id,
        @Param("oldStatus") String oldStatus,
        @Param("newStatus") String newStatus
    );
}

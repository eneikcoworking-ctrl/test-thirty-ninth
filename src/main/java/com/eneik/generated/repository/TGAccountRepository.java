package com.eneik.generated.repository;

import com.eneik.generated.entity.TGAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TGAccountRepository extends JpaRepository<TGAccount, Long> {

    List<TGAccount> findByStatusOrderByIdAsc(String status);

    @Modifying
    @Query("UPDATE TGAccount a SET a.status = :newStatus WHERE a.id = :id AND a.status = :expectedStatus")
    int updateStatusAtomic(
        @Param("id") Long id,
        @Param("expectedStatus") String expectedStatus,
        @Param("newStatus") String newStatus
    );
}

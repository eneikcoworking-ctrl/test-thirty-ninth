package com.eneik.generated.repository;

import com.eneik.generated.entity.TGAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TGAccountRepository extends JpaRepository<TGAccount, Long> {
}

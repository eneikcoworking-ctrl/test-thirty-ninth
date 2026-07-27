package com.eneik.generated.repository;

import com.eneik.generated.domain.TelegramAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramAccountRepository extends JpaRepository<TelegramAccount, String> {
}

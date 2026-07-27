package com.eneik.generated.repository;

import com.eneik.generated.entity.IgnoredChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IgnoredChatRepository extends JpaRepository<IgnoredChat, Long> {
    Optional<IgnoredChat> findByChatId(String chatId);
}

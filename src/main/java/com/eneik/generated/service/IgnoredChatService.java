package com.eneik.generated.service;

import com.eneik.generated.entity.IgnoredChat;
import com.eneik.generated.repository.IgnoredChatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IgnoredChatService {

    private final IgnoredChatRepository repository;

    public IgnoredChatService(IgnoredChatRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isIgnored(String chatId) {
        if (chatId == null) {
            return false;
        }
        return repository.findByChatId(chatId).isPresent();
    }

    @Transactional
    public IgnoredChat ignoreChat(String chatId) {
        if (chatId == null || chatId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat ID cannot be null or empty");
        }
        return repository.findByChatId(chatId)
                .orElseGet(() -> repository.save(new IgnoredChat(chatId)));
    }

    @Transactional
    public void unignoreChat(String chatId) {
        if (chatId == null) {
            return;
        }
        repository.findByChatId(chatId).ifPresent(repository::delete);
    }
}

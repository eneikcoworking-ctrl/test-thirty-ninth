package com.eneik.generated.service;

import com.eneik.generated.entity.TGAccount;
import com.eneik.generated.exception.TelegramException;
import org.springframework.stereotype.Component;

@Component
public class DummyTelegramClient implements TelegramClient {
    @Override
    public void sendMessage(TGAccount account, String recipient, String message) throws TelegramException {
        // Dummy implementation for development
    }
}

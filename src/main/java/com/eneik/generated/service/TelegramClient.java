package com.eneik.generated.service;

import com.eneik.generated.entity.TGAccount;
import com.eneik.generated.exception.TelegramException;

public interface TelegramClient {
    void sendMessage(TGAccount account, String recipient, String message) throws TelegramException;
}

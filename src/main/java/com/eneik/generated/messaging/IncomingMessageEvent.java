package com.eneik.generated.messaging;

import java.time.Instant;

public class IncomingMessageEvent {
    private final String chatId;
    private final String sender;
    private final String text;
    private final Instant timestamp;

    public IncomingMessageEvent(String chatId, String sender, String text, Instant timestamp) {
        this.chatId = chatId;
        this.sender = sender;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getChatId() {
        return chatId;
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "IncomingMessageEvent{" +
                "chatId='" + chatId + '\'' +
                ", sender='" + sender + '\'' +
                ", text='" + text + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

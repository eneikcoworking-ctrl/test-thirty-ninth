package com.eneik.generated.client;

public interface TelegramClient {
    void subscribeToChannel(String sessionName, String targetChannel);
    void markRecentPostsAsRead(String sessionName, String targetChannel);
    void setOnlinePresence(String sessionName, boolean isOnline);
}

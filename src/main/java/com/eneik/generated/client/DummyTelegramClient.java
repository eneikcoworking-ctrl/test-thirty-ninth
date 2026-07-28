package com.eneik.generated.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DummyTelegramClient implements TelegramClient {

    private static final Logger logger = LoggerFactory.getLogger(DummyTelegramClient.class);

    @Override
    public void subscribeToChannel(String sessionName, String targetChannel) {
        logger.info("Session '{}' subscribed to channel '{}'", sessionName, targetChannel);
    }

    @Override
    public void markRecentPostsAsRead(String sessionName, String targetChannel) {
        logger.info("Session '{}' marked recent posts as read in channel '{}'", sessionName, targetChannel);
    }

    @Override
    public void setOnlinePresence(String sessionName, boolean isOnline) {
        logger.info("Session '{}' online presence set to: {}", sessionName, isOnline);
    }
}

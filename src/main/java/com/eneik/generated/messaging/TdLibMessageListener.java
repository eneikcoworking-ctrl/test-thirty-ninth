package com.eneik.generated.messaging;

import com.eneik.generated.service.IgnoredChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Component
public class TdLibMessageListener {

    private static final Logger log = LoggerFactory.getLogger(TdLibMessageListener.class);

    private final IgnoredChatService ignoredChatService;
    private final InternalMessageQueue internalQueue;
    private final Clock clock;

    public TdLibMessageListener(
            IgnoredChatService ignoredChatService,
            InternalMessageQueue internalQueue,
            Optional<Clock> clockOpt) {
        this.ignoredChatService = ignoredChatService;
        this.internalQueue = internalQueue;
        this.clock = clockOpt.orElse(Clock.systemUTC());
    }

    /**
     * Entry method simulating the callback from TDLib when a new inbound message is received.
     *
     * @param chatId the unique identifier of the Telegram chat
     * @param sender the sender info (e.g. username or phone number)
     * @param text   the body of the incoming message
     */
    public void onNewMessage(String chatId, String sender, String text) {
        log.info("Received new raw message: chatId={}, sender={}, text={}", chatId, sender, text);

        if (chatId == null || chatId.trim().isEmpty()) {
            log.warn("Discarding inbound message with null or empty chatId");
            return;
        }

        // Given an ignored chat, When a message arrives, Then it is filtered out.
        if (ignoredChatService.isIgnored(chatId)) {
            log.info("Filtered out message from ignored chat: {}", chatId);
            return;
        }

        // Given an incoming message, When detected by the TDLib listener, Then an event is published to the internal queue.
        IncomingMessageEvent event = new IncomingMessageEvent(chatId, sender, text, clock.instant());
        internalQueue.publish(event);
        log.info("Successfully published message event to internal queue: {}", event);
    }
}

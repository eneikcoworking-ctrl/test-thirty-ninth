package com.eneik.generated.messaging;

import com.eneik.generated.entity.IgnoredChat;
import com.eneik.generated.service.IgnoredChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class InboundMessageListenerTest {

    @Autowired
    private TdLibMessageListener tdLibMessageListener;

    @Autowired
    private IgnoredChatService ignoredChatService;

    @Autowired
    private InternalMessageQueue internalQueue;

    @Autowired
    private Clock clock;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-07-27T12:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @BeforeEach
    public void setUp() {
        // Clear the internal queue before each test to ensure isolation
        internalQueue.clear();
    }

    @Test
    public void testInboundMessage_fromActiveChat_publishesEventToInternalQueue() {
        // Arrange
        String chatId = "chat_12345";
        String sender = "user_john";
        String messageText = "Hi, I am interested in your offer!";

        // Ensure this chat is NOT ignored
        ignoredChatService.unignoreChat(chatId);

        // Act
        tdLibMessageListener.onNewMessage(chatId, sender, messageText);

        // Assert
        assertThat(internalQueue.size()).isEqualTo(1);
        IncomingMessageEvent event = internalQueue.poll();
        assertThat(event).isNotNull();
        assertThat(event.getChatId()).isEqualTo(chatId);
        assertThat(event.getSender()).isEqualTo(sender);
        assertThat(event.getText()).isEqualTo(messageText);

        // Assert the timestamp is deterministic and matches the fixed clock
        assertThat(event.getTimestamp()).isEqualTo(Instant.parse("2026-07-27T12:00:00Z"));
    }

    @Test
    public void testInboundMessage_fromIgnoredChat_isFilteredOut() {
        // Arrange
        String chatId = "chat_ignored_999";
        String sender = "spammer_xyz";
        String messageText = "This message should be ignored.";

        // Ignore the chat
        ignoredChatService.ignoreChat(chatId);
        assertThat(ignoredChatService.isIgnored(chatId)).isTrue();

        // Act
        tdLibMessageListener.onNewMessage(chatId, sender, messageText);

        // Assert
        assertThat(internalQueue.size()).isEqualTo(0);
        IncomingMessageEvent event = internalQueue.poll();
        assertThat(event).isNull();
    }

    @Test
    public void testIgnoreAndUnignore_updatesStateDynamically() {
        // Arrange
        String chatId = "chat_dynamic_777";
        String sender = "dynamic_user";
        String text = "Dynamic messaging test";

        // Act 1: Send from active chat (initially not ignored)
        tdLibMessageListener.onNewMessage(chatId, sender, text);
        assertThat(internalQueue.size()).isEqualTo(1);
        internalQueue.clear();

        // Act 2: Ignore chat and send again
        ignoredChatService.ignoreChat(chatId);
        tdLibMessageListener.onNewMessage(chatId, sender, text);
        assertThat(internalQueue.size()).isEqualTo(0);

        // Act 3: Unignore chat and send again
        ignoredChatService.unignoreChat(chatId);
        tdLibMessageListener.onNewMessage(chatId, sender, text);
        assertThat(internalQueue.size()).isEqualTo(1);

        IncomingMessageEvent event = internalQueue.poll();
        assertThat(event).isNotNull();
        assertThat(event.getChatId()).isEqualTo(chatId);
    }
}

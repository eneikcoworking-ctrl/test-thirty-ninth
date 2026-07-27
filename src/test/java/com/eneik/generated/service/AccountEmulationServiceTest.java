package com.eneik.generated.service;

import com.eneik.generated.entity.AppUser;
import com.eneik.generated.entity.TGAccount;
import com.eneik.generated.entity.TGAccountMessageLog;
import com.eneik.generated.model.DialogueState;
import com.eneik.generated.model.DialogueTurn;
import com.eneik.generated.repository.TGAccountRepository;
import com.eneik.generated.repository.TGAccountMessageLogRepository;
import com.eneik.generated.repository.DialogueStateRepository;
import com.eneik.generated.repository.DialogueTurnRepository;
import com.eneik.generated.repository.UserRepository;
import com.eneik.generated.exception.AccountNotActiveException;
import com.eneik.generated.exception.RateLimitExceededException;
import com.eneik.generated.exception.DialogueLimitExceededException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class AccountEmulationServiceTest {

    @Autowired
    private TGAccountRepository accountRepository;

    @Autowired
    private TGAccountMessageLogRepository messageLogRepository;

    @Autowired
    private DialogueStateRepository dialogueStateRepository;

    @Autowired
    private DialogueTurnRepository dialogueTurnRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TypingStatusBroadcaster typingStatusBroadcaster;

    private AccountEmulationService emulationService;
    private PauseSimulator pauseSimulator;
    private Clock fixedClock;
    private Random seededRandom;
    private AppUser testUser;
    private TGAccount activeAccount;

    @BeforeEach
    public void setUp() {
        typingStatusBroadcaster.clearLog();

        // Seed Random & create Mock Clock for 100% reproducible tests
        seededRandom = new Random(42); // fixed seed
        fixedClock = Clock.fixed(Instant.parse("2026-07-27T12:00:00Z"), ZoneId.of("UTC"));

        // Create PauseSimulator with skipActualSleep = true to avoid slow tests
        pauseSimulator = new PauseSimulator(seededRandom, true);

        // Construct service with seedable and mock dependencies
        emulationService = new AccountEmulationService(
                accountRepository,
                messageLogRepository,
                dialogueStateRepository,
                dialogueTurnRepository,
                pauseSimulator,
                typingStatusBroadcaster,
                Optional.of(fixedClock)
        );

        // Clean up database tables and set up a base user and active account
        userRepository.deleteAll();
        accountRepository.deleteAll();

        testUser = new AppUser();
        testUser.setUsername("test_emulation_user");
        userRepository.saveAndFlush(testUser);

        activeAccount = new TGAccount("+123456789", "ACTIVE");
        activeAccount.setUser(testUser);
        accountRepository.saveAndFlush(activeAccount);
    }

    @Test
    public void testSendMessage_activeAccount_appliesPauseAndBroadcastsTyping() {
        // Arrange
        Random localRandom = new Random(42);
        int expectedPause = 120 + localRandom.nextInt(181);

        // Act
        emulationService.sendMessage(activeAccount.getId(), null, "+987654321", "Hello from automated agent!");

        // Assert Typing Status Broadcasted
        List<String> broadcastLog = typingStatusBroadcaster.getBroadcastLog();
        assertThat(broadcastLog).hasSize(1);
        assertThat(broadcastLog.get(0)).contains("Account " + activeAccount.getId() + " typing to +987654321");

        // Assert Log saved
        long sentCount = messageLogRepository.countMessagesSentSince(activeAccount.getId(), Instant.MIN);
        assertThat(sentCount).isEqualTo(1);

        List<TGAccountMessageLog> logs = messageLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getRecipient()).isEqualTo("+987654321");
        assertThat(logs.get(0).getMessageText()).isEqualTo("Hello from automated agent!");
    }

    @Test
    public void testSendMessage_inactiveAccount_throwsAccountNotActiveException() {
        // Arrange
        TGAccount inactiveAccount = new TGAccount("+555555555", "RE_AUTHORIZATION_REQUIRED");
        inactiveAccount.setUser(testUser);
        accountRepository.saveAndFlush(inactiveAccount);

        // Act & Assert
        assertThatThrownBy(() -> {
            emulationService.sendMessage(inactiveAccount.getId(), null, "+987654321", "Hello");
        }).isInstanceOf(AccountNotActiveException.class)
          .hasMessageContaining("Account is not active");
    }

    @Test
    public void testSendMessage_hitting15Limit_rejectsWithRateLimitError() {
        // Arrange
        Instant baseTime = Instant.now(fixedClock);
        for (int i = 0; i < 15; i++) {
            TGAccountMessageLog logEntry = new TGAccountMessageLog(
                    activeAccount,
                    "+987654321",
                    "Spam message " + i,
                    baseTime.minusSeconds(i * 60)
            );
            messageLogRepository.save(logEntry);
        }
        messageLogRepository.flush();

        // Act & Assert: The 16th message must be rejected with rate limit exception
        assertThatThrownBy(() -> {
            emulationService.sendMessage(activeAccount.getId(), null, "+987654321", "16th Message");
        }).isInstanceOf(RateLimitExceededException.class)
          .hasMessageContaining("Account has reached its daily limit of 15 messages");
    }

    @Test
    public void testSendMessage_withinLimit_allowsSending() {
        // Arrange
        Instant baseTime = Instant.now(fixedClock);
        for (int i = 0; i < 14; i++) {
            TGAccountMessageLog logEntry = new TGAccountMessageLog(
                    activeAccount,
                    "+987654321",
                    "Message " + i,
                    baseTime.minusSeconds(i * 60)
            );
            messageLogRepository.save(logEntry);
        }
        messageLogRepository.flush();

        // Act & Assert: 15th message should be allowed and saved
        emulationService.sendMessage(activeAccount.getId(), null, "+987654321", "15th Message");

        long sentCount = messageLogRepository.countMessagesSentSince(activeAccount.getId(), baseTime.minusSeconds(3600));
        assertThat(sentCount).isEqualTo(15);
    }

    @Test
    public void testSendMessage_sessionReaches8Messages_stopsWithConcreteBlocker() {
        // Arrange
        DialogueState state = new DialogueState();
        dialogueStateRepository.save(state);

        // Add 8 existing dialogue turns using the helper method to maintain bi-directional synchronization
        for (int i = 0; i < 8; i++) {
            DialogueTurn turn = new DialogueTurn();
            turn.setSender(i % 2 == 0 ? "HUMAN" : "AI");
            turn.setMessageText("Dialogue turn " + i);
            state.addTurn(turn);
        }
        dialogueStateRepository.saveAndFlush(state);

        DialogueState loadedState = dialogueStateRepository.findById(state.getId()).orElseThrow();
        assertThat(loadedState.getTurns()).hasSize(8);

        // Act & Assert: The next message should be blocked and marked as human intervention required
        assertThatThrownBy(() -> {
            emulationService.sendMessage(activeAccount.getId(), loadedState.getId(), "+987654321", "Blocked message");
        }).isInstanceOf(DialogueLimitExceededException.class)
          .hasMessageContaining("Dialogue has reached the 8 back-and-forth messages limit");

        DialogueState finalState = dialogueStateRepository.findById(state.getId()).orElseThrow();
        assertThat(finalState.isHumanInterventionRequired()).isTrue();
    }
}

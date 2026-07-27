package com.eneik.generated.service;

import com.eneik.generated.entity.TGAccount;
import com.eneik.generated.entity.TGAccountMessageLog;
import com.eneik.generated.model.DialogueState;
import com.eneik.generated.model.DialogueTurn;
import com.eneik.generated.repository.TGAccountRepository;
import com.eneik.generated.repository.TGAccountMessageLogRepository;
import com.eneik.generated.repository.DialogueStateRepository;
import com.eneik.generated.repository.DialogueTurnRepository;
import com.eneik.generated.exception.AccountNotActiveException;
import com.eneik.generated.exception.RateLimitExceededException;
import com.eneik.generated.exception.DialogueLimitExceededException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class AccountEmulationService {
    private static final Logger log = LoggerFactory.getLogger(AccountEmulationService.class);

    private final TGAccountRepository accountRepository;
    private final TGAccountMessageLogRepository messageLogRepository;
    private final DialogueStateRepository dialogueStateRepository;
    private final DialogueTurnRepository dialogueTurnRepository;
    private final PauseSimulator pauseSimulator;
    private final TypingStatusBroadcaster typingStatusBroadcaster;
    private final Clock clock;

    public AccountEmulationService(
            TGAccountRepository accountRepository,
            TGAccountMessageLogRepository messageLogRepository,
            DialogueStateRepository dialogueStateRepository,
            DialogueTurnRepository dialogueTurnRepository,
            PauseSimulator pauseSimulator,
            TypingStatusBroadcaster typingStatusBroadcaster,
            Optional<Clock> clockOpt) {
        this.accountRepository = accountRepository;
        this.messageLogRepository = messageLogRepository;
        this.dialogueStateRepository = dialogueStateRepository;
        this.dialogueTurnRepository = dialogueTurnRepository;
        this.pauseSimulator = pauseSimulator;
        this.typingStatusBroadcaster = typingStatusBroadcaster;
        this.clock = clockOpt.orElse(Clock.systemUTC());
    }

    @Transactional
    public void sendMessage(Long accountId, Long dialogueStateId, String recipient, String messageText) {
        // 1. Retrieve and check TGAccount status
        TGAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        if (account.getStatus() == null || !account.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw new AccountNotActiveException("Account is not active (current status: " + account.getStatus() + ")");
        }

        // 2. Check 15-message daily rate limit (last 24 hours)
        Instant since = Instant.now(clock).minus(24, ChronoUnit.HOURS);
        long sentCount = messageLogRepository.countMessagesSentSince(accountId, since);
        if (sentCount >= 15) {
            throw new RateLimitExceededException("Account has reached its daily limit of 15 messages.");
        }

        // 3. Check dialogue/session 8-message limits
        DialogueState dialogueState = null;
        if (dialogueStateId != null) {
            dialogueState = dialogueStateRepository.findById(dialogueStateId)
                    .orElseThrow(() -> new IllegalArgumentException("DialogueState not found with ID: " + dialogueStateId));

            if (dialogueState.isHumanInterventionRequired()) {
                throw new DialogueLimitExceededException("Dialogue requires human intervention and is blocked.");
            }

            int currentTurns = dialogueState.getTurns().size();
            if (currentTurns >= 8) {
                dialogueState.setHumanInterventionRequired(true);
                dialogueStateRepository.saveAndFlush(dialogueState);
                throw new DialogueLimitExceededException("Dialogue has reached the 8 back-and-forth messages limit and is now blocked.");
            }
        }

        // 4. Broadcast "typing..." status
        typingStatusBroadcaster.broadcastTyping(accountId, recipient);

        // 5. Calculate random pause and apply
        int pauseSecs = pauseSimulator.calculatePauseSeconds();
        pauseSimulator.applyPause(pauseSecs);

        // 6. Record sent message log
        TGAccountMessageLog logEntry = new TGAccountMessageLog(account, recipient, messageText, Instant.now(clock));
        messageLogRepository.save(logEntry);

        // 7. If part of a dialogue state, record the DialogueTurn and update AI turns count
        if (dialogueState != null) {
            DialogueTurn newTurn = new DialogueTurn();
            newTurn.setDialogueState(dialogueState);
            newTurn.setSender("AI");
            newTurn.setMessageText(messageText);
            newTurn.setTimestamp(LocalDateTime.now(clock));
            dialogueTurnRepository.save(newTurn);

            dialogueState.setAiTurnsCount(dialogueState.getAiTurnsCount() + 1);
            if (dialogueState.getTurns().size() >= 8) {
                dialogueState.setHumanInterventionRequired(true);
            }
            dialogueStateRepository.save(dialogueState);
        }
    }
}

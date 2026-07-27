package com.eneik.generated.service;

import com.eneik.generated.campaign.model.Campaign;
import com.eneik.generated.campaign.model.Lead;
import com.eneik.generated.campaign.repository.CampaignRepository;
import com.eneik.generated.campaign.repository.LeadRepository;
import com.eneik.generated.entity.TGAccount;
import com.eneik.generated.exception.DialogueBlockedException;
import com.eneik.generated.exception.FloodWaitException;
import com.eneik.generated.exception.TelegramException;
import com.eneik.generated.model.DialogueState;
import com.eneik.generated.model.DialogueTurn;
import com.eneik.generated.repository.DialogueStateRepository;
import com.eneik.generated.repository.DialogueTurnRepository;
import com.eneik.generated.repository.TGAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class CampaignEngineService {

    private final CampaignRepository campaignRepository;
    private final LeadRepository leadRepository;
    private final TGAccountRepository tgAccountRepository;
    private final DialogueStateRepository dialogueStateRepository;
    private final DialogueTurnRepository dialogueTurnRepository;
    private final SpintaxService spintaxService;
    private final TelegramClient telegramClient;
    private final CampaignStateService campaignStateService;
    private final Random defaultRandom;

    public CampaignEngineService(
            CampaignRepository campaignRepository,
            LeadRepository leadRepository,
            TGAccountRepository tgAccountRepository,
            DialogueStateRepository dialogueStateRepository,
            DialogueTurnRepository dialogueTurnRepository,
            SpintaxService spintaxService,
            TelegramClient telegramClient,
            CampaignStateService campaignStateService) {
        this.campaignRepository = campaignRepository;
        this.leadRepository = leadRepository;
        this.tgAccountRepository = tgAccountRepository;
        this.dialogueStateRepository = dialogueStateRepository;
        this.dialogueTurnRepository = dialogueTurnRepository;
        this.spintaxService = spintaxService;
        this.telegramClient = telegramClient;
        this.campaignStateService = campaignStateService;
        this.defaultRandom = new Random();
    }

    /**
     * Dispatches a message to a lead for a campaign with automatic session/account rotation on FLOOD_WAIT.
     * Uses default Random generator for spintax.
     * Not annotated with @Transactional so that individual status updates commit immediately
     * and do not roll back if an exception occurs during the overall workflow.
     */
    public void dispatchMessage(Long leadId, String templateText) {
        dispatchMessage(leadId, templateText, this.defaultRandom);
    }

    /**
     * Dispatches a message to a lead with custom/seeded Random generator for deterministic spintax resolution.
     */
    public void dispatchMessage(Long leadId, String templateText, Random rng) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

        if (!"PENDING".equals(lead.getStatus())) {
            throw new IllegalStateException("Lead is already in state: " + lead.getStatus());
        }

        List<TGAccount> activeAccounts = tgAccountRepository.findByStatusOrderByIdAsc("ACTIVE");
        if (activeAccounts.isEmpty()) {
            int updated = campaignStateService.updateLeadStatusAtomic(leadId, "PENDING", "FAILED");
            if (updated > 0) {
                lead.setStatus("FAILED");
            }
            throw new IllegalStateException("No active Telegram accounts available for dispatching.");
        }

        // Try accounts one by one
        for (int i = 0; i < activeAccounts.size(); i++) {
            TGAccount account = activeAccounts.get(i);
            try {
                // Resolve spintax
                String resolvedMessage = spintaxService.evaluate(templateText, rng);

                // Send the message
                String recipient = lead.getUsername() != null ? lead.getUsername() : lead.getPhoneNumber();
                telegramClient.sendMessage(account, recipient, resolvedMessage);

                // Success! Atomically transition the Lead status to DISPATCHED
                int updated = campaignStateService.updateLeadStatusAtomic(leadId, "PENDING", "DISPATCHED");
                if (updated > 0) {
                    lead.setStatus("DISPATCHED");
                }
                return; // Completed successfully

            } catch (FloodWaitException e) {
                // Account hit a FLOOD_WAIT. Atomically transition its status to FLOOD_WAIT using a separate transaction.
                campaignStateService.updateTGAccountStatusAtomic(account.getId(), "ACTIVE", "FLOOD_WAIT");
                account.setStatus("FLOOD_WAIT");

                // If this was the last available active account, mark lead as FAILED and fail.
                if (i == activeAccounts.size() - 1) {
                    int updated = campaignStateService.updateLeadStatusAtomic(leadId, "PENDING", "FAILED");
                    if (updated > 0) {
                        lead.setStatus("FAILED");
                    }
                    throw new TelegramException("Failed to dispatch message: all available active accounts hit FLOOD_WAIT.", e);
                }
                // Otherwise, the loop automatically retries with the next active account
            } catch (TelegramException e) {
                // For other general exceptions, we also fail the dispatch of this lead
                int updated = campaignStateService.updateLeadStatusAtomic(leadId, "PENDING", "FAILED");
                if (updated > 0) {
                    lead.setStatus("FAILED");
                }
                throw e;
            }
        }
    }

    /**
     * Records a dialogue turn and tracks back-and-forth messages.
     * If the session reaches 8 back-and-forth messages, stops with a concrete blocker instead of looping.
     */
    @Transactional
    public void processMessageInDialogue(Long dialogueStateId, String sender, String messageText) {
        DialogueState state = dialogueStateRepository.findById(dialogueStateId)
                .orElseThrow(() -> new IllegalArgumentException("Dialogue state not found: " + dialogueStateId));

        if (state.isHumanInterventionRequired()) {
            throw new DialogueBlockedException("Automated processing blocked. Human intervention is required.");
        }

        List<DialogueTurn> turns = dialogueTurnRepository.findByDialogueStateIdOrderByTimestampAsc(dialogueStateId);
        int currentCount = turns.size();

        if (currentCount >= 8) {
            // Already blocked
            throw new DialogueBlockedException("Dialogue limit reached (8 messages). Automated processing blocked.");
        }

        int newCount = currentCount + 1;
        boolean requiresIntervention = (newCount >= 8);

        // Calculate new AI turns count
        int newAiTurnsCount = state.getAiTurnsCount();
        if ("AI".equalsIgnoreCase(sender)) {
            newAiTurnsCount++;
        }

        // Atomically update the DialogueState with current expected required/turnsCount values to guard against race conditions.
        int updatedRows = campaignStateService.updateDialogueStateAtomic(
                dialogueStateId,
                state.isHumanInterventionRequired(),
                state.getAiTurnsCount(),
                requiresIntervention,
                newAiTurnsCount
        );
        if (updatedRows == 0) {
            // Under concurrency, optimistic lock/concurrency failure occurred
            throw new DialogueBlockedException("Dialogue state was modified concurrently. Automated processing blocked.");
        }

        // Add the turn
        DialogueTurn turn = new DialogueTurn();
        turn.setSender(sender);
        turn.setMessageText(messageText);
        turn.setTimestamp(LocalDateTime.now());
        turn.setDialogueState(state);
        dialogueTurnRepository.save(turn);

        // Update state in-memory object
        state.setAiTurnsCount(newAiTurnsCount);
        state.setHumanInterventionRequired(requiresIntervention);

        if (requiresIntervention) {
            throw new DialogueBlockedException("Dialogue session reached the maximum limit of 8 back-and-forth messages. Automated processing blocked.");
        }
    }
}

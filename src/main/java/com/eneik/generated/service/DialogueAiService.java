package com.eneik.generated.service;

import com.eneik.generated.model.DialogueState;
import com.eneik.generated.model.DialogueTurn;
import com.eneik.generated.repository.DialogueStateRepository;
import com.eneik.generated.repository.DialogueTurnRepository;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DialogueAiService {

    private final DialogueStateRepository stateRepository;
    private final DialogueTurnRepository turnRepository;
    private final ChatClient chatClient;

    public DialogueAiService(
            DialogueStateRepository stateRepository,
            DialogueTurnRepository turnRepository,
            @Autowired(required = false) ChatClient chatClient) {
        this.stateRepository = stateRepository;
        this.turnRepository = turnRepository;
        this.chatClient = chatClient;
    }

    /**
     * Evaluates the full chat context and generates a reply or fires a stop-trigger.
     *
     * @param dialogueStateId ID of the dialogue state.
     * @param incomingMessage The incoming human message to add and evaluate.
     * @param persona         The persona configured for the prompt template.
     * @return The generated AI DialogueTurn, or null if a stop-trigger fired.
     */
    @Transactional
    public DialogueTurn evaluateAndGenerateReply(Long dialogueStateId, String incomingMessage, String persona) {
        // 1. Load DialogueState
        DialogueState state = stateRepository.findById(dialogueStateId)
                .orElseThrow(() -> new IllegalArgumentException("Dialogue state not found: " + dialogueStateId));

        // 2. Check back-and-forth messages limit (8 maximum)
        // If the session already contains 8 or more turns, throw a concrete blocker exception
        if (state.getTurns().size() >= 8) {
            throw new IllegalStateException("Session has reached the limit of 8 back-and-forth messages. Dialogue blocked.");
        }

        // 3. Add incoming message from the HUMAN
        DialogueTurn humanTurn = new DialogueTurn();
        humanTurn.setSender("HUMAN");
        humanTurn.setMessageText(incomingMessage);
        humanTurn.setTimestamp(LocalDateTime.now());
        state.addTurn(humanTurn);
        turnRepository.save(humanTurn);

        // 4. Check for pricing intent (stop-trigger)
        boolean hasPricingIntent = detectPricingIntent(incomingMessage);
        if (hasPricingIntent) {
            // Atomically-guarded database update to transition to human intervention required
            int updated = stateRepository.updateDialogueStateAtomic(
                    state.getId(),
                    false, // oldRequired
                    true,  // required (flags 'Awaiting Human Intervention')
                    state.getAiTurnsCount(),
                    LocalDateTime.now()
            );
            if (updated == 0) {
                // If it was already true or updated concurrently, reload/handle
                state = stateRepository.findById(dialogueStateId).orElseThrow();
            } else {
                state.setHumanInterventionRequired(true);
            }
            return null;
        }

        // 5. Build prompt using Spring AI PromptTemplate
        String templateText = "System Prompt (Persona): {persona}\nChat History:\n{history}\nAI:";
        PromptTemplate promptTemplate = new PromptTemplate(templateText);

        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put("persona", persona != null ? persona : "Friendly Sales Assistant");
        modelMap.put("history", formatHistory(state.getTurns()));
        Prompt prompt = promptTemplate.create(modelMap);

        // 6. Generate reply based on Spring AI ChatClient with deterministic fallback
        String aiReplyText = callSpringAiOrFallback(prompt, persona);

        // 7. Save AI turn
        DialogueTurn aiTurn = new DialogueTurn();
        aiTurn.setSender("AI");
        aiTurn.setMessageText(aiReplyText);
        aiTurn.setTimestamp(LocalDateTime.now());
        state.addTurn(aiTurn);
        turnRepository.save(aiTurn);

        // 8. Atomically increment AI turns count
        int updatedCount = state.getAiTurnsCount() + 1;
        stateRepository.updateDialogueStateAtomic(
                state.getId(),
                state.isHumanInterventionRequired(),
                state.isHumanInterventionRequired(),
                updatedCount,
                LocalDateTime.now()
        );
        state.setAiTurnsCount(updatedCount);

        return aiTurn;
    }

    /**
     * Determines whether the incoming message is asking for pricing.
     */
    public boolean detectPricingIntent(String text) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("price") ||
               lower.contains("pricing") ||
               lower.contains("how much") ||
               lower.contains("cost") ||
               lower.contains("tariff") ||
               lower.contains("rate");
    }

    /**
     * Formats the chat history for inclusion in the prompt template.
     */
    private String formatHistory(List<DialogueTurn> history) {
        StringBuilder sb = new StringBuilder();
        for (DialogueTurn turn : history) {
            sb.append(turn.getSender()).append(": ").append(turn.getMessageText()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Generates a reply using Spring AI's ChatClient if present, otherwise uses deterministic fallback.
     */
    private String callSpringAiOrFallback(Prompt prompt, String persona) {
        if (chatClient != null) {
            try {
                ChatResponse response = chatClient.call(prompt);
                if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
                    return response.getResult().getOutput().getContent();
                }
            } catch (Exception e) {
                // Safe deterministic fallback upon error
            }
        }
        return callFallbackLlm(persona);
    }

    /**
     * Safe deterministic fallback LLM generation.
     */
    private String callFallbackLlm(String persona) {
        if (persona == null) {
            persona = "Friendly Sales Assistant";
        }

        if (persona.contains("Friendly")) {
            return "Hey there! 😊 Happy to help you out with that! Let me know if you need anything else!";
        } else if (persona.contains("Professional")) {
            return "Greetings. Regarding your request, I can confirm that our services align perfectly with your technical requirements.";
        } else {
            return "Hello. Thank you for your message. How can I assist you further today?";
        }
    }
}

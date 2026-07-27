package com.eneik.generated.service;

import com.eneik.generated.model.DialogueState;
import com.eneik.generated.model.DialogueTurn;
import com.eneik.generated.repository.DialogueStateRepository;
import com.eneik.generated.repository.DialogueTurnRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class DialogueAiServiceTest {

    @Autowired
    private DialogueStateRepository stateRepository;

    @Autowired
    private DialogueTurnRepository turnRepository;

    @Autowired
    private DialogueAiService dialogueAiService;

    @Test
    public void testEvaluateAndGenerateReply_FriendlyPersona() {
        // Arrange
        DialogueState state = new DialogueState();
        stateRepository.saveAndFlush(state);

        // Act
        DialogueTurn reply = dialogueAiService.evaluateAndGenerateReply(
                state.getId(),
                "Hello, can you help me?",
                "Friendly Sales Assistant"
        );

        // Assert
        assertThat(reply).isNotNull();
        assertThat(reply.getSender()).isEqualTo("AI");
        assertThat(reply.getMessageText()).contains("Hey there!");

        DialogueState updatedState = stateRepository.findById(state.getId()).orElseThrow();
        assertThat(updatedState.getAiTurnsCount()).isEqualTo(1);
        assertThat(updatedState.isHumanInterventionRequired()).isFalse();
        assertThat(updatedState.getTurns()).hasSize(2); // 1 HUMAN + 1 AI
    }

    @Test
    public void testEvaluateAndGenerateReply_ProfessionalPersona() {
        // Arrange
        DialogueState state = new DialogueState();
        stateRepository.saveAndFlush(state);

        // Act
        DialogueTurn reply = dialogueAiService.evaluateAndGenerateReply(
                state.getId(),
                "I am interested in your software services.",
                "Professional Technical Consultant"
        );

        // Assert
        assertThat(reply).isNotNull();
        assertThat(reply.getSender()).isEqualTo("AI");
        assertThat(reply.getMessageText()).contains("Greetings. Regarding your request");

        DialogueState updatedState = stateRepository.findById(state.getId()).orElseThrow();
        assertThat(updatedState.getAiTurnsCount()).isEqualTo(1);
        assertThat(updatedState.isHumanInterventionRequired()).isFalse();
    }

    @Test
    public void testEvaluateAndGenerateReply_PricingIntentStopTrigger() {
        // Arrange
        DialogueState state = new DialogueState();
        stateRepository.saveAndFlush(state);

        // Act
        DialogueTurn reply = dialogueAiService.evaluateAndGenerateReply(
                state.getId(),
                "How much does your service cost?",
                "Friendly Sales Assistant"
        );

        // Assert
        assertThat(reply).isNull(); // Stop-trigger fires, no AI response generated

        DialogueState updatedState = stateRepository.findById(state.getId()).orElseThrow();
        assertThat(updatedState.getAiTurnsCount()).isEqualTo(0);
        assertThat(updatedState.isHumanInterventionRequired()).isTrue(); // Flags Awaiting Human Intervention
        assertThat(updatedState.getTurns()).hasSize(1); // Only the HUMAN turn is saved
        assertThat(updatedState.getTurns().get(0).getMessageText()).isEqualTo("How much does your service cost?");
    }

    @Test
    public void testEvaluateAndGenerateReply_DialogueLimitReached() {
        // Arrange
        DialogueState state = new DialogueState();
        stateRepository.saveAndFlush(state);

        // Populate with 8 messages (4 exchanges of human and AI)
        for (int i = 0; i < 4; i++) {
            DialogueTurn human = new DialogueTurn();
            human.setSender("HUMAN");
            human.setMessageText("User message " + i);
            human.setDialogueState(state);
            state.addTurn(human);
            turnRepository.save(human);

            DialogueTurn ai = new DialogueTurn();
            ai.setSender("AI");
            ai.setMessageText("AI response " + i);
            ai.setDialogueState(state);
            state.addTurn(ai);
            turnRepository.save(ai);
        }
        stateRepository.saveAndFlush(state);

        // Verify state has 8 messages
        assertThat(state.getTurns()).hasSize(8);

        // Act & Assert
        assertThatThrownBy(() -> dialogueAiService.evaluateAndGenerateReply(
                state.getId(),
                "One more message...",
                "Friendly Sales Assistant"
        ))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Session has reached the limit of 8 back-and-forth messages");
    }

    @Test
    public void testAtomicUpdateConcurrencyGuard() {
        // Arrange
        DialogueState state = new DialogueState();
        stateRepository.saveAndFlush(state);

        // Act
        int updatedRows = stateRepository.updateDialogueStateAtomic(
                state.getId(),
                false, // oldRequired
                true,  // required
                1,
                LocalDateTime.now()
        );

        // Assert
        assertThat(updatedRows).isEqualTo(1);

        // If another concurrent request tries to update with stale expected condition (oldRequired=false)
        int staleUpdatedRows = stateRepository.updateDialogueStateAtomic(
                state.getId(),
                false, // stale expected condition
                true,
                2,
                LocalDateTime.now()
        );

        assertThat(staleUpdatedRows).isEqualTo(0); // Fails atomically, preventing concurrent override
    }

    @Test
    public void testEvaluateWithSpringAiChatClientMock() {
        // Arrange
        DialogueState state = new DialogueState();
        stateRepository.saveAndFlush(state);

        // Set up a mock/anonymous ChatClient to simulate active Spring AI behavior
        ChatClient mockChatClient = new ChatClient() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(new Generation("Custom Mocked AI Reply Content"));
            }
        };

        DialogueAiService serviceWithMock = new DialogueAiService(stateRepository, turnRepository, mockChatClient);

        // Act
        DialogueTurn reply = serviceWithMock.evaluateAndGenerateReply(
                state.getId(),
                "Hi, I want a custom demo.",
                "Friendly Sales Assistant"
        );

        // Assert
        assertThat(reply).isNotNull();
        assertThat(reply.getMessageText()).isEqualTo("Custom Mocked AI Reply Content");
    }
}

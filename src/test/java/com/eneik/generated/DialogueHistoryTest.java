package com.eneik.generated;

import com.eneik.generated.model.DialogueState;
import com.eneik.generated.model.DialogueTurn;
import com.eneik.generated.repository.DialogueStateRepository;
import com.eneik.generated.repository.DialogueTurnRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class DialogueHistoryTest {

    @Autowired
    private DialogueStateRepository dialogueStateRepository;

    @Autowired
    private DialogueTurnRepository dialogueTurnRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testSaveMessageExchange_orderedByTimestampWithCorrectSenderFlags() {
        // Arrange
        DialogueState state = new DialogueState();
        dialogueStateRepository.saveAndFlush(state);

        LocalDateTime baseTime = LocalDateTime.of(2026, 7, 27, 12, 0, 0);

        DialogueTurn turn1 = new DialogueTurn();
        turn1.setSender("HUMAN");
        turn1.setMessageText("Hello, is anyone there?");
        turn1.setTimestamp(baseTime);
        turn1.setDialogueState(state);

        DialogueTurn turn2 = new DialogueTurn();
        turn2.setSender("AI");
        turn2.setMessageText("Yes! I am here to help you.");
        turn2.setTimestamp(baseTime.plusMinutes(2)); // third in time
        turn2.setDialogueState(state);

        DialogueTurn turn3 = new DialogueTurn();
        turn3.setSender("HUMAN");
        turn3.setMessageText("Great, tell me more.");
        turn3.setTimestamp(baseTime.plusMinutes(1)); // second in time
        turn3.setDialogueState(state);

        dialogueTurnRepository.save(turn1);
        dialogueTurnRepository.save(turn2);
        dialogueTurnRepository.save(turn3);

        dialogueTurnRepository.flush();

        // Clear persistence context to force reloading from the database
        entityManager.clear();

        // Retrieve and assert
        DialogueState retrievedState = dialogueStateRepository.findById(state.getId()).orElseThrow();
        List<DialogueTurn> retrievedTurns = retrievedState.getTurns();

        // Assert size
        assertThat(retrievedTurns).hasSize(3);

        // Assert sorting is strictly ordered by timestamp: turn1 (base), turn3 (base + 1 min), turn2 (base + 2 min)
        assertThat(retrievedTurns.get(0).getSender()).isEqualTo("HUMAN");
        assertThat(retrievedTurns.get(0).getMessageText()).isEqualTo("Hello, is anyone there?");
        assertThat(retrievedTurns.get(0).getTimestamp()).isEqualTo(baseTime);

        assertThat(retrievedTurns.get(1).getSender()).isEqualTo("HUMAN");
        assertThat(retrievedTurns.get(1).getMessageText()).isEqualTo("Great, tell me more.");
        assertThat(retrievedTurns.get(1).getTimestamp()).isEqualTo(baseTime.plusMinutes(1));

        assertThat(retrievedTurns.get(2).getSender()).isEqualTo("AI");
        assertThat(retrievedTurns.get(2).getMessageText()).isEqualTo("Yes! I am here to help you.");
        assertThat(retrievedTurns.get(2).getTimestamp()).isEqualTo(baseTime.plusMinutes(2));

        // Also assert the repository query returns the same sorted order
        List<DialogueTurn> dbTurnsOrdered = dialogueTurnRepository.findByDialogueStateIdOrderByTimestampAsc(state.getId());
        assertThat(dbTurnsOrdered).hasSize(3);
        assertThat(dbTurnsOrdered.get(0).getId()).isEqualTo(turn1.getId());
        assertThat(dbTurnsOrdered.get(1).getId()).isEqualTo(turn3.getId());
        assertThat(dbTurnsOrdered.get(2).getId()).isEqualTo(turn2.getId());
    }

    @Test
    public void testDialogueState_tracksAiTurnsAndHumanInterventionFlags() {
        // Arrange
        DialogueState state = new DialogueState();
        state.setAiTurnsCount(1);
        state.setHumanInterventionRequired(false);
        dialogueStateRepository.saveAndFlush(state);

        // Act & Assert Initial
        DialogueState initialFetched = dialogueStateRepository.findById(state.getId()).orElseThrow();
        assertThat(initialFetched.getAiTurnsCount()).isEqualTo(1);
        assertThat(initialFetched.isHumanInterventionRequired()).isFalse();

        // Act Update
        initialFetched.setAiTurnsCount(3);
        initialFetched.setHumanInterventionRequired(true);
        dialogueStateRepository.saveAndFlush(initialFetched);

        // Clear persistence context
        entityManager.clear();

        // Assert Updated
        DialogueState updatedFetched = dialogueStateRepository.findById(state.getId()).orElseThrow();
        assertThat(updatedFetched.getAiTurnsCount()).isEqualTo(3);
        assertThat(updatedFetched.isHumanInterventionRequired()).isTrue();
    }
}

package com.eneik.generated.controller;

import com.eneik.generated.model.DialogueState;
import com.eneik.generated.model.DialogueTurn;
import com.eneik.generated.model.UnifiedInboxItem;
import com.eneik.generated.repository.DialogueStateRepository;
import com.eneik.generated.repository.DialogueTurnRepository;
import com.eneik.generated.repository.UnifiedInboxItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inbox")
public class DialogueInboxController {

    private final UnifiedInboxItemRepository inboxItemRepository;
    private final DialogueStateRepository dialogueStateRepository;
    private final DialogueTurnRepository dialogueTurnRepository;

    public DialogueInboxController(UnifiedInboxItemRepository inboxItemRepository,
                                   DialogueStateRepository dialogueStateRepository,
                                   DialogueTurnRepository dialogueTurnRepository) {
        this.inboxItemRepository = inboxItemRepository;
        this.dialogueStateRepository = dialogueStateRepository;
        this.dialogueTurnRepository = dialogueTurnRepository;
    }

    @GetMapping
    public ResponseEntity<List<UnifiedInboxItem>> getUnifiedInbox() {
        return ResponseEntity.ok(inboxItemRepository.findAllByOrderByLastActivityAtDesc());
    }

    @PostMapping("/{dialogueId}/reply")
    @Transactional
    public ResponseEntity<DialogueTurnResponse> manualReply(@PathVariable Long dialogueId, @RequestBody ReplyRequest request) {
        DialogueState state = dialogueStateRepository.findById(dialogueId)
                .orElseThrow(() -> new IllegalArgumentException("Dialogue state not found: " + dialogueId));

        int updated = dialogueStateRepository.updateDialogueStateAtomic(
                state.getId(),
                state.isHumanInterventionRequired(),
                true,
                state.getAiTurnsCount(),
                LocalDateTime.now()
        );

        if (updated == 0 && !state.isHumanInterventionRequired()) {
            throw new IllegalStateException("Failed to update dialogue state atomically");
        }

        DialogueTurn turn = new DialogueTurn();
        turn.setDialogueState(state);
        turn.setSender("HUMAN");
        turn.setMessageText(request.getMessage());
        turn.setTimestamp(LocalDateTime.now());
        turn = dialogueTurnRepository.saveAndFlush(turn);

        DialogueTurnResponse response = new DialogueTurnResponse();
        response.setId(turn.getId());
        response.setSender(turn.getSender());
        response.setMessageText(turn.getMessageText());
        response.setTimestamp(turn.getTimestamp());

        return ResponseEntity.ok(response);
    }

    public static class ReplyRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class DialogueTurnResponse {
        private Long id;
        private String sender;
        private String messageText;
        private LocalDateTime timestamp;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        public String getMessageText() { return messageText; }
        public void setMessageText(String messageText) { this.messageText = messageText; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}

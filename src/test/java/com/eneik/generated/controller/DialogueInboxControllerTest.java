package com.eneik.generated.controller;

import com.eneik.generated.model.DialogueState;
import com.eneik.generated.model.DialogueTurn;
import com.eneik.generated.model.UnifiedInboxItem;
import com.eneik.generated.repository.DialogueStateRepository;
import com.eneik.generated.repository.DialogueTurnRepository;
import com.eneik.generated.repository.UnifiedInboxItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DialogueInboxController.class)
public class DialogueInboxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UnifiedInboxItemRepository inboxItemRepository;

    @MockBean
    private DialogueStateRepository dialogueStateRepository;

    @MockBean
    private DialogueTurnRepository dialogueTurnRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetUnifiedInbox() throws Exception {
        UnifiedInboxItem item = new UnifiedInboxItem();
        Mockito.when(inboxItemRepository.findAllByOrderByLastActivityAtDesc()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/inbox")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testManualReply() throws Exception {
        DialogueState state = new DialogueState();
        state.setId(1L);
        state.setHumanInterventionRequired(false);

        Mockito.when(dialogueStateRepository.findById(1L)).thenReturn(Optional.of(state));
        Mockito.when(dialogueStateRepository.updateDialogueStateAtomic(eq(1L), eq(false), eq(true), eq(0), any(LocalDateTime.class)))
                .thenReturn(1);
        Mockito.when(dialogueTurnRepository.saveAndFlush(any(DialogueTurn.class))).thenAnswer(i -> {
            DialogueTurn t = i.getArgument(0);
            t.setId(42L);
            return t;
        });

        DialogueInboxController.ReplyRequest request = new DialogueInboxController.ReplyRequest();
        request.setMessage("Manual reply text");

        mockMvc.perform(post("/api/inbox/1/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sender").value("HUMAN"))
                .andExpect(jsonPath("$.messageText").value("Manual reply text"));
    }
}

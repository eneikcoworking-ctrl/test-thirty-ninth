package com.eneik.generated.controller;

import com.eneik.generated.entity.TGAccount;
import com.eneik.generated.model.DialogueState;
import com.eneik.generated.model.DialogueTurn;
import com.eneik.generated.repository.DialogueStateRepository;
import com.eneik.generated.repository.DialogueTurnRepository;
import com.eneik.generated.repository.TGAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DialogueInboxControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DialogueStateRepository dialogueStateRepository;

    @Autowired
    private DialogueTurnRepository dialogueTurnRepository;

    @Autowired
    private TGAccountRepository tgAccountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testGetInboxAndManualReply() throws Exception {
        TGAccount account1 = new TGAccount("+12223334444", "Active");
        tgAccountRepository.saveAndFlush(account1);

        DialogueState ds1 = new DialogueState();
        ds1.setTgAccount(account1);
        ds1.setStatus("ACTIVE");
        ds1.setUpdatedAt(LocalDateTime.now());
        dialogueStateRepository.saveAndFlush(ds1);

        // Fetch inbox
        mockMvc.perform(get("/api/inbox")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dialogueId").value(ds1.getId()))
                .andExpect(jsonPath("$[0].accountPhoneNumber").value("+12223334444"))
                .andExpect(jsonPath("$[0].humanInterventionRequired").value(false));

        // Reply
        DialogueInboxController.ReplyRequest request = new DialogueInboxController.ReplyRequest();
        request.setMessage("This is a manual reply");

        mockMvc.perform(post("/api/inbox/" + ds1.getId() + "/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sender").value("HUMAN"))
                .andExpect(jsonPath("$.messageText").value("This is a manual reply"));

        entityManager.flush();
        entityManager.clear();

        DialogueState updatedState = dialogueStateRepository.findById(ds1.getId()).orElseThrow();
        assertThat(updatedState.isHumanInterventionRequired()).isTrue();

        assertThat(updatedState.getTurns()).hasSize(1);
        assertThat(updatedState.getTurns().get(0).getSender()).isEqualTo("HUMAN");
        assertThat(updatedState.getTurns().get(0).getMessageText()).isEqualTo("This is a manual reply");
    }
}

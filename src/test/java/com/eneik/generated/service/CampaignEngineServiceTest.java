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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
public class CampaignEngineServiceTest {

    @Autowired
    private CampaignEngineService campaignEngineService;

    @Autowired
    private SpintaxService spintaxService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private TGAccountRepository tgAccountRepository;

    @Autowired
    private DialogueStateRepository dialogueStateRepository;

    @Autowired
    private DialogueTurnRepository dialogueTurnRepository;

    @MockBean
    private TelegramClient telegramClient;

    @BeforeEach
    public void setup() {
        cleanup();
    }

    @AfterEach
    public void teardown() {
        cleanup();
    }

    private void cleanup() {
        dialogueTurnRepository.deleteAll();
        dialogueStateRepository.deleteAll();
        leadRepository.deleteAll();
        campaignRepository.deleteAll();
        tgAccountRepository.deleteAll();
    }

    @Test
    public void testSpintaxEvaluation_randomizedOutputAndSeededDeterministicOutput() {
        // Given a spintax string '{Hi|Hello}'
        String template = "{Hi|Hello}";

        // Seeded random for reproducibility (Seed 1 gives one result, Seed 10 gives another)
        Random rng1 = new Random(1);
        Random rng2 = new Random(10);

        String resolved1 = spintaxService.evaluate(template, rng1);
        String resolved2 = spintaxService.evaluate(template, rng2);

        // Verify the outputs are selected from options and are deterministic given seeds
        assertThat(resolved1).isIn("Hi", "Hello");
        assertThat(resolved2).isIn("Hi", "Hello");

        // Verify nested spintax
        String nestedTemplate = "{Hello {there|friend}|Hi}";
        Random rng3 = new Random(3); // Nested evaluation with specific seed
        String resolvedNested = spintaxService.evaluate(nestedTemplate, rng3);
        assertThat(resolvedNested).isIn("Hello there", "Hello friend", "Hi");
    }

    @Test
    public void testAccountRotation_uponFloodWaitError() {
        // Given a campaign and a lead
        Campaign campaign = new Campaign("Promo Campaign", "Promo", "ACTIVE");
        campaign = campaignRepository.save(campaign);

        Lead lead = new Lead("john_doe", "+12345", "PENDING", "meta");
        campaign.addLead(lead);
        lead = leadRepository.save(lead);

        // And two active TGAccounts
        TGAccount account1 = new TGAccount("+1001", "ACTIVE");
        TGAccount account2 = new TGAccount("+1002", "ACTIVE");
        account1 = tgAccountRepository.save(account1);
        account2 = tgAccountRepository.save(account2);

        // When dispatching: first account throws FloodWaitException, second account succeeds
        doAnswer(invocation -> {
            TGAccount acc = invocation.getArgument(0);
            if ("+1001".equals(acc.getPhoneNumber())) {
                throw new FloodWaitException("Flood limit reached on first account", 60);
            }
            return null;
        }).when(telegramClient).sendMessage(any(TGAccount.class), any(), any());

        // Trigger dispatch
        campaignEngineService.dispatchMessage(lead.getId(), "{Hi|Hello}", new Random(42));

        // Then:
        // 1. First account status must be rotated/updated to FLOOD_WAIT
        TGAccount updatedAccount1 = tgAccountRepository.findById(account1.getId()).orElseThrow();
        assertThat(updatedAccount1.getStatus()).isEqualTo("FLOOD_WAIT");

        // 2. Second account status remains ACTIVE
        TGAccount updatedAccount2 = tgAccountRepository.findById(account2.getId()).orElseThrow();
        assertThat(updatedAccount2.getStatus()).isEqualTo("ACTIVE");

        // 3. Lead status must be updated to DISPATCHED
        Lead updatedLead = leadRepository.findById(lead.getId()).orElseThrow();
        assertThat(updatedLead.getStatus()).isEqualTo("DISPATCHED");
    }

    @Test
    public void testDialogueStoppage_whenSessionReaches8BackAndForthMessages() {
        // Given a dialogue state
        DialogueState state = new DialogueState();
        state = dialogueStateRepository.save(state);

        // Let's add 7 dialogue turns first (less than 8)
        for (int i = 1; i <= 7; i++) {
            campaignEngineService.processMessageInDialogue(state.getId(), i % 2 == 0 ? "AI" : "HUMAN", "Message " + i);
        }

        // Fetch dialogue turns to confirm size is 7
        List<DialogueTurn> initialTurns = dialogueTurnRepository.findByDialogueStateIdOrderByTimestampAsc(state.getId());
        assertThat(initialTurns).hasSize(7);

        // When the 8th message is added
        DialogueState finalState = state;
        DialogueBlockedException exception = assertThrows(DialogueBlockedException.class, () -> {
            campaignEngineService.processMessageInDialogue(finalState.getId(), "AI", "Message 8");
        });

        // Then it should throw DialogueBlockedException and set humanInterventionRequired to true
        assertThat(exception.getMessage()).contains("limit of 8");

        DialogueState updatedState = dialogueStateRepository.findById(state.getId()).orElseThrow();
        assertThat(updatedState.isHumanInterventionRequired()).isTrue();

        // Any further message must be blocked immediately
        DialogueBlockedException subsequentException = assertThrows(DialogueBlockedException.class, () -> {
            campaignEngineService.processMessageInDialogue(finalState.getId(), "HUMAN", "Message 9");
        });
        assertThat(subsequentException.getMessage()).contains("blocked");
    }
}

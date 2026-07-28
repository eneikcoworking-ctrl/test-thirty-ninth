package com.eneik.generated;

import com.eneik.generated.entity.TGAccount;
import com.eneik.generated.model.DialogueState;
import com.eneik.generated.repository.DialogueStateRepository;
import com.eneik.generated.repository.TGAccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DialogueInboxViewTest {

    @Autowired
    private TGAccountRepository tgAccountRepository;

    @Autowired
    private DialogueStateRepository dialogueStateRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testUnifiedInboxView_queriesCorrectlyAndUsesOptimizedIndex() {
        // 1. Arrange: Create TG Accounts
        TGAccount account1 = new TGAccount("+12223334444", "Active");
        TGAccount account2 = new TGAccount("+15556667777", "Active");
        tgAccountRepository.saveAndFlush(account1);
        tgAccountRepository.saveAndFlush(account2);

        // 2. Arrange: Create dialogues with different statuses
        LocalDateTime now = LocalDateTime.now();

        DialogueState ds1 = new DialogueState();
        ds1.setTgAccount(account1);
        ds1.setStatus("ACTIVE");
        ds1.setUpdatedAt(now.minusMinutes(10));
        dialogueStateRepository.save(ds1);

        DialogueState ds2 = new DialogueState();
        ds2.setTgAccount(account1);
        ds2.setStatus("PENDING_HUMAN");
        ds2.setUpdatedAt(now.minusMinutes(5));
        dialogueStateRepository.save(ds2);

        DialogueState ds3 = new DialogueState();
        ds3.setTgAccount(account2);
        ds3.setStatus("ACTIVE");
        ds3.setUpdatedAt(now);
        dialogueStateRepository.save(ds3);

        // 2.5 Arrange: Seed thousands of additional dialogues to fulfill "thousands of messages" Acceptance Criteria
        List<DialogueState> bulkList = new ArrayList<>();
        for (int i = 4; i <= 2003; i++) {
            TGAccount selectedAccount = (i % 2 == 0) ? account1 : account2;
            String status = (i % 3 == 0) ? "ACTIVE" : "PENDING_HUMAN";
            DialogueState ds = new DialogueState();
            ds.setTgAccount(selectedAccount);
            ds.setStatus(status);
            ds.setUpdatedAt(now.minusMinutes(100 + i));
            ds.setAiTurnsCount(i % 5);
            ds.setHumanInterventionRequired(i % 4 == 0);
            bulkList.add(ds);
        }
        dialogueStateRepository.saveAll(bulkList);

        dialogueStateRepository.flush();
        entityManager.clear();

        // 3. Act: Query the unified view
        List<?> results = entityManager.createNativeQuery(
            "SELECT * FROM unified_inbox_view WHERE dialogue_status = 'ACTIVE' ORDER BY last_activity_at DESC"
        ).getResultList();

        // 4. Assert: Correct number of rows and order (ds3 then ds1)
        // Since we added 2000 records, let's verify total count matching 'ACTIVE'
        // Specifically, ds1, ds3 are ACTIVE (2) plus bulkList records where i % 3 == 0
        // i from 4 to 2003. Total count of divisibles is 666.
        // Total should be 666 + 2 = 668.
        assertThat(results).hasSize(668);

        Object[] firstRow = (Object[]) results.get(0);
        // columns: dialogue_id, tg_account_id, account_phone_number, dialogue_status, ai_turns_count, human_intervention_required, last_activity_at
        assertThat(firstRow[2].toString()).isEqualTo("+15556667777");
        assertThat(firstRow[3].toString()).isEqualTo("ACTIVE");

        Object[] secondRow = (Object[]) results.get(1);
        assertThat(secondRow[2].toString()).isEqualTo("+12223334444");
        assertThat(secondRow[3].toString()).isEqualTo("ACTIVE");

        // 5. Act & Assert: Check the execution plan uses our composite index
        String explainPlan = (String) entityManager.createNativeQuery(
            "EXPLAIN SELECT * FROM unified_inbox_view WHERE dialogue_status = 'ACTIVE' ORDER BY last_activity_at DESC"
        ).getSingleResult();

        System.out.println("EXPLAIN PLAN: " + explainPlan);

        // Assert that our composite index idx_dialogue_state_status_time is used in the execution plan
        assertThat(explainPlan).containsIgnoringCase("idx_dialogue_state_status_time");
    }
}

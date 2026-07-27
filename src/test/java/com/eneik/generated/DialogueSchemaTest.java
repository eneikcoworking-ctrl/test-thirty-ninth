package com.eneik.generated;

import com.eneik.generated.domain.Conversation;
import com.eneik.generated.domain.TelegramAccount;
import com.eneik.generated.repository.ConversationRepository;
import com.eneik.generated.repository.TelegramAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class DialogueSchemaTest {

    @Autowired
    private TelegramAccountRepository accountRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testSchemaAndIndexUsage() {
        // 1. Arrange - Seed data using repositories with a reproducible seeding mechanism
        TelegramAccount account1 = new TelegramAccount("acc-001", "+123456789", "agent_tom", "ACTIVE");
        TelegramAccount account2 = new TelegramAccount("acc-002", "+987654321", "agent_alice", "ACTIVE");

        accountRepository.save(account1);
        accountRepository.save(account2);

        LocalDateTime now = LocalDateTime.of(2026, 7, 27, 20, 13, 42);

        // Seed several initial hand-crafted conversations
        Conversation conv1 = new Conversation("conv-001", account1, "lead_john", "+111111", "IN_PROGRESS", now);
        Conversation conv2 = new Conversation("conv-002", account2, "lead_doe", "+222222", "COMPLETED", now.plusHours(1));
        Conversation conv3 = new Conversation("conv-003", account1, "lead_jane", "+333333", "IN_PROGRESS", now.plusHours(2));

        conversationRepository.save(conv1);
        conversationRepository.save(conv2);
        conversationRepository.save(conv3);

        // Seeding thousands of messages to fulfill the "Given thousands of messages" Acceptance Criteria.
        // We will insert 2000 additional conversation records to simulate production scale.
        List<Conversation> bulkList = new ArrayList<>();
        for (int i = 4; i <= 2003; i++) {
            TelegramAccount selectedAccount = (i % 2 == 0) ? account1 : account2;
            String status = (i % 3 == 0) ? "IN_PROGRESS" : "COMPLETED";
            bulkList.add(new Conversation(
                    "conv-" + String.format("%04d", i),
                    selectedAccount,
                    "lead_" + i,
                    "+100" + i,
                    status,
                    now.plusMinutes(i)
            ));
        }
        conversationRepository.saveAll(bulkList);

        // 2. Act & Assert - Fetch and verify mapped JPA entities and total counts
        long totalConversations = conversationRepository.count();
        assertThat(totalConversations).isEqualTo(2003);

        // 3. Act & Assert - Query the custom view and verify correctness
        List<Map<String, Object>> viewRows = jdbcTemplate.queryForList("SELECT * FROM unified_inbox_view ORDER BY conversation_updated_at DESC");
        assertThat(viewRows.size()).isEqualTo(2003);

        Map<String, Object> latestRow = viewRows.get(0);
        assertThat(latestRow.get("CONVERSATION_ID")).isEqualTo("conv-2003");
        assertThat(latestRow.get("LEAD_USERNAME")).isEqualTo("lead_2003");

        // 4. Act & Assert - Run EXPLAIN to verify the index is used on status filter queries with thousands of records
        // H2 "EXPLAIN" output contains the execution plan description which mentions the index name if used.
        List<Map<String, Object>> explainRows = jdbcTemplate.queryForList(
                "EXPLAIN SELECT * FROM conversations WHERE status = 'IN_PROGRESS' ORDER BY updated_at DESC"
        );
        assertThat(explainRows).isNotEmpty();
        String plan = (String) explainRows.get(0).values().iterator().next();

        // Assert that index 'IDX_CONV_STATUS_UPDATED_AT' is used in the plan (ignoring case)
        assertThat(plan.toUpperCase()).contains("IDX_CONV_STATUS_UPDATED_AT");
    }
}

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

        Conversation conv1 = new Conversation("conv-001", account1, "lead_john", "+111111", "IN_PROGRESS", now);
        Conversation conv2 = new Conversation("conv-002", account2, "lead_doe", "+222222", "COMPLETED", now.plusHours(1));
        Conversation conv3 = new Conversation("conv-003", account1, "lead_jane", "+333333", "IN_PROGRESS", now.plusHours(2));

        conversationRepository.save(conv1);
        conversationRepository.save(conv2);
        conversationRepository.save(conv3);

        // 2. Act & Assert - Fetch and verify mapped JPA entities
        List<Conversation> allConversations = conversationRepository.findAll();
        assertThat(allConversations).hasSize(3);

        // 3. Act & Assert - Query the custom view
        List<Map<String, Object>> viewRows = jdbcTemplate.queryForList("SELECT * FROM unified_inbox_view ORDER BY conversation_updated_at DESC");
        assertThat(viewRows).hasSize(3);

        Map<String, Object> latestRow = viewRows.get(0);
        assertThat(latestRow.get("CONVERSATION_ID")).isEqualTo("conv-003");
        assertThat(latestRow.get("LEAD_USERNAME")).isEqualTo("lead_jane");
        assertThat(latestRow.get("ACCOUNT_USERNAME")).isEqualTo("agent_tom");

        // 4. Act & Assert - Run EXPLAIN to verify the index is used on status filter queries
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

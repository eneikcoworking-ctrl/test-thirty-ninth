package com.eneik.generated;

import com.eneik.generated.controller.AiConfigurationController;
import com.eneik.generated.controller.AiConfigurationController.*;
import com.eneik.generated.entity.AiConfiguration;
import com.eneik.generated.repository.AiConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class AiConfigurationTest {

    @Autowired
    private AiConfigurationRepository repository;

    @Autowired
    private AiConfigurationController controller;

    @Test
    public void testGetAndUpdateConfiguration() {
        // Retrieve default configuration (should be created via DB seed or controller default fallback)
        ResponseEntity<AiConfigurationResponse> getResponse = controller.getConfiguration();
        assertThat(getResponse.getStatusCode().is2xxSuccessful()).isTrue();
        AiConfigurationResponse config = getResponse.getBody();
        assertThat(config).isNotNull();
        assertThat(config.getSystemPrompt()).contains("highly analytical AI assistant");
        assertThat(config.getStopTriggers()).hasSize(3);
        assertThat(config.getStopTriggers().get(0).getKeyword()).isEqualTo("Exit");
        assertThat(config.getStopTriggers().get(0).isEnabled()).isTrue();
        assertThat(config.getModelVersion()).isEqualTo("GPT-4-Turbo");

        // Update full configuration
        AiConfigurationRequest updateBody = new AiConfigurationRequest();
        updateBody.setSystemPrompt("New custom prompt containing placeholder {userName}.");
        updateBody.setStopTriggers(List.of(
            new StopTrigger("Exit", true),
            new StopTrigger("Cancel", true),
            new StopTrigger("Error", true),
            new StopTrigger("Halt", true)
        ));
        updateBody.setModelVersion("Claude-3.5-S");

        ResponseEntity<AiConfigurationResponse> updateResponse = controller.updateConfiguration(updateBody);
        assertThat(updateResponse.getStatusCode().is2xxSuccessful()).isTrue();

        AiConfigurationResponse updated = updateResponse.getBody();
        assertThat(updated).isNotNull();
        assertThat(updated.getSystemPrompt()).isEqualTo("New custom prompt containing placeholder {userName}.");
        assertThat(updated.getStopTriggers()).hasSize(4);
        assertThat(updated.getStopTriggers().get(3).getKeyword()).isEqualTo("Halt");
        assertThat(updated.getModelVersion()).isEqualTo("Claude-3.5-S");

        // Verify it was persisted in the DB
        AiConfiguration dbFetched = repository.findById(updated.getId()).orElseThrow();
        assertThat(dbFetched.getSystemPrompt()).isEqualTo("New custom prompt containing placeholder {userName}.");
    }

    @Test
    public void testUpdateStopTriggersOnly() {
        List<StopTrigger> list = List.of(
            new StopTrigger("Exit", true),
            new StopTrigger("Abort", true)
        );

        ResponseEntity<AiConfigurationResponse> response = controller.updateStopTriggers(list);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        AiConfigurationResponse updated = response.getBody();
        assertThat(updated).isNotNull();
        assertThat(updated.getStopTriggers()).hasSize(2);
        assertThat(updated.getStopTriggers().get(1).getKeyword()).isEqualTo("Abort");

        // System prompt and model should remain unchanged
        assertThat(updated.getSystemPrompt()).contains("highly analytical AI assistant");
    }
}

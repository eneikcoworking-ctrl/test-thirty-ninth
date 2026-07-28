package com.eneik.generated.ai;

public interface LlmClient {
    /**
     * Sends a prompt to the LLM and returns the generated response.
     * @param prompt the prompt text
     * @return the generated response text
     * @throws Exception if an error occurs during generation
     */
    String generateResponse(String prompt) throws Exception;
}

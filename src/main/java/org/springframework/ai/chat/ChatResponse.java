package org.springframework.ai.chat;

public class ChatResponse {
    private final Generation result;

    public ChatResponse(Generation result) {
        this.result = result;
    }

    public Generation getResult() {
        return result;
    }
}

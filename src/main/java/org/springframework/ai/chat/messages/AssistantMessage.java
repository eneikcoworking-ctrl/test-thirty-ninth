package org.springframework.ai.chat.messages;

public class AssistantMessage {
    private final String content;

    public AssistantMessage(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}

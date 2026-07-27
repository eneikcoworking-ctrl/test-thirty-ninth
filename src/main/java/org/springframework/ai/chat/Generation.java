package org.springframework.ai.chat;

import org.springframework.ai.chat.messages.AssistantMessage;

public class Generation {
    private final AssistantMessage output;

    public Generation(String content) {
        this.output = new AssistantMessage(content);
    }

    public AssistantMessage getOutput() {
        return output;
    }
}

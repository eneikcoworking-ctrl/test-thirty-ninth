package org.springframework.ai.chat;

import org.springframework.ai.chat.prompt.Prompt;

public interface ChatClient {
    ChatResponse call(Prompt prompt);
}

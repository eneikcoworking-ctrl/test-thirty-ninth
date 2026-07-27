package org.springframework.ai.chat.prompt;

public class Prompt {
    private final String instructions;

    public Prompt(String instructions) {
        this.instructions = instructions;
    }

    public String getInstructions() {
        return instructions;
    }
}

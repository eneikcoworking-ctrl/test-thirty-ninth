package org.springframework.ai.chat.prompt;

import java.util.Map;

public class PromptTemplate {
    private final String template;

    public PromptTemplate(String template) {
        this.template = template;
    }

    public Prompt create(Map<String, Object> model) {
        String rendered = template;
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue().toString());
        }
        return new Prompt(rendered);
    }
}

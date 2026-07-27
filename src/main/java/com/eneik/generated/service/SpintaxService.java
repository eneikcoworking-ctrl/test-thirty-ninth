package com.eneik.generated.service;

import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SpintaxService {

    private final Random random;

    public SpintaxService() {
        this.random = new Random();
    }

    public SpintaxService(Random random) {
        this.random = random;
    }

    /**
     * Resolves a spintax string like "{Hi|Hello}" into a randomized selection.
     * Supports nested brackets like "{Hi {there|friend}|Hello}".
     */
    public String evaluate(String text) {
        return evaluate(text, this.random);
    }

    /**
     * Resolves a spintax string using a custom/seeded Random generator.
     */
    public String evaluate(String text, Random rng) {
        if (text == null) {
            return null;
        }

        // Match the innermost curly-braces {choice1|choice2} that do not contain curly-braces inside.
        // This allows recursive/nested resolution from inside out.
        Pattern pattern = Pattern.compile("\\{([^{}]+)\\}");
        String result = text;

        while (true) {
            Matcher matcher = pattern.matcher(result);
            if (!matcher.find()) {
                break;
            }
            String group = matcher.group(1);
            // Split by '|', keeping empty trailing elements if any
            String[] choices = group.split("\\|", -1);
            int idx = rng.nextInt(choices.length);
            String chosen = choices[idx];
            result = result.substring(0, matcher.start()) + chosen + result.substring(matcher.end());
        }

        return result;
    }
}

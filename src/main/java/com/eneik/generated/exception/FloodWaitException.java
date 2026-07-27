package com.eneik.generated.exception;

public class FloodWaitException extends TelegramException {
    private final int retryAfterSeconds;

    public FloodWaitException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

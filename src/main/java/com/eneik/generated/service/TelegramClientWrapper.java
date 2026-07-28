package com.eneik.generated.service;

import com.eneik.generated.entity.Proxy;

public interface TelegramClientWrapper {
    /**
     * Authenticates with OTP code and returns session data representation.
     */
    String authenticateWithOtp(String phoneNumber, String otp, Proxy proxy);

    /**
     * Authenticates using a file (representing session/tdata) and returns session data representation.
     */
    String authenticateWithFile(String phoneNumber, String fileContent, String fileType, Proxy proxy);

    /**
     * Verifies if a session is currently active and healthy or needs re-authorization.
     */
    boolean checkHealth(String sessionData, Proxy proxy);
}

package com.eneik.generated.service;

import com.eneik.generated.entity.Proxy;
import org.springframework.stereotype.Service;

@Service
public class DefaultTelegramClientWrapper implements TelegramClientWrapper {

    @Override
    public String authenticateWithOtp(String phoneNumber, String otp, Proxy proxy) {
        if (otp == null || otp.trim().isEmpty() || otp.equals("INVALID")) {
            throw new IllegalArgumentException("Invalid OTP code supplied.");
        }
        // Simulate TDLib/GramJS session generation using proxy configurations
        String proxyInfo = proxy != null ? proxy.getHost() + ":" + proxy.getPort() : "no-proxy";
        return "session_otp_" + phoneNumber + "_" + proxyInfo;
    }

    @Override
    public String authenticateWithFile(String phoneNumber, String fileContent, String fileType, Proxy proxy) {
        if (fileContent == null || fileContent.trim().isEmpty() || fileContent.contains("CORRUPTED")) {
            throw new IllegalArgumentException("Corrupted or empty session file.");
        }
        String proxyInfo = proxy != null ? proxy.getHost() + ":" + proxy.getPort() : "no-proxy";
        return "session_file_" + fileType + "_" + phoneNumber + "_" + proxyInfo;
    }

    @Override
    public boolean checkHealth(String sessionData, Proxy proxy) {
        if (sessionData == null || sessionData.contains("REAUTH_NEEDED") || sessionData.contains("DISCONNECTED")) {
            return false;
        }
        return true;
    }
}

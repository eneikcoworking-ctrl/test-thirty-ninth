package com.eneik.generated.service;

import com.eneik.generated.entity.AppUser;
import com.eneik.generated.entity.Proxy;
import com.eneik.generated.entity.TGAccount;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TGAccountRepository;
import com.eneik.generated.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TelegramSessionService {

    private final TGAccountRepository tgAccountRepository;
    private final ProxyRepository proxyRepository;
    private final UserRepository userRepository;
    private final TelegramClientWrapper telegramClientWrapper;

    public TelegramSessionService(
            TGAccountRepository tgAccountRepository,
            ProxyRepository proxyRepository,
            UserRepository userRepository,
            TelegramClientWrapper telegramClientWrapper) {
        this.tgAccountRepository = tgAccountRepository;
        this.proxyRepository = proxyRepository;
        this.userRepository = userRepository;
        this.telegramClientWrapper = telegramClientWrapper;
    }

    @Transactional
    public TGAccount onboardWithOtp(String phoneNumber, String otp, Long proxyId, Long userId) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }

        Proxy proxy = null;
        if (proxyId != null) {
            proxy = proxyRepository.findById(proxyId)
                    .orElseThrow(() -> new IllegalArgumentException("Proxy not found with id: " + proxyId));
        } else {
            throw new IllegalArgumentException("Mandatory proxy assignment is required.");
        }

        AppUser user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        }

        // Connect/authenticate through the assigned proxy
        String sessionData = telegramClientWrapper.authenticateWithOtp(phoneNumber, otp, proxy);

        // Find or create the account
        TGAccount account = tgAccountRepository.findByPhoneNumber(phoneNumber)
                .orElse(new TGAccount(phoneNumber, "Active"));

        // Check unique proxy constraint
        if (account.getProxy() == null || !account.getProxy().getId().equals(proxyId)) {
            for (TGAccount existing : tgAccountRepository.findAll()) {
                if (existing.getProxy() != null && existing.getProxy().getId().equals(proxyId)
                        && !existing.getId().equals(account.getId())) {
                    throw new IllegalStateException("Proxy with ID " + proxyId + " is already assigned to another account.");
                }
            }
        }

        account.setProxy(proxy);
        account.setUser(user);
        account.setSessionData(sessionData);
        account.setSessionType("OTP");
        account.setStatus("Active");

        return tgAccountRepository.save(account);
    }

    @Transactional
    public TGAccount onboardWithFile(String phoneNumber, String fileContent, String fileType, Long proxyId, Long userId) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }

        Proxy proxy = null;
        if (proxyId != null) {
            proxy = proxyRepository.findById(proxyId)
                    .orElseThrow(() -> new IllegalArgumentException("Proxy not found with id: " + proxyId));
        } else {
            throw new IllegalArgumentException("Mandatory proxy assignment is required.");
        }

        AppUser user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        }

        // Connect/authenticate through the assigned proxy
        String sessionData = telegramClientWrapper.authenticateWithFile(phoneNumber, fileContent, fileType, proxy);

        // Find or create the account
        TGAccount account = tgAccountRepository.findByPhoneNumber(phoneNumber)
                .orElse(new TGAccount(phoneNumber, "Active"));

        // Check unique proxy constraint
        if (account.getProxy() == null || !account.getProxy().getId().equals(proxyId)) {
            for (TGAccount existing : tgAccountRepository.findAll()) {
                if (existing.getProxy() != null && existing.getProxy().getId().equals(proxyId)
                        && !existing.getId().equals(account.getId())) {
                    throw new IllegalStateException("Proxy with ID " + proxyId + " is already assigned to another account.");
                }
            }
        }

        account.setProxy(proxy);
        account.setUser(user);
        account.setSessionData(sessionData);
        account.setSessionType("FILE_" + fileType.toUpperCase());
        account.setStatus("Active");

        return tgAccountRepository.save(account);
    }

    @Transactional
    public TGAccount runHealthCheck(Long accountId) {
        TGAccount account = tgAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + accountId));

        boolean isHealthy = telegramClientWrapper.checkHealth(account.getSessionData(), account.getProxy());

        if (!isHealthy) {
            String originalStatus = account.getStatus();
            int updatedRows = tgAccountRepository.updateStatusAtomic(account.getId(), originalStatus, "Re-authorization Required");
            if (updatedRows > 0) {
                account.setStatus("Re-authorization Required");
            } else {
                account = tgAccountRepository.findById(accountId)
                        .orElseThrow(() -> new IllegalArgumentException("Account not found during refresh: " + accountId));
            }
        } else {
            if ("Re-authorization Required".equals(account.getStatus())) {
                int updatedRows = tgAccountRepository.updateStatusAtomic(account.getId(), "Re-authorization Required", "Active");
                if (updatedRows > 0) {
                    account.setStatus("Active");
                }
            }
        }

        return account;
    }
}

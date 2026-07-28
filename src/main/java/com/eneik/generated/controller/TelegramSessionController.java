package com.eneik.generated.controller;

import com.eneik.generated.entity.TGAccount;
import com.eneik.generated.service.TelegramSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telegram")
public class TelegramSessionController {

    private final TelegramSessionService telegramSessionService;

    public TelegramSessionController(TelegramSessionService telegramSessionService) {
        this.telegramSessionService = telegramSessionService;
    }

    @PostMapping("/auth/otp")
    public ResponseEntity<TGAccountResponse> onboardWithOtp(@RequestBody OtpRequest request) {
        TGAccount account = telegramSessionService.onboardWithOtp(
                request.getPhoneNumber(),
                request.getOtp(),
                request.getProxyId(),
                request.getUserId()
        );
        return ResponseEntity.ok(toResponse(account));
    }

    @PostMapping("/auth/file")
    public ResponseEntity<TGAccountResponse> onboardWithFile(@RequestBody FileRequest request) {
        TGAccount account = telegramSessionService.onboardWithFile(
                request.getPhoneNumber(),
                request.getFileContent(),
                request.getFileType(),
                request.getProxyId(),
                request.getUserId()
        );
        return ResponseEntity.ok(toResponse(account));
    }

    @PostMapping("/health-check")
    public ResponseEntity<TGAccountResponse> runHealthCheck(@RequestBody HealthCheckRequest request) {
        TGAccount account = telegramSessionService.runHealthCheck(request.getAccountId());
        return ResponseEntity.ok(toResponse(account));
    }

    private TGAccountResponse toResponse(TGAccount account) {
        TGAccountResponse response = new TGAccountResponse();
        response.setId(account.getId());
        response.setPhoneNumber(account.getPhoneNumber());
        response.setStatus(account.getStatus());
        response.setSessionType(account.getSessionType());
        if (account.getProxy() != null) {
            response.setProxyId(account.getProxy().getId());
        }
        if (account.getUser() != null) {
            response.setUserId(account.getUser().getId());
        }
        return response;
    }

    public static class OtpRequest {
        private String phoneNumber;
        private String otp;
        private Long proxyId;
        private Long userId;

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getOtp() {
            return otp;
        }

        public void setOtp(String otp) {
            this.otp = otp;
        }

        public Long getProxyId() {
            return proxyId;
        }

        public void setProxyId(Long proxyId) {
            this.proxyId = proxyId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }

    public static class FileRequest {
        private String phoneNumber;
        private String fileContent;
        private String fileType;
        private Long proxyId;
        private Long userId;

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getFileContent() {
            return fileContent;
        }

        public void setFileContent(String fileContent) {
            this.fileContent = fileContent;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public Long getProxyId() {
            return proxyId;
        }

        public void setProxyId(Long proxyId) {
            this.proxyId = proxyId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }

    public static class HealthCheckRequest {
        private Long accountId;

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }
    }

    public static class TGAccountResponse {
        private Long id;
        private String phoneNumber;
        private String status;
        private String sessionType;
        private Long proxyId;
        private Long userId;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getSessionType() {
            return sessionType;
        }

        public void setSessionType(String sessionType) {
            this.sessionType = sessionType;
        }

        public Long getProxyId() {
            return proxyId;
        }

        public void setProxyId(Long proxyId) {
            this.proxyId = proxyId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
}

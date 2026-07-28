package com.eneik.generated.controller;

import com.eneik.generated.entity.AppUser;
import com.eneik.generated.entity.Proxy;
import com.eneik.generated.entity.TGAccount;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TGAccountRepository;
import com.eneik.generated.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AccountController {

    private final TGAccountRepository accountRepository;
    private final ProxyRepository proxyRepository;
    private final UserRepository userRepository;

    // Temporary session onboarding maps (in-memory) for demonstration and OTP flow
    private final Map<String, OtpSession> otpSessions = new HashMap<>();

    public AccountController(TGAccountRepository accountRepository, ProxyRepository proxyRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.proxyRepository = proxyRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void seedInitialData() {
        if (accountRepository.count() == 0) {
            AppUser defaultUser = userRepository.findAll().stream()
                    .filter(u -> "admin_operator".equals(u.getUsername()))
                    .findFirst()
                    .orElseGet(() -> userRepository.save(new AppUser("admin_operator")));

            Proxy proxy1 = new Proxy("192.168.1.50", 1080, "SOCKS5");
            proxy1.setUsername("socks_usr");
            proxy1.setPassword("socks_pwd");
            proxy1 = proxyRepository.save(proxy1);

            Proxy proxy2 = new Proxy("192.168.1.51", 8080, "HTTP");
            proxy2 = proxyRepository.save(proxy2);

            TGAccount acc1 = new TGAccount("+12345678901", "Active");
            acc1.setUser(defaultUser);
            acc1.setProxy(proxy1);
            accountRepository.save(acc1);

            TGAccount acc2 = new TGAccount("+19876543210", "Temporary Spam-Block");
            acc2.setUser(defaultUser);
            acc2.setProxy(proxy2);
            accountRepository.save(acc2);

            TGAccount acc3 = new TGAccount("+447911123456", "Re-authorization Required");
            acc3.setUser(defaultUser);
            accountRepository.save(acc3);
        }
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<Map<String, Object>>> listAccounts() {
        List<TGAccount> accounts = accountRepository.findAll();
        List<Map<String, Object>> response = new ArrayList<>();
        for (TGAccount acc : accounts) {
            response.add(mapAccountToResponse(acc));
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<?> getAccount(@PathVariable("accountId") Long accountId) {
        Optional<TGAccount> accOpt = accountRepository.findById(accountId);
        if (accOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", "Account not found.", "timestamp", Instant.now().toString()));
        }
        return ResponseEntity.ok(mapAccountToResponse(accOpt.get()));
    }

    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<?> deleteAccount(@PathVariable("accountId") Long accountId) {
        Optional<TGAccount> accOpt = accountRepository.findById(accountId);
        if (accOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", "Account not found.", "timestamp", Instant.now().toString()));
        }
        accountRepository.deleteById(accountId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/accounts/onboard/otp-request")
    public ResponseEntity<?> requestOtp(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        if (phoneNumber == null || !phoneNumber.matches("^\\+[1-9]\\d{1,14}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", 400, "message", "Invalid phone number format.", "timestamp", Instant.now().toString()));
        }

        String referenceId = "onb-" + UUID.randomUUID().toString();
        otpSessions.put(referenceId, new OtpSession(phoneNumber, "12345")); // Hardcoded valid verification code for testing

        return ResponseEntity.ok(Map.of(
                "referenceId", referenceId,
                "status", "PENDING_OTP"
        ));
    }

    @PostMapping("/accounts/onboard/otp-verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String referenceId = request.get("referenceId");
        String code = request.get("code");

        OtpSession session = otpSessions.get(referenceId);
        if (session == null || !session.code.equals(code)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", 400, "message", "Invalid or expired OTP code or reference ID.", "timestamp", Instant.now().toString()));
        }

        // Finalize onboarding by saving account to database
        AppUser defaultUser = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            AppUser u = new AppUser("admin_operator");
            return userRepository.save(u);
        });

        TGAccount newAcc = new TGAccount(session.phoneNumber, "Active");
        newAcc.setUser(defaultUser);
        TGAccount savedAcc = accountRepository.save(newAcc);

        otpSessions.remove(referenceId);

        return ResponseEntity.ok(mapAccountToResponse(savedAcc));
    }

    @PostMapping("/accounts/onboard/file")
    public ResponseEntity<?> onboardByFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("format") String format,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", 400, "message", "Uploaded file is empty.", "timestamp", Instant.now().toString()));
        }

        if (!"SESSION".equalsIgnoreCase(format) && !"TDATA_ZIP".equalsIgnoreCase(format)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", 400, "message", "Unsupported file format.", "timestamp", Instant.now().toString()));
        }

        String targetPhone = (phoneNumber != null && !phoneNumber.trim().isEmpty()) ? phoneNumber : "+1555" + (100000 + new Random().nextInt(900000));

        AppUser defaultUser = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            AppUser u = new AppUser("admin_operator");
            return userRepository.save(u);
        });

        TGAccount newAcc = new TGAccount(targetPhone, "Active");
        newAcc.setUser(defaultUser);
        TGAccount savedAcc = accountRepository.save(newAcc);

        return ResponseEntity.ok(mapAccountToResponse(savedAcc));
    }

    @PutMapping("/accounts/{accountId}/proxy")
    public ResponseEntity<?> bindProxy(@PathVariable("accountId") Long accountId, @RequestBody Map<String, Object> request) {
        Optional<TGAccount> accOpt = accountRepository.findById(accountId);
        if (accOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", "Account not found.", "timestamp", Instant.now().toString()));
        }

        TGAccount acc = accOpt.get();

        String host = (String) request.get("host");
        Integer port = (Integer) request.get("port");
        String type = (String) request.get("type");
        String username = (String) request.get("username");
        String password = (String) request.get("password");

        if (host == null || port == null || type == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", 400, "message", "Invalid proxy parameters.", "timestamp", Instant.now().toString()));
        }

        // Strict Isolated Proxy Binding Constraints check
        List<TGAccount> allAccs = accountRepository.findAll();
        for (TGAccount other : allAccs) {
            if (!other.getId().equals(acc.getId()) && other.getProxy() != null) {
                Proxy p = other.getProxy();
                if (p.getHost().equalsIgnoreCase(host) && p.getPort() == port) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("status", 409, "message", "Proxy collision. The proxy is already bound to another active session.", "timestamp", Instant.now().toString()));
                }
            }
        }

        // Clear existing proxy association on this account first if any
        Proxy oldProxy = acc.getProxy();
        acc.setProxy(null);
        accountRepository.saveAndFlush(acc);

        if (oldProxy != null) {
            proxyRepository.delete(oldProxy);
        }

        Proxy newProxy = new Proxy(host, port, type);
        newProxy.setUsername(username);
        newProxy.setPassword(password);
        newProxy = proxyRepository.save(newProxy);

        acc.setProxy(newProxy);
        TGAccount updatedAcc = accountRepository.save(acc);

        return ResponseEntity.ok(mapAccountToResponse(updatedAcc));
    }

    @PutMapping("/accounts/{accountId}/status")
    public ResponseEntity<?> updateAccountStatus(@PathVariable("accountId") Long accountId, @RequestBody Map<String, String> request) {
        Optional<TGAccount> accOpt = accountRepository.findById(accountId);
        if (accOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", "Account not found.", "timestamp", Instant.now().toString()));
        }

        String status = request.get("status");
        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", 400, "message", "Status cannot be empty.", "timestamp", Instant.now().toString()));
        }

        TGAccount acc = accOpt.get();
        acc.setStatus(status);
        TGAccount updatedAcc = accountRepository.save(acc);

        return ResponseEntity.ok(mapAccountToResponse(updatedAcc));
    }

    private Map<String, Object> mapAccountToResponse(TGAccount acc) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", acc.getId());
        resp.put("phoneNumber", acc.getPhoneNumber());
        resp.put("status", acc.getStatus());
        if (acc.getUser() != null) {
            resp.put("userId", acc.getUser().getId());
        } else {
            resp.put("userId", null);
        }

        if (acc.getProxy() != null) {
            Map<String, Object> proxyInfo = new HashMap<>();
            proxyInfo.put("id", acc.getProxy().getId());
            proxyInfo.put("host", acc.getProxy().getHost());
            proxyInfo.put("port", acc.getProxy().getPort());
            proxyInfo.put("type", acc.getProxy().getType());
            proxyInfo.put("username", acc.getProxy().getUsername());
            resp.put("proxy", proxyInfo);
        } else {
            resp.put("proxy", null);
        }
        return resp;
    }

    private static class OtpSession {
        final String phoneNumber;
        final String code;

        OtpSession(String phoneNumber, String code) {
            this.phoneNumber = phoneNumber;
            this.code = code;
        }
    }
}

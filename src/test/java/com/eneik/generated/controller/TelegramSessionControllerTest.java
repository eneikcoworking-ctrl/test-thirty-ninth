package com.eneik.generated.controller;

import com.eneik.generated.entity.AppUser;
import com.eneik.generated.entity.Proxy;
import com.eneik.generated.entity.TGAccount;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TGAccountRepository;
import com.eneik.generated.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TelegramSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private TGAccountRepository tgAccountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private AppUser testUser;
    private Proxy testProxy1;
    private Proxy testProxy2;

    @BeforeEach
    public void setup() {
        tgAccountRepository.deleteAll();
        proxyRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new AppUser("test_operator");
        testUser = userRepository.save(testUser);

        testProxy1 = new Proxy("192.168.1.100", 1080, "SOCKS5");
        testProxy1 = proxyRepository.save(testProxy1);

        testProxy2 = new Proxy("192.168.1.101", 8080, "HTTP");
        testProxy2 = proxyRepository.save(testProxy2);
    }

    @Test
    public void testOnboardWithOtpSuccess() throws Exception {
        TelegramSessionController.OtpRequest request = new TelegramSessionController.OtpRequest();
        request.setPhoneNumber("+12345678901");
        request.setOtp("77777");
        request.setProxyId(testProxy1.getId());
        request.setUserId(testUser.getId());

        mockMvc.perform(post("/api/telegram/auth/otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.phoneNumber").value("+12345678901"))
                .andExpect(jsonPath("$.status").value("Active"))
                .andExpect(jsonPath("$.sessionType").value("OTP"))
                .andExpect(jsonPath("$.proxyId").value(testProxy1.getId()))
                .andExpect(jsonPath("$.userId").value(testUser.getId()));

        // Verify state is saved in DB
        TGAccount account = tgAccountRepository.findByPhoneNumber("+12345678901").orElse(null);
        assertNotNull(account);
        assertEquals("session_otp_+12345678901_192.168.1.100:1080", account.getSessionData());
    }

    @Test
    public void testOnboardWithFileSuccess() throws Exception {
        TelegramSessionController.FileRequest request = new TelegramSessionController.FileRequest();
        request.setPhoneNumber("+12345678902");
        request.setFileContent("some_valid_session_file_contents");
        request.setFileType("session");
        request.setProxyId(testProxy2.getId());
        request.setUserId(testUser.getId());

        mockMvc.perform(post("/api/telegram/auth/file")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+12345678902"))
                .andExpect(jsonPath("$.status").value("Active"))
                .andExpect(jsonPath("$.sessionType").value("FILE_SESSION"))
                .andExpect(jsonPath("$.proxyId").value(testProxy2.getId()));

        TGAccount account = tgAccountRepository.findByPhoneNumber("+12345678902").orElse(null);
        assertNotNull(account);
        assertEquals("session_file_session_+12345678902_192.168.1.101:8080", account.getSessionData());
    }

    @Test
    public void testOnboardRequiresProxy() throws Exception {
        TelegramSessionController.OtpRequest request = new TelegramSessionController.OtpRequest();
        request.setPhoneNumber("+12345678903");
        request.setOtp("12345");
        request.setProxyId(null); // No proxy, violating FEAT-ACC-02/mandatory assignment

        mockMvc.perform(post("/api/telegram/auth/otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testProxyMustBeUnique() throws Exception {
        // Setup initial account using proxy 1
        TGAccount first = new TGAccount("+100001", "Active");
        first.setProxy(testProxy1);
        tgAccountRepository.save(first);

        // Try to onboard another account with same proxy 1
        TelegramSessionController.OtpRequest request = new TelegramSessionController.OtpRequest();
        request.setPhoneNumber("+100002");
        request.setOtp("12345");
        request.setProxyId(testProxy1.getId());

        mockMvc.perform(post("/api/telegram/auth/otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError()); // due to unique constraint or custom check
    }

    @Test
    public void testHealthCheckTransitionsToReauthorizationRequired() throws Exception {
        // Save an account that will fail health check
        TGAccount account = new TGAccount("+12345678904", "Active");
        account.setProxy(testProxy1);
        account.setSessionData("session_otp_+12345678904_192.168.1.100:1080_REAUTH_NEEDED");
        account = tgAccountRepository.save(account);

        TelegramSessionController.HealthCheckRequest request = new TelegramSessionController.HealthCheckRequest();
        request.setAccountId(account.getId());

        mockMvc.perform(post("/api/telegram/health-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Re-authorization Required"));

        // Verify database level update
        TGAccount updated = tgAccountRepository.findById(account.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("Re-authorization Required", updated.getStatus());
    }
}

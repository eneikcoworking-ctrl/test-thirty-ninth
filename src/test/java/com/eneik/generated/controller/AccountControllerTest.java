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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TGAccountRepository accountRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private AppUser testUser;

    @BeforeEach
    public void setUp() {
        accountRepository.deleteAll();
        proxyRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new AppUser("test_operator");
        testUser = userRepository.save(testUser);
    }

    @Test
    public void testListAccountsSeededSuccessfully() throws Exception {
        TGAccount acc = new TGAccount("+15550001111", "Active");
        acc.setUser(testUser);
        accountRepository.save(acc);

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].phoneNumber", is("+15550001111")))
                .andExpect(jsonPath("$[0].status", is("Active")));
    }

    @Test
    public void testGetAccountDetailsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Account not found")));
    }

    @Test
    public void testDeleteAccount() throws Exception {
        TGAccount acc = new TGAccount("+15550002222", "Active");
        acc.setUser(testUser);
        acc = accountRepository.save(acc);

        mockMvc.perform(delete("/api/v1/accounts/" + acc.getId()))
                .andExpect(status().isNoContent());

        assertTrue(accountRepository.findById(acc.getId()).isEmpty());
    }

    @Test
    public void testOtpOnboardingFlow() throws Exception {
        // Step 1: Request OTP
        Map<String, String> request = new HashMap<>();
        request.put("phoneNumber", "+15559990000");

        String responseStr = mockMvc.perform(post("/api/v1/accounts/onboard/otp-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceId", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING_OTP")))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> responseMap = objectMapper.readValue(responseStr, Map.class);
        String referenceId = (String) responseMap.get("referenceId");

        // Step 2: Verify OTP
        Map<String, String> verifyRequest = new HashMap<>();
        verifyRequest.put("referenceId", referenceId);
        verifyRequest.put("code", "12345"); // Seeded code is 12345

        mockMvc.perform(post("/api/v1/accounts/onboard/otp-verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber", is("+15559990000")))
                .andExpect(jsonPath("$.status", is("Active")));
    }

    @Test
    public void testOnboardByFile() throws Exception {
        MockMultipartFile sessionFile = new MockMultipartFile(
                "file",
                "test.session",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "dummy session bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/accounts/onboard/file")
                        .file(sessionFile)
                        .param("format", "SESSION")
                        .param("phoneNumber", "+15557778888"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber", is("+15557778888")))
                .andExpect(jsonPath("$.status", is("Active")));
    }

    @Test
    public void testBindProxy() throws Exception {
        TGAccount acc = new TGAccount("+15550003333", "Active");
        acc.setUser(testUser);
        acc = accountRepository.save(acc);

        Map<String, Object> proxyRequest = new HashMap<>();
        proxyRequest.put("host", "192.168.1.100");
        proxyRequest.put("port", 1085);
        proxyRequest.put("type", "SOCKS5");

        mockMvc.perform(put("/api/v1/accounts/" + acc.getId() + "/proxy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(proxyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proxy.host", is("192.168.1.100")))
                .andExpect(jsonPath("$.proxy.port", is(1085)))
                .andExpect(jsonPath("$.proxy.type", is("SOCKS5")));
    }

    @Test
    public void testBindProxyCollisionConflict() throws Exception {
        Proxy proxy = new Proxy("192.168.1.200", 1080, "SOCKS5");
        proxy = proxyRepository.save(proxy);

        TGAccount acc1 = new TGAccount("+15550004441", "Active");
        acc1.setProxy(proxy);
        acc1.setUser(testUser);
        accountRepository.save(acc1);

        TGAccount acc2 = new TGAccount("+15550004442", "Active");
        acc2.setUser(testUser);
        acc2 = accountRepository.save(acc2);

        Map<String, Object> proxyRequest = new HashMap<>();
        proxyRequest.put("host", "192.168.1.200");
        proxyRequest.put("port", 1080);
        proxyRequest.put("type", "SOCKS5");

        mockMvc.perform(put("/api/v1/accounts/" + acc2.getId() + "/proxy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(proxyRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Proxy collision")));
    }

    @Test
    public void testUpdateAccountStatus() throws Exception {
        TGAccount acc = new TGAccount("+15550005555", "Active");
        acc.setUser(testUser);
        acc = accountRepository.save(acc);

        Map<String, String> statusRequest = new HashMap<>();
        statusRequest.put("status", "Permanent Ban");

        mockMvc.perform(put("/api/v1/accounts/" + acc.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Permanent Ban")));

        TGAccount updated = accountRepository.findById(acc.getId()).orElse(null);
        assertEquals("Permanent Ban", updated.getStatus());
    }
}

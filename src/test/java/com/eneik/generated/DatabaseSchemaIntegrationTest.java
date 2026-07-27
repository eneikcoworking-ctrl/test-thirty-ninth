package com.eneik.generated;

import com.eneik.generated.entity.AppUser;
import com.eneik.generated.entity.Proxy;
import com.eneik.generated.entity.TGAccount;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TGAccountRepository;
import com.eneik.generated.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DatabaseSchemaIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TGAccountRepository tgAccountRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Test
    @Transactional
    public void testUserToMultipleAccountsRelationship() {
        // Given an empty database (implicitly within transaction context per test run, or fresh user records)
        AppUser user = new AppUser("john_doe");
        user = userRepository.save(user);

        TGAccount account1 = new TGAccount("+1234567890", "Active");
        account1.setUser(user);

        TGAccount account2 = new TGAccount("+1987654321", "Active");
        account2.setUser(user);

        user.getTgAccounts().add(account1);
        user.getTgAccounts().add(account2);

        userRepository.save(user);

        // Then the user should be successfully persisted with multiple accounts
        AppUser retrievedUser = userRepository.findById(user.getId()).orElse(null);
        assertNotNull(retrievedUser);
        assertEquals(2, retrievedUser.getTgAccounts().size());
    }

    @Test
    public void testStrictOneToOneProxyBindingConstraint() {
        // Given a proxy and two different accounts
        Proxy proxy = new Proxy("127.0.0.1", 1080, "SOCKS5");
        proxy = proxyRepository.save(proxy);

        TGAccount account1 = new TGAccount("+1000000001", "Active");
        account1.setProxy(proxy);
        tgAccountRepository.save(account1);

        TGAccount account2 = new TGAccount("+1000000002", "Active");
        account2.setProxy(proxy);

        // When assigning the same proxy to a different account, then a DataIntegrityViolationException must be thrown due to unique constraint
        Proxy finalProxy = proxy;
        assertThrows(DataIntegrityViolationException.class, () -> {
            tgAccountRepository.saveAndFlush(account2);
        });

        // Clean up
        try {
            tgAccountRepository.deleteAll();
            proxyRepository.deleteAll();
        } catch (Exception ignored) {}
    }
}

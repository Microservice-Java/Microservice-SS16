package com.example.user.service;

import com.example.user.config.CacheConfig;
import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class UserServiceCacheTest {

    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        Cache cache = cacheManager.getCache(CacheConfig.USERS_CACHE);
        if (cache != null) {
            cache.clear();
        }

        sampleUser = User.builder()
                .id("USER001")
                .name("Rika Fintech")
                .email("rika@fintech.com")
                .phone("0987654321")
                .accountBalance(50000000.0)
                .build();
    }

    @Test
    @DisplayName("1. Verify Cache Hit/Miss Behavior: 1st Call Hits DB, 2nd Call Reads Cache")
    void testCacheHitMiss_ProvesCachingIsActive() {
        when(userRepository.findById("USER001")).thenReturn(Optional.of(sampleUser));

        // 1st Call (Cache Miss): Must query Database
        User userCall1 = userService.getUserById("USER001");
        assertNotNull(userCall1);
        assertEquals("USER001", userCall1.getId());
        assertEquals("Rika Fintech", userCall1.getName());
        verify(userRepository, times(1)).findById("USER001");

        // 2nd Call (Cache Hit): Must retrieve from Spring Cache without querying Database again
        User userCall2 = userService.getUserById("USER001");
        assertNotNull(userCall2);
        assertEquals("USER001", userCall2.getId());

        // Verify UserRepository was NOT called a 2nd time! Total invocations remain 1.
        verify(userRepository, times(1)).findById("USER001");
    }

    @Test
    @DisplayName("2. Verify Unless Null Condition: Does NOT Cache Null Results")
    void testUnlessNull_DoesNotCacheNullResult() {
        when(userRepository.findById("USER_NON_EXISTENT")).thenReturn(Optional.empty());

        // 1st Call for non-existent user returns null
        User call1 = userService.getUserById("USER_NON_EXISTENT");
        assertNull(call1);
        verify(userRepository, times(1)).findById("USER_NON_EXISTENT");

        // 2nd Call for non-existent user: Because of 'unless = "#result == null"', null was not cached.
        // It should query DB again to check if user was registered in the meantime.
        User call2 = userService.getUserById("USER_NON_EXISTENT");
        assertNull(call2);
        verify(userRepository, times(2)).findById("USER_NON_EXISTENT");
    }

    @Test
    @DisplayName("3. Verify Input Validation: Fail-Fast on Null or Blank User ID")
    void testInputValidation_NullOrBlankUserId() {
        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class, () ->
                userService.getUserById(null));
        assertTrue(exNull.getMessage().contains("User ID must not be null or blank"));

        IllegalArgumentException exBlank = assertThrows(IllegalArgumentException.class, () ->
                userService.getUserById("   "));
        assertTrue(exBlank.getMessage().contains("User ID must not be null or blank"));

        verifyNoInteractions(userRepository);
    }
}

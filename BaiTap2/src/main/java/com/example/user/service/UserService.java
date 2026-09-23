package com.example.user.service;

import com.example.user.config.CacheConfig;
import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Retrieves user by ID.
     * Annotated with @Cacheable to cache user details under 'users' cache.
     * - condition: Ensures null or blank userId bypasses caching.
     * - unless: Prevents caching null results (User not found).
     */
    @Cacheable(
        value = CacheConfig.USERS_CACHE,
        key = "#userId",
        condition = "#userId != null && !#userId.trim().isEmpty()",
        unless = "#result == null"
    )
    public User getUserById(String userId) {
        validateUserId(userId);
        System.out.println(">>> Truy vấn Database cho userId: " + userId);
        log.info("[DB Query] Fetching User from Database for userId: {}", userId);

        return userRepository.findById(userId).orElse(null);
    }

    @Transactional
    public User saveUser(User user) {
        if (user == null || user.getId() == null || user.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("User and User ID must not be null or blank");
        }
        return userRepository.save(user);
    }

    @Transactional
    @CacheEvict(
        value = CacheConfig.USERS_CACHE,
        key = "#user.id",
        condition = "#user != null && #user.id != null && !#user.id.trim().isEmpty()"
    )
    public User updateUser(User user) {
        if (user == null || user.getId() == null || user.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("User and User ID must not be null or blank");
        }
        log.info("[DB Update & Cache Evict] Updating user and evicting cache for userId: {}", user.getId());
        return userRepository.save(user);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID must not be null or blank");
        }
    }
}

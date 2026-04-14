package com.codernote.platform.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
public class CacheVersionService {

    private static final String SEARCH_USER_VERSION_KEY_PREFIX = "cache:ver:search:user:";
    private static final String SEARCH_GLOBAL_VERSION_KEY = "cache:ver:search:global";
    private static final String STATISTICS_USER_VERSION_KEY_PREFIX = "cache:ver:statistics:user:";
    private static final String STATISTICS_GLOBAL_VERSION_KEY = "cache:ver:statistics:global";

    private static final String DEFAULT_VERSION = "1";
    private static final long VERSION_KEY_TTL_DAYS = 180L;

    private final StringRedisTemplate stringRedisTemplate;

    public CacheVersionService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String getSearchUserVersion(Long userId) {
        return readVersion(SEARCH_USER_VERSION_KEY_PREFIX + userId);
    }

    public String getSearchGlobalVersion() {
        return readVersion(SEARCH_GLOBAL_VERSION_KEY);
    }

    public String getStatisticsUserVersion(Long userId) {
        return readVersion(STATISTICS_USER_VERSION_KEY_PREFIX + userId);
    }

    public String getStatisticsGlobalVersion() {
        return readVersion(STATISTICS_GLOBAL_VERSION_KEY);
    }

    public void bumpSearchForUser(Long userId) {
        bump(SEARCH_USER_VERSION_KEY_PREFIX + userId);
    }

    public void bumpSearchGlobal() {
        bump(SEARCH_GLOBAL_VERSION_KEY);
    }

    public void bumpStatisticsForUser(Long userId) {
        bump(STATISTICS_USER_VERSION_KEY_PREFIX + userId);
    }

    public void bumpStatisticsGlobal() {
        bump(STATISTICS_GLOBAL_VERSION_KEY);
    }

    public void bumpSearchAndStatisticsForUser(Long userId) {
        bumpSearchForUser(userId);
        bumpStatisticsForUser(userId);
    }

    private String readVersion(String key) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            return StringUtils.hasText(value) ? value : DEFAULT_VERSION;
        } catch (Exception ignore) {
            return DEFAULT_VERSION;
        }
    }

    private void bump(String key) {
        try {
            Long version = stringRedisTemplate.opsForValue().increment(key);
            if (version != null && version > 0L) {
                stringRedisTemplate.expire(key, VERSION_KEY_TTL_DAYS, TimeUnit.DAYS);
            }
        } catch (Exception ignore) {
            // no-op
        }
    }
}

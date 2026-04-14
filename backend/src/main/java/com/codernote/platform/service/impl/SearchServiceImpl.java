package com.codernote.platform.service.impl;

import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.material.MaterialListItemVO;
import com.codernote.platform.dto.note.NoteListItemVO;
import com.codernote.platform.dto.question.QuestionListItemVO;
import com.codernote.platform.dto.search.SearchResultVO;
import com.codernote.platform.exception.BizException;
import com.codernote.platform.service.CacheVersionService;
import com.codernote.platform.service.MaterialService;
import com.codernote.platform.service.NoteService;
import com.codernote.platform.service.QuestionService;
import com.codernote.platform.service.SearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class SearchServiceImpl implements SearchService {

    private static final String SEARCH_CACHE_KEY_PREFIX = "search:global:";
    private static final String SEARCH_LOCK_KEY_SUFFIX = ":lock";
    private static final long CACHE_JITTER_DIVISOR = 5L;
    private static final long CACHE_LOCK_RETRY_SLEEP_BASE_MILLIS = 40L;
    private static final long CACHE_LOCK_RETRY_SLEEP_MAX_MILLIS = 400L;
    private static final long CACHE_LOCK_RETRY_SLEEP_JITTER_MILLIS = 20L;
    private static final RedisScript<Long> LOCK_RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long.class
    );

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final QuestionService questionService;
    private final MaterialService materialService;
    private final NoteService noteService;
    private final CacheVersionService cacheVersionService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final long searchCacheTtlSeconds;
    private final long searchCacheLockSeconds;
    private final int searchMaxPageSize;
    private final int searchMaxPageNo;
    private final int searchMaxCachedPageNo;
    private final int searchMaxKeywordLength;
    private final int searchMaxPayloadBytes;

    public SearchServiceImpl(QuestionService questionService,
                             MaterialService materialService,
                             NoteService noteService,
                             CacheVersionService cacheVersionService,
                             ObjectMapper objectMapper,
                             StringRedisTemplate stringRedisTemplate,
                             @Value("${app.cache.search-ttl-seconds:20}") long searchCacheTtlSeconds,
                             @Value("${app.cache.search-lock-seconds:3}") long searchCacheLockSeconds,
                             @Value("${app.cache.search-max-page-size:50}") int searchMaxPageSize,
                             @Value("${app.cache.search-max-page-no:1000}") int searchMaxPageNo,
                             @Value("${app.cache.search-max-cached-page-no:10}") int searchMaxCachedPageNo,
                             @Value("${app.cache.search-max-keyword-length:64}") int searchMaxKeywordLength,
                             @Value("${app.cache.search-max-payload-bytes:262144}") int searchMaxPayloadBytes) {
        this.questionService = questionService;
        this.materialService = materialService;
        this.noteService = noteService;
        this.cacheVersionService = cacheVersionService;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.searchCacheTtlSeconds = Math.max(1L, searchCacheTtlSeconds);
        this.searchCacheLockSeconds = Math.max(1L, searchCacheLockSeconds);
        this.searchMaxPageSize = Math.max(1, searchMaxPageSize);
        this.searchMaxPageNo = Math.max(1, searchMaxPageNo);
        this.searchMaxCachedPageNo = Math.max(1, searchMaxCachedPageNo);
        this.searchMaxKeywordLength = Math.max(1, searchMaxKeywordLength);
        this.searchMaxPayloadBytes = Math.max(1024, searchMaxPayloadBytes);
    }

    @Override
    public SearchResultVO search(Long userId, String keyword, Long pageNo, Long pageSize) {
        if (userId == null) {
            throw new BizException(400, "User is required");
        }
        String normalizedKeyword = normalizeKeyword(keyword);
        long normalizedPageNo = normalizePageNo(pageNo);
        long normalizedPageSize = normalizePageSize(pageSize);

        boolean cacheable = isCacheable(normalizedPageNo);
        String cacheKey = null;
        if (cacheable) {
            cacheKey = buildSearchCacheKey(userId, normalizedKeyword, normalizedPageNo, normalizedPageSize);
            SearchResultVO cached = readSearchCache(cacheKey);
            if (cached != null) {
                return cached;
            }
            String lockKey = cacheKey + SEARCH_LOCK_KEY_SUFFIX;
            CacheLockAttempt lockAttempt = tryAcquireLock(lockKey);
            if (lockAttempt.acquired()) {
                try {
                    SearchResultVO retryCached = readSearchCache(cacheKey);
                    if (retryCached != null) {
                        return retryCached;
                    }
                    SearchResultVO computed = doSearch(userId, normalizedKeyword, normalizedPageNo, normalizedPageSize);
                    writeSearchCache(cacheKey, computed);
                    return computed;
                } finally {
                    releaseLock(lockKey, lockAttempt.token());
                }
            }

            if (lockAttempt.cacheAvailable()) {
                SearchResultVO waitCached = waitAndRetryRead(cacheKey,
                        TimeUnit.SECONDS.toMillis(searchCacheLockSeconds));
                if (waitCached != null) {
                    return waitCached;
                }
            }
        }

        SearchResultVO computed = doSearch(userId, normalizedKeyword, normalizedPageNo, normalizedPageSize);
        if (cacheable) {
            writeSearchCache(cacheKey, computed);
        }
        return computed;
    }

    private SearchResultVO doSearch(Long userId, String keyword, long pageNo, long pageSize) {
        SearchResultVO vo = new SearchResultVO();
        vo.setKeyword(keyword);
        PageResult<QuestionListItemVO> questionPage = questionService.page(userId, pageNo, pageSize,
                null, null, null, keyword);
        PageResult<MaterialListItemVO> materialPage = materialService.page(userId, pageNo, pageSize,
                null, null, null, keyword);
        PageResult<NoteListItemVO> notePage = noteService.page(userId, pageNo, pageSize,
                null, null, null, keyword, null, null);
        vo.setQuestionPage(questionPage);
        vo.setMaterialPage(materialPage);
        vo.setNotePage(notePage);
        return vo;
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new BizException(400, "Keyword is required");
        }
        String normalized = keyword.trim();
        if (normalized.length() > searchMaxKeywordLength) {
            throw new BizException(400, "Keyword too long");
        }
        return normalized;
    }

    private long normalizePageNo(Long pageNo) {
        long safe = pageNo == null || pageNo < 1 ? 1L : pageNo;
        return Math.min(safe, searchMaxPageNo);
    }

    private long normalizePageSize(Long pageSize) {
        long safe = pageSize == null || pageSize < 1 ? 10L : pageSize;
        return Math.min(safe, searchMaxPageSize);
    }

    private boolean isCacheable(long pageNo) {
        return pageNo <= searchMaxCachedPageNo;
    }

    private String buildSearchCacheKey(Long userId, String keyword, long pageNo, long pageSize) {
        String keywordHash = DigestUtils.md5DigestAsHex(keyword.getBytes(StandardCharsets.UTF_8));
        String userVersion = cacheVersionService.getSearchUserVersion(userId);
        String globalVersion = cacheVersionService.getSearchGlobalVersion();
        return SEARCH_CACHE_KEY_PREFIX
                + userId + ":u" + userVersion
                + ":g" + globalVersion
                + ":" + keywordHash
                + ":" + pageNo
                + ":" + pageSize;
    }

    private SearchResultVO readSearchCache(String cacheKey) {
        try {
            String raw = stringRedisTemplate.opsForValue().get(cacheKey);
            if (!StringUtils.hasText(raw)) {
                return null;
            }
            return objectMapper.readValue(raw, SearchResultVO.class);
        } catch (Exception ex) {
            log.warn("Failed to read search cache key={}", cacheKey, ex);
            return null;
        }
    }

    private void writeSearchCache(String cacheKey, SearchResultVO value) {
        try {
            String payload = objectMapper.writeValueAsString(value);
            int payloadBytes = payload.getBytes(StandardCharsets.UTF_8).length;
            if (payloadBytes > searchMaxPayloadBytes) {
                return;
            }
            long ttl = withJitter(searchCacheTtlSeconds);
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    payload,
                    ttl,
                    TimeUnit.SECONDS
            );
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize search cache key={}", cacheKey, ex);
        } catch (Exception ex) {
            log.warn("Failed to write search cache key={}", cacheKey, ex);
        }
    }

    private CacheLockAttempt tryAcquireLock(String lockKey) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                    lockKey,
                    token,
                    searchCacheLockSeconds,
                    TimeUnit.SECONDS
            );
            if (Boolean.TRUE.equals(ok)) {
                return CacheLockAttempt.acquired(token);
            }
            return CacheLockAttempt.busy();
        } catch (Exception ex) {
            log.warn("Failed to acquire search cache lock key={}", lockKey, ex);
            return CacheLockAttempt.error();
        }
    }

    private void releaseLock(String lockKey, String token) {
        try {
            stringRedisTemplate.execute(
                    LOCK_RELEASE_SCRIPT,
                    Collections.singletonList(lockKey),
                    token
            );
        } catch (Exception ex) {
            log.warn("Failed to release search cache lock key={}", lockKey, ex);
        }
    }

    private SearchResultVO waitAndRetryRead(String cacheKey, long maxWaitMillis) {
        long safeMaxWait = Math.max(CACHE_LOCK_RETRY_SLEEP_BASE_MILLIS, maxWaitMillis);
        long waited = 0L;
        long sleepMillis = CACHE_LOCK_RETRY_SLEEP_BASE_MILLIS;
        while (waited < safeMaxWait) {
            long currentSleepMillis = Math.min(
                    CACHE_LOCK_RETRY_SLEEP_MAX_MILLIS,
                    sleepMillis + ThreadLocalRandom.current().nextLong(CACHE_LOCK_RETRY_SLEEP_JITTER_MILLIS + 1L)
            );
            try {
                Thread.sleep(currentSleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            waited += currentSleepMillis;
            SearchResultVO cached = readSearchCache(cacheKey);
            if (cached != null) {
                return cached;
            }
            sleepMillis = Math.min(CACHE_LOCK_RETRY_SLEEP_MAX_MILLIS, sleepMillis * 2L);
        }
        return null;
    }

    private long withJitter(long ttlSeconds) {
        long jitterBound = Math.max(1L, ttlSeconds / CACHE_JITTER_DIVISOR);
        return ttlSeconds + ThreadLocalRandom.current().nextLong(jitterBound + 1L);
    }

    private static final class CacheLockAttempt {
        private final String token;
        private final boolean cacheAvailable;

        private CacheLockAttempt(String token, boolean cacheAvailable) {
            this.token = token;
            this.cacheAvailable = cacheAvailable;
        }

        private static CacheLockAttempt acquired(String token) {
            return new CacheLockAttempt(token, true);
        }

        private static CacheLockAttempt busy() {
            return new CacheLockAttempt(null, true);
        }

        private static CacheLockAttempt error() {
            return new CacheLockAttempt(null, false);
        }

        private boolean acquired() {
            return token != null;
        }

        private boolean cacheAvailable() {
            return cacheAvailable;
        }

        private String token() {
            return token;
        }
    }
}

package com.codernote.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codernote.platform.dto.review.ReviewCenterOverviewVO;
import com.codernote.platform.dto.statistics.StatisticsOverviewVO;
import com.codernote.platform.entity.ErrorQuestion;
import com.codernote.platform.entity.QuestionTag;
import com.codernote.platform.entity.StudyMaterial;
import com.codernote.platform.entity.Tag;
import com.codernote.platform.exception.BizException;
import com.codernote.platform.mapper.ErrorQuestionMapper;
import com.codernote.platform.mapper.QuestionTagMapper;
import com.codernote.platform.mapper.StudyMaterialMapper;
import com.codernote.platform.mapper.TagMapper;
import com.codernote.platform.service.CacheVersionService;
import com.codernote.platform.service.ReviewService;
import com.codernote.platform.service.StatisticsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

        private static final String STATISTICS_OVERVIEW_CACHE_KEY_PREFIX = "stats:overview:";
        private static final String STATISTICS_LOCK_KEY_SUFFIX = ":lock";
        private static final long CACHE_JITTER_DIVISOR = 5L;
        private static final long CACHE_LOCK_RETRY_SLEEP_BASE_MILLIS = 40L;
        private static final long CACHE_LOCK_RETRY_SLEEP_MAX_MILLIS = 400L;
        private static final long CACHE_LOCK_RETRY_SLEEP_JITTER_MILLIS = 20L;
        private static final RedisScript<Long> LOCK_RELEASE_SCRIPT = new DefaultRedisScript<>(
                "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
                Long.class
        );

        private static final Logger log = LoggerFactory.getLogger(StatisticsServiceImpl.class);

        private final ErrorQuestionMapper questionMapper;
        private final StudyMaterialMapper materialMapper;
        private final QuestionTagMapper questionTagMapper;
        private final TagMapper tagMapper;
        private final ReviewService reviewService;
        private final CacheVersionService cacheVersionService;
        private final ObjectMapper objectMapper;
        private final StringRedisTemplate stringRedisTemplate;
        private final long overviewCacheTtlSeconds;
        private final long overviewCacheLockSeconds;
        private final int overviewMaxPayloadBytes;

        public StatisticsServiceImpl(ErrorQuestionMapper questionMapper,
                        StudyMaterialMapper materialMapper,
                        QuestionTagMapper questionTagMapper,
                        TagMapper tagMapper,
                        ReviewService reviewService,
                        CacheVersionService cacheVersionService,
                        ObjectMapper objectMapper,
                        StringRedisTemplate stringRedisTemplate,
                        @Value("${app.cache.statistics-overview-ttl-seconds:30}") long overviewCacheTtlSeconds,
                        @Value("${app.cache.statistics-overview-lock-seconds:3}") long overviewCacheLockSeconds,
                        @Value("${app.cache.statistics-overview-max-payload-bytes:131072}") int overviewMaxPayloadBytes) {
                this.questionMapper = questionMapper;
                this.materialMapper = materialMapper;
                this.questionTagMapper = questionTagMapper;
                this.tagMapper = tagMapper;
                this.reviewService = reviewService;
                this.cacheVersionService = cacheVersionService;
                this.objectMapper = objectMapper;
                this.stringRedisTemplate = stringRedisTemplate;
                this.overviewCacheTtlSeconds = Math.max(1L, overviewCacheTtlSeconds);
                this.overviewCacheLockSeconds = Math.max(1L, overviewCacheLockSeconds);
                this.overviewMaxPayloadBytes = Math.max(1024, overviewMaxPayloadBytes);
        }

        @Override
        public StatisticsOverviewVO overview(Long userId) {
                if (userId == null) {
                        throw new BizException(400, "User is required");
                }
                String cacheKey = buildOverviewCacheKey(userId);
                StatisticsOverviewVO cached = readOverviewCache(cacheKey);
                if (cached != null) {
                        return cached;
                }

                String lockKey = cacheKey + STATISTICS_LOCK_KEY_SUFFIX;
                CacheLockAttempt lockAttempt = tryAcquireLock(lockKey);
                if (lockAttempt.acquired()) {
                        try {
                                StatisticsOverviewVO retryCached = readOverviewCache(cacheKey);
                                if (retryCached != null) {
                                        return retryCached;
                                }
                                StatisticsOverviewVO computed = computeOverview(userId);
                                writeOverviewCache(cacheKey, computed);
                                return computed;
                        } finally {
                                releaseLock(lockKey, lockAttempt.token());
                        }
                }

                if (lockAttempt.cacheAvailable()) {
                        StatisticsOverviewVO waitCached = waitAndRetryRead(cacheKey,
                                TimeUnit.SECONDS.toMillis(overviewCacheLockSeconds));
                        if (waitCached != null) {
                                return waitCached;
                        }
                }

                StatisticsOverviewVO computed = computeOverview(userId);
                writeOverviewCache(cacheKey, computed);
                return computed;
        }

        private StatisticsOverviewVO computeOverview(Long userId) {
                StatisticsOverviewVO vo = new StatisticsOverviewVO();

                long questionTotal = questionMapper.selectCount(new LambdaQueryWrapper<ErrorQuestion>()
                                .eq(ErrorQuestion::getUserId, userId));
                long masteredCount = questionMapper.selectCount(new LambdaQueryWrapper<ErrorQuestion>()
                                .eq(ErrorQuestion::getUserId, userId)
                                .eq(ErrorQuestion::getMasteryStatus, "MASTERED"));
                long reviewingCount = questionMapper.selectCount(new LambdaQueryWrapper<ErrorQuestion>()
                                .eq(ErrorQuestion::getUserId, userId)
                                .eq(ErrorQuestion::getMasteryStatus, "REVIEWING"));
                long notMasteredCount = questionMapper.selectCount(new LambdaQueryWrapper<ErrorQuestion>()
                                .eq(ErrorQuestion::getUserId, userId)
                                .eq(ErrorQuestion::getMasteryStatus, "NOT_MASTERED"));
                long materialTotal = materialMapper.selectCount(new LambdaQueryWrapper<StudyMaterial>()
                                .eq(StudyMaterial::getUserId, userId));

                vo.setQuestionTotal(questionTotal);
                vo.setMasteredQuestionCount(masteredCount);
                vo.setReviewingQuestionCount(reviewingCount);
                vo.setNotMasteredQuestionCount(notMasteredCount);
                vo.setMaterialTotal(materialTotal);

                List<ErrorQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<ErrorQuestion>()
                                .eq(ErrorQuestion::getUserId, userId)
                                .select(ErrorQuestion::getId));
                if (questions.isEmpty()) {
                        vo.setTopKnowledgePoints(Collections.emptyList());
                        fillReviewStats(vo, userId);
                        return vo;
                }

                List<Long> questionIds = questions.stream().map(ErrorQuestion::getId).collect(Collectors.toList());
                List<QuestionTag> relations = questionTagMapper.selectList(new LambdaQueryWrapper<QuestionTag>()
                                .in(QuestionTag::getQuestionId, questionIds));

                if (relations.isEmpty()) {
                        vo.setTopKnowledgePoints(Collections.emptyList());
                        fillReviewStats(vo, userId);
                        return vo;
                }

                Map<Long, Long> counter = new HashMap<>();
                for (QuestionTag relation : relations) {
                        counter.put(relation.getTagId(), counter.getOrDefault(relation.getTagId(), 0L) + 1);
                }

                Map<Long, String> tagNameMap = tagMapper.selectBatchIds(counter.keySet()).stream()
                                .collect(Collectors.toMap(Tag::getId, Tag::getName));

                List<StatisticsOverviewVO.TagRankItem> topList = counter.entrySet().stream()
                                .map(entry -> {
                                        StatisticsOverviewVO.TagRankItem item = new StatisticsOverviewVO.TagRankItem();
                                        item.setTagId(entry.getKey());
                                        item.setTagName(tagNameMap.getOrDefault(entry.getKey(), "Unknown Tag"));
                                        item.setCount(entry.getValue());
                                        return item;
                                })
                                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                                .limit(10)
                                .collect(Collectors.toList());

                vo.setTopKnowledgePoints(topList);
                fillReviewStats(vo, userId);
                return vo;
        }

        private String buildOverviewCacheKey(Long userId) {
                String userVersion = cacheVersionService.getStatisticsUserVersion(userId);
                String globalVersion = cacheVersionService.getStatisticsGlobalVersion();
                return STATISTICS_OVERVIEW_CACHE_KEY_PREFIX + userId + ":u" + userVersion + ":g" + globalVersion;
        }

        private StatisticsOverviewVO readOverviewCache(String cacheKey) {
                try {
                        String raw = stringRedisTemplate.opsForValue().get(cacheKey);
                        if (!StringUtils.hasText(raw)) {
                                return null;
                        }
                        return objectMapper.readValue(raw, StatisticsOverviewVO.class);
                } catch (Exception ex) {
                        log.warn("Failed to read statistics cache key={}", cacheKey, ex);
                        return null;
                }
        }

        private void writeOverviewCache(String cacheKey, StatisticsOverviewVO value) {
                try {
                        String payload = objectMapper.writeValueAsString(value);
                        int payloadBytes = payload.getBytes(StandardCharsets.UTF_8).length;
                        if (payloadBytes > overviewMaxPayloadBytes) {
                                return;
                        }
                        long ttl = withJitter(overviewCacheTtlSeconds);
                        stringRedisTemplate.opsForValue().set(
                                cacheKey,
                                payload,
                                ttl,
                                TimeUnit.SECONDS
                        );
                } catch (JsonProcessingException ex) {
                        log.warn("Failed to serialize statistics cache key={}", cacheKey, ex);
                } catch (Exception ex) {
                        log.warn("Failed to write statistics cache key={}", cacheKey, ex);
                }
        }

        private CacheLockAttempt tryAcquireLock(String lockKey) {
                String token = UUID.randomUUID().toString();
                try {
                        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                                lockKey,
                                token,
                                overviewCacheLockSeconds,
                                TimeUnit.SECONDS
                        );
                        if (Boolean.TRUE.equals(ok)) {
                                return CacheLockAttempt.acquired(token);
                        }
                        return CacheLockAttempt.busy();
                } catch (Exception ex) {
                        log.warn("Failed to acquire statistics cache lock key={}", lockKey, ex);
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
                        log.warn("Failed to release statistics cache lock key={}", lockKey, ex);
                }
        }

        private StatisticsOverviewVO waitAndRetryRead(String cacheKey, long maxWaitMillis) {
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
                        StatisticsOverviewVO cached = readOverviewCache(cacheKey);
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

        private void fillReviewStats(StatisticsOverviewVO vo, Long userId) {
                ReviewCenterOverviewVO reviewOverview = reviewService.overview(userId);
                vo.setReviewTodayCompletedCount(reviewOverview.getTodayCompletedCount());
                vo.setReviewTodayDueTotal(reviewOverview.getTodayDueTotal());
                vo.setReviewTotalCount(reviewOverview.getTotalReviewCount());
                vo.setReviewMasteryRate(reviewOverview.getMasteryRate());

                List<StatisticsOverviewVO.ReviewWeakTagItem> weakTags = reviewOverview.getWeakTags() == null
                        ? Collections.emptyList()
                        : reviewOverview.getWeakTags().stream()
                        .map(item -> {
                                StatisticsOverviewVO.ReviewWeakTagItem weakTag = new StatisticsOverviewVO.ReviewWeakTagItem();
                                weakTag.setTagId(item.getTagId());
                                weakTag.setTagName(item.getTagName());
                                weakTag.setPendingCount(item.getPendingCount());
                                return weakTag;
                        })
                        .collect(Collectors.toList());
                vo.setReviewWeakTags(weakTags);

                List<StatisticsOverviewVO.ReviewTrendItem> trend = reviewOverview.getTrend() == null
                        ? Collections.emptyList()
                        : reviewOverview.getTrend().stream()
                        .map(item -> {
                                StatisticsOverviewVO.ReviewTrendItem trendItem = new StatisticsOverviewVO.ReviewTrendItem();
                                trendItem.setDate(item.getDate());
                                trendItem.setDueTotal(item.getDueTotal());
                                trendItem.setCompletedCount(item.getCompletedCount());
                                return trendItem;
                        })
                        .collect(Collectors.toList());
                vo.setReviewTrend(trend);
        }
}

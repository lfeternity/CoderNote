package com.codernote.platform.config;

import com.codernote.platform.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class ReviewScheduleTask {

    private static final Logger log = LoggerFactory.getLogger(ReviewScheduleTask.class);
    private static final String REVIEW_DAILY_LOCK_KEY = "schedule:review:daily:refresh:lock";
    private static final RedisScript<Long> LOCK_RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long.class
    );

    private final ReviewService reviewService;
    private final StringRedisTemplate stringRedisTemplate;
    private final long reviewDailyLockSeconds;
    private final ReentrantLock fallbackLocalLock = new ReentrantLock();

    public ReviewScheduleTask(ReviewService reviewService,
                              StringRedisTemplate stringRedisTemplate,
                              @Value("${app.schedule.review-daily-lock-seconds:3600}") long reviewDailyLockSeconds) {
        this.reviewService = reviewService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.reviewDailyLockSeconds = Math.max(1L, reviewDailyLockSeconds);
    }

    /**
     * Refresh daily review snapshots and completion baseline for all users.
     */
    @Scheduled(cron = "${app.schedule.review-daily-cron:0 0 0 * * ?}")
    public void refreshDailyReviewStats() {
        if (refreshWithRedisLock()) {
            return;
        }
        refreshWithLocalLockFallback();
    }

    private boolean refreshWithRedisLock() {
        String lockToken = UUID.randomUUID().toString();
        boolean lockAcquired = false;
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                    REVIEW_DAILY_LOCK_KEY,
                    lockToken,
                    reviewDailyLockSeconds,
                    TimeUnit.SECONDS
            );
            if (!Boolean.TRUE.equals(ok)) {
                log.debug("Skip review daily refresh because distributed lock is held by another worker");
                return true;
            }
            lockAcquired = true;
            reviewService.refreshTodayStatsForAllUsers();
            return true;
        } catch (Exception ex) {
            log.warn("Failed to execute review daily refresh with distributed lock, fallback to local lock", ex);
            return false;
        } finally {
            if (lockAcquired) {
                try {
                    stringRedisTemplate.execute(
                            LOCK_RELEASE_SCRIPT,
                            Collections.singletonList(REVIEW_DAILY_LOCK_KEY),
                            lockToken
                    );
                } catch (Exception ex) {
                    log.warn("Failed to release review daily refresh lock", ex);
                }
            }
        }
    }

    private void refreshWithLocalLockFallback() {
        if (!fallbackLocalLock.tryLock()) {
            log.debug("Skip review daily refresh because local fallback lock is held");
            return;
        }
        try {
            reviewService.refreshTodayStatsForAllUsers();
        } finally {
            fallbackLocalLock.unlock();
        }
    }
}

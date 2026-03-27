package com.codernote.platform.config;

import com.codernote.platform.service.ReviewService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReviewScheduleTask {

    private final ReviewService reviewService;

    public ReviewScheduleTask(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 每日 00:00 刷新当日待复习快照和完成统计基础数据。
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void refreshDailyReviewStats() {
        reviewService.refreshTodayStatsForAllUsers();
    }
}

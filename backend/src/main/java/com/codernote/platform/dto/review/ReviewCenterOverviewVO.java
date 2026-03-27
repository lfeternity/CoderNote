package com.codernote.platform.dto.review;

import lombok.Data;

import java.util.List;

@Data
public class ReviewCenterOverviewVO {

    private Long todayCompletedCount;
    private Long todayDueTotal;
    private Long totalReviewCount;
    private Double masteryRate;
    private List<WeakTagItem> weakTags;
    private List<TrendItem> trend;

    @Data
    public static class WeakTagItem {
        private Long tagId;
        private String tagName;
        private Long pendingCount;
    }

    @Data
    public static class TrendItem {
        private String date;
        private Long dueTotal;
        private Long completedCount;
    }
}

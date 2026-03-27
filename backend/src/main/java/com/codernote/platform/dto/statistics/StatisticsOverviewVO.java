package com.codernote.platform.dto.statistics;

import lombok.Data;

import java.util.List;

@Data
public class StatisticsOverviewVO {

    private Long questionTotal;
    private Long masteredQuestionCount;
    private Long reviewingQuestionCount;
    private Long notMasteredQuestionCount;
    private Long materialTotal;
    private List<TagRankItem> topKnowledgePoints;

    private Long reviewTodayCompletedCount;
    private Long reviewTodayDueTotal;
    private Long reviewTotalCount;
    private Double reviewMasteryRate;
    private List<ReviewWeakTagItem> reviewWeakTags;
    private List<ReviewTrendItem> reviewTrend;

    @Data
    public static class TagRankItem {
        private Long tagId;
        private String tagName;
        private Long count;
    }

    @Data
    public static class ReviewWeakTagItem {
        private Long tagId;
        private String tagName;
        private Long pendingCount;
    }

    @Data
    public static class ReviewTrendItem {
        private String date;
        private Long dueTotal;
        private Long completedCount;
    }
}

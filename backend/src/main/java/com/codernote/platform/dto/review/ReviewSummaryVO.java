package com.codernote.platform.dto.review;

import lombok.Data;

@Data
public class ReviewSummaryVO {

    private Long todayQuestionCount;
    private Long todayNoteCount;
    private Long todayTotalCount;
    private Long overdueCount;
    private Long upcomingCount;
    private Long completedCount;
}

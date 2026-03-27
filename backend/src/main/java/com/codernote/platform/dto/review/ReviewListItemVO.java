package com.codernote.platform.dto.review;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewListItemVO {

    private Long planId;
    private String contentType;
    private Long contentId;
    private String title;
    private String language;
    private List<String> tagNames;
    private String masteryStatus;
    private LocalDateTime nextReviewAt;
    private LocalDateTime lastReviewAt;
    private Integer reviewCount;
    private Boolean overdue;
}

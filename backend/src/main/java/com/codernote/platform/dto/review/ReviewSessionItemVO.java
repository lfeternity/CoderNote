package com.codernote.platform.dto.review;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewSessionItemVO {

    private Long planId;
    private String contentType;
    private Long contentId;
    private String title;
    private String language;
    private List<String> tagNames;
    private String masteryStatus;
    private LocalDateTime nextReviewAt;
    private Integer reviewCount;

    // question fields
    private String questionBody;
    private String answerBody;
    private String answerExplain;

    // note fields
    private String noteContent;
}

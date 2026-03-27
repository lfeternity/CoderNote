package com.codernote.platform.dto.question;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionListItemVO {
    private Long id;
    private String title;
    private String language;
    private String coverPath;
    private List<String> tagNames;
    private String masteryStatus;
    private Boolean inReviewPlan;
    private LocalDateTime createdAt;
}

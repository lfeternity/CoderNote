package com.codernote.platform.dto.note;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoteListItemVO {

    private Long id;

    private String title;

    private String language;

    private String coverPath;

    private List<String> tagNames;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long relatedQuestionCount;

    private Long relatedMaterialCount;

    private Boolean favorite;

    private String masteryStatus;

    private Boolean inReviewPlan;
}

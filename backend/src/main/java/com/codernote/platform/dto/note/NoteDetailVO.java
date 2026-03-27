package com.codernote.platform.dto.note;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoteDetailVO {

    private Long id;

    private String title;

    private String content;

    private String language;

    private String coverPath;

    private String masteryStatus;

    private List<String> tagNames;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<Long> manualQuestionIds;

    private List<Long> manualMaterialIds;

    private List<LinkedQuestionVO> relatedQuestions;

    private List<LinkedMaterialVO> relatedMaterials;

    private Boolean favorite;

    private Boolean inReviewPlan;

    @Data
    public static class LinkedQuestionVO {
        private Long id;
        private String title;
        private String language;
        private String masteryStatus;
        private List<String> tagNames;
    }

    @Data
    public static class LinkedMaterialVO {
        private Long id;
        private String title;
        private String materialType;
        private String language;
        private List<String> tagNames;
    }
}

package com.codernote.platform.dto.question;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionDetailVO {
    private Long id;
    private String title;
    private String language;
    private String coverPath;
    private List<String> tagNames;
    private String masteryStatus;
    private Boolean inReviewPlan;
    private String errorCode;
    private String errorReason;
    private String correctCode;
    private String solution;
    private String source;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LinkedMaterialVO> relatedMaterials;
    private List<LinkedNoteVO> relatedNotes;
    private List<Long> manualMaterialIds;
    private List<QuestionAttachmentVO> errorQuestionAttachments;
    private List<QuestionAttachmentVO> correctSolutionAttachments;

    @Data
    public static class LinkedMaterialVO {
        private Long id;
        private String title;
        private String materialType;
        private String language;
        private List<String> tagNames;
    }

    @Data
    public static class LinkedNoteVO {
        private Long id;
        private String title;
        private String language;
        private List<String> tagNames;
    }
}

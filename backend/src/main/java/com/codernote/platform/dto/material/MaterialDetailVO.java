package com.codernote.platform.dto.material;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MaterialDetailVO {
    private Long id;
    private String title;
    private String materialType;
    private String language;
    private String coverPath;
    private List<String> tagNames;
    private String content;
    private List<MaterialAttachmentVO> contentAttachments;
    private String source;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LinkedQuestionVO> relatedQuestions;
    private List<LinkedNoteVO> relatedNotes;
    private List<Long> manualQuestionIds;
    private Boolean favorite;

    @Data
    public static class LinkedQuestionVO {
        private Long id;
        private String title;
        private String language;
        private String masteryStatus;
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

package com.codernote.platform.dto.question;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class QuestionSaveRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Language is required")
    private String language;

    private String coverPath;

    @NotEmpty(message = "At least one tag is required")
    private List<String> tagNames;

    private String errorCode;

    @NotBlank(message = "Error reason is required")
    private String errorReason;

    private String correctCode;

    @NotBlank(message = "Solution is required")
    private String solution;

    private String source;

    private String remark;

    private List<Long> manualMaterialIds;

    private List<QuestionAttachmentVO> errorQuestionAttachments;

    private List<QuestionAttachmentVO> correctSolutionAttachments;
}

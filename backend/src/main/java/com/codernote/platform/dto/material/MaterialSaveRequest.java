package com.codernote.platform.dto.material;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class MaterialSaveRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Material type is required")
    private String materialType;

    @NotBlank(message = "Language is required")
    private String language;

    private String coverPath;

    @NotEmpty(message = "At least one tag is required")
    private List<String> tagNames;

    private String content;

    private String source;

    private String remark;

    @JsonAlias("manual_question_ids")
    private List<Long> manualQuestionIds;

    @JsonAlias("content_attachments")
    private List<MaterialAttachmentVO> contentAttachments;
}

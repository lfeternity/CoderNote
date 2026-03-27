package com.codernote.platform.dto.note;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class NoteSaveRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @NotBlank(message = "Language is required")
    private String language;

    private String coverPath;

    @NotEmpty(message = "At least one tag is required")
    private List<String> tagNames;

    private List<Long> manualQuestionIds;

    private List<Long> manualMaterialIds;

    @Size(max = 120, message = "Version summary max 120 chars")
    private String versionSummary;
}

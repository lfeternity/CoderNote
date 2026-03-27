package com.codernote.platform.dto.review;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ReviewStatusUpdateRequest {

    @NotBlank(message = "contentType is required")
    private String contentType;

    @NotNull(message = "contentId is required")
    private Long contentId;

    @NotBlank(message = "masteryStatus is required")
    private String masteryStatus;
}

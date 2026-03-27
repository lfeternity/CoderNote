package com.codernote.platform.dto.review;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ReviewExecuteRequest {

    @NotNull(message = "planId is required")
    private Long planId;

    @NotBlank(message = "action is required")
    private String action;
}

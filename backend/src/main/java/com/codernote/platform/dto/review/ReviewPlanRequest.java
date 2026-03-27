package com.codernote.platform.dto.review;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ReviewPlanRequest {

    @NotBlank(message = "contentType is required")
    private String contentType;

    @NotNull(message = "contentId is required")
    private Long contentId;

    private String planMode = "AUTO";

    /**
     * manual dates, format: yyyy-MM-dd
     */
    private List<String> manualReviewDates;
}

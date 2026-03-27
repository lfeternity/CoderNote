package com.codernote.platform.dto.review;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class ReviewBatchPlanRequest {

    @NotBlank(message = "contentType is required")
    private String contentType;

    @NotEmpty(message = "contentIds is required")
    private List<Long> contentIds;

    private String planMode = "AUTO";

    /**
     * manual dates, format: yyyy-MM-dd
     */
    private List<String> manualReviewDates;
}

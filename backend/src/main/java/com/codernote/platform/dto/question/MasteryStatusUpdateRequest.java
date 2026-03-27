package com.codernote.platform.dto.question;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class MasteryStatusUpdateRequest {

    @NotBlank(message = "Mastery status is required")
    private String masteryStatus;
}

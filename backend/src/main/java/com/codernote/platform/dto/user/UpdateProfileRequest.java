package com.codernote.platform.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UpdateProfileRequest {

    private String studentNo;

    @NotBlank(message = "Major is required")
    private String major;

    private String remark;
}

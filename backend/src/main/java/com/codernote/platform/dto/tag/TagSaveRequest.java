package com.codernote.platform.dto.tag;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TagSaveRequest {

    @NotBlank(message = "Tag name is required")
    private String name;
}

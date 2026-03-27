package com.codernote.platform.dto.question;

import lombok.Data;

@Data
public class QuestionAttachmentVO {

    private String fileName;

    private String path;

    private String contentType;

    private Long size;

    private Boolean image;

    private String previewUrl;

    private String downloadUrl;
}
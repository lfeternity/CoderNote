package com.codernote.platform.dto.material;

import lombok.Data;

@Data
public class MaterialAttachmentVO {

    private String fileName;

    private String path;

    private String contentType;

    private Long size;

    private Boolean image;

    private String previewUrl;

    private String downloadUrl;
}
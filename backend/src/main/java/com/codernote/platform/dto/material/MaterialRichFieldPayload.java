package com.codernote.platform.dto.material;

import lombok.Data;

import java.util.List;

@Data
public class MaterialRichFieldPayload {

    private String text;

    private List<MaterialAttachmentVO> attachments;
}
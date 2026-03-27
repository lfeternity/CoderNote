package com.codernote.platform.dto.question;

import lombok.Data;

import java.util.List;

@Data
public class QuestionRichFieldPayload {

    private String text;

    private List<QuestionAttachmentVO> attachments;
}
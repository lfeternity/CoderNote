package com.codernote.platform.dto.user;

import lombok.Data;

@Data
public class CaptchaVO {

    private String imageBase64;

    private Integer expireSeconds;
}
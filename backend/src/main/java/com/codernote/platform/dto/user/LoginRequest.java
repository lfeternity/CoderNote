package com.codernote.platform.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginRequest {

    @NotBlank(message = "Nickname is required")
    private String nickname;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Captcha is required")
    private String captchaCode;
}
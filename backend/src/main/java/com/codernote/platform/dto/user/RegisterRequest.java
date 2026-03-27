package com.codernote.platform.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {

    @NotBlank(message = "Nickname is required")
    @Size(min = 1, max = 10, message = "Nickname length must be 1-10")
    private String nickname;

    private String studentNo;

    @NotBlank(message = "Major is required")
    private String major;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,16}$", message = "Password must be 6-16 with letters and numbers")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "Captcha is required")
    private String captchaCode;

    private String remark;
}
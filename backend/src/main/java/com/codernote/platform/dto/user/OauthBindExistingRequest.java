package com.codernote.platform.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class OauthBindExistingRequest {

    @NotBlank(message = "Bind token is required")
    private String bindToken;

    @NotBlank(message = "Nickname is required")
    private String nickname;

    @NotBlank(message = "Password is required")
    private String password;
}

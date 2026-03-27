package com.codernote.platform.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class OauthAutoRegisterRequest {

    @NotBlank(message = "Bind token is required")
    private String bindToken;

    @Size(max = 10, message = "Nickname length must be 1-10")
    private String nickname;
}

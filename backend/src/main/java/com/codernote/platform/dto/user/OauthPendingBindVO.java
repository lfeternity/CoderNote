package com.codernote.platform.dto.user;

import lombok.Data;

@Data
public class OauthPendingBindVO {
    private String platform;
    private String platformName;
    private String nickname;
    private String avatarUrl;
    private String redirectPath;
}

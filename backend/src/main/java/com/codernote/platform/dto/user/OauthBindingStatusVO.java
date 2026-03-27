package com.codernote.platform.dto.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OauthBindingStatusVO {
    private String platform;
    private String platformName;
    private String brandColor;
    private Boolean bound;
    private String platformNickname;
    private LocalDateTime bindTime;
}

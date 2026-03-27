package com.codernote.platform.dto.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileVO {
    private Long id;
    private String nickname;
    private String studentNo;
    private String major;
    private String remark;
    private LocalDateTime registerTime;
    private String avatarUrl;
}
package com.codernote.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_oauth_bind")
public class UserOauthBind {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String platform;

    @TableField("open_id")
    private String openId;

    @TableField("union_id")
    private String unionId;

    @TableField("platform_nickname")
    private String platformNickname;

    @TableField("platform_avatar_url")
    private String platformAvatarUrl;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

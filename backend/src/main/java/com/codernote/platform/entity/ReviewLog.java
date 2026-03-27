package com.codernote.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_log")
public class ReviewLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("plan_id")
    private Long planId;

    @TableField("content_type")
    private String contentType;

    @TableField("content_id")
    private Long contentId;

    private String action;

    @TableField("mastery_status_before")
    private String masteryStatusBefore;

    @TableField("mastery_status_after")
    private String masteryStatusAfter;

    @TableField("next_review_before")
    private LocalDateTime nextReviewBefore;

    @TableField("next_review_after")
    private LocalDateTime nextReviewAfter;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

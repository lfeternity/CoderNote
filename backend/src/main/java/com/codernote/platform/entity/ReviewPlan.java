package com.codernote.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_plan")
public class ReviewPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("content_type")
    private String contentType;

    @TableField("content_id")
    private Long contentId;

    @TableField("mastery_status")
    private String masteryStatus;

    @TableField("plan_mode")
    private String planMode;

    /**
     * comma separated datetime points, format: yyyy-MM-dd HH:mm:ss
     */
    @TableField("review_points")
    private String reviewPoints;

    @TableField("next_point_index")
    private Integer nextPointIndex;

    @TableField("review_count")
    private Integer reviewCount;

    @TableField("next_review_at")
    private LocalDateTime nextReviewAt;

    @TableField("last_review_at")
    private LocalDateTime lastReviewAt;

    @TableField("mastered_at")
    private LocalDateTime masteredAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

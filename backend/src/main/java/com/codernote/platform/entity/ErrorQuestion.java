package com.codernote.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("error_question")
public class ErrorQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String title;

    private String language;

    @TableField("cover_path")
    private String coverPath;

    @TableField("mastery_status")
    private String masteryStatus;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_reason")
    private String errorReason;

    @TableField("correct_code")
    private String correctCode;

    private String solution;

    private String source;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

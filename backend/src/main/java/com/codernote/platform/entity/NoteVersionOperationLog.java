package com.codernote.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("note_version_operation_log")
public class NoteVersionOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("note_id")
    private Long noteId;

    @TableField("user_id")
    private Long userId;

    private String action;

    @TableField("source_version_no")
    private Integer sourceVersionNo;

    @TableField("target_version_no")
    private Integer targetVersionNo;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

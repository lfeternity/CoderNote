package com.codernote.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("review_daily_stat")
public class ReviewDailyStat {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("stat_date")
    private LocalDate statDate;

    @TableField("due_total")
    private Integer dueTotal;

    @TableField("due_question_count")
    private Integer dueQuestionCount;

    @TableField("due_note_count")
    private Integer dueNoteCount;

    @TableField("completed_count")
    private Integer completedCount;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

package com.codernote.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("note_version")
public class NoteVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("note_id")
    private Long noteId;

    @TableField("user_id")
    private Long userId;

    @TableField("version_no")
    private Integer versionNo;

    private String title;

    private String content;

    private String language;

    @TableField("tag_names_json")
    private String tagNamesJson;

    private String summary;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

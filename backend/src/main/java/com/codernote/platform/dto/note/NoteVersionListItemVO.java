package com.codernote.platform.dto.note;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteVersionListItemVO {

    private Long id;

    private Integer versionNo;

    private String versionLabel;

    private String summary;

    private String editorName;

    private LocalDateTime createdAt;
}

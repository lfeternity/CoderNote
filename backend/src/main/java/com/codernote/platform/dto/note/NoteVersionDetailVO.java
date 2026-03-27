package com.codernote.platform.dto.note;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoteVersionDetailVO {

    private Long id;

    private Integer versionNo;

    private String versionLabel;

    private String title;

    private String content;

    private String language;

    private List<String> tagNames;

    private String summary;

    private String editorName;

    private LocalDateTime createdAt;
}

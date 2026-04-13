package com.codernote.platform.dto.tag;

import lombok.Data;

@Data
public class TagListItemVO {
    private Long id;
    private String name;
    private String coverPath;
    private Long usageCount;
    private Boolean canEdit;
    private Boolean canDelete;
}

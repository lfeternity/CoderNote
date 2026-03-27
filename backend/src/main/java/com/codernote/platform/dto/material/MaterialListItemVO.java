package com.codernote.platform.dto.material;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MaterialListItemVO {
    private Long id;
    private String title;
    private String materialType;
    private String language;
    private String coverPath;
    private List<String> tagNames;
    private LocalDateTime createdAt;
    private Boolean favorite;
}

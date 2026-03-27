package com.codernote.platform.dto.ai;

import lombok.Data;

@Data
public class AiSummaryVO {

    private String model;

    private Integer usedCount;

    private Integer remainingCount;

    private String summaryType;

    private String markdown;

    private String summaryText;

    private String mindMapImageData;

    private String mindMapFileName;
}

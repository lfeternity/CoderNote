package com.codernote.platform.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiModelCatalogVO {

    private String defaultModel;

    private List<ModelOption> models;

    @Data
    public static class ModelOption {
        private String value;
        private String label;
        private String description;
    }
}

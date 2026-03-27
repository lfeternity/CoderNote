package com.codernote.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.note-version")
public class NoteVersionProperties {

    private Integer maxVersionsPerNote = 50;

    private Integer contentSizeLimitBytes = 5 * 1024 * 1024;
}

package com.codernote.platform.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class TagSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public TagSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        Integer tableExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.TABLES " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag'",
                Integer.class
        );
        if (tableExists == null || tableExists <= 0) {
            return;
        }

        Integer columnExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag' AND COLUMN_NAME = 'cover_path'",
                Integer.class
        );
        if (columnExists == null || columnExists <= 0) {
            jdbcTemplate.execute("ALTER TABLE `tag` ADD COLUMN `cover_path` VARCHAR(500) DEFAULT NULL AFTER `name`");
        }
    }
}

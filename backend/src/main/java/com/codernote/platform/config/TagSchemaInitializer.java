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

        Integer oldUniqueExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag' AND INDEX_NAME = 'uk_tag_name'",
                Integer.class
        );
        if (oldUniqueExists != null && oldUniqueExists > 0) {
            jdbcTemplate.execute("ALTER TABLE `tag` DROP INDEX `uk_tag_name`");
        }

        Integer newUniqueExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag' AND INDEX_NAME = 'uk_tag_creator_name'",
                Integer.class
        );
        if (newUniqueExists == null || newUniqueExists <= 0) {
            jdbcTemplate.execute("ALTER TABLE `tag` ADD UNIQUE KEY `uk_tag_creator_name` (`creator_user_id`, `name`)");
        }
    }
}

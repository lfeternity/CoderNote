package com.codernote.platform.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class OauthSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public OauthSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `user_oauth_bind` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                "`user_id` BIGINT NOT NULL," +
                "`platform` VARCHAR(20) NOT NULL," +
                "`open_id` VARCHAR(128) NOT NULL," +
                "`union_id` VARCHAR(128) DEFAULT NULL," +
                "`platform_nickname` VARCHAR(100) DEFAULT NULL," +
                "`platform_avatar_url` VARCHAR(500) DEFAULT NULL," +
                "`created_at` DATETIME NOT NULL," +
                "`updated_at` DATETIME NOT NULL," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `uk_oauth_platform_openid` (`platform`,`open_id`)," +
                "UNIQUE KEY `uk_oauth_user_platform` (`user_id`,`platform`)," +
                "KEY `idx_oauth_user_id` (`user_id`)," +
                "CONSTRAINT `fk_oauth_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
    }
}

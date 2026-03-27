package com.codernote.platform.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ReviewSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public ReviewSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `review_plan` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                "`user_id` BIGINT NOT NULL," +
                "`content_type` VARCHAR(20) NOT NULL," +
                "`content_id` BIGINT NOT NULL," +
                "`mastery_status` VARCHAR(30) NOT NULL," +
                "`plan_mode` VARCHAR(20) NOT NULL," +
                "`review_points` LONGTEXT NOT NULL," +
                "`next_point_index` INT NOT NULL DEFAULT 0," +
                "`review_count` INT NOT NULL DEFAULT 0," +
                "`next_review_at` DATETIME DEFAULT NULL," +
                "`last_review_at` DATETIME DEFAULT NULL," +
                "`mastered_at` DATETIME DEFAULT NULL," +
                "`created_at` DATETIME NOT NULL," +
                "`updated_at` DATETIME NOT NULL," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `uk_review_plan_user_content` (`user_id`,`content_type`,`content_id`)," +
                "KEY `idx_review_plan_due` (`user_id`,`mastery_status`,`next_review_at`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `review_log` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                "`user_id` BIGINT NOT NULL," +
                "`plan_id` BIGINT NOT NULL," +
                "`content_type` VARCHAR(20) NOT NULL," +
                "`content_id` BIGINT NOT NULL," +
                "`action` VARCHAR(20) NOT NULL," +
                "`mastery_status_before` VARCHAR(30) NOT NULL," +
                "`mastery_status_after` VARCHAR(30) NOT NULL," +
                "`next_review_before` DATETIME DEFAULT NULL," +
                "`next_review_after` DATETIME DEFAULT NULL," +
                "`reviewed_at` DATETIME NOT NULL," +
                "`created_at` DATETIME NOT NULL," +
                "PRIMARY KEY (`id`)," +
                "KEY `idx_review_log_user_reviewed` (`user_id`,`reviewed_at`)," +
                "KEY `idx_review_log_plan` (`plan_id`)," +
                "CONSTRAINT `fk_review_log_plan` FOREIGN KEY (`plan_id`) REFERENCES `review_plan` (`id`) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `review_daily_stat` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                "`user_id` BIGINT NOT NULL," +
                "`stat_date` DATE NOT NULL," +
                "`due_total` INT NOT NULL DEFAULT 0," +
                "`due_question_count` INT NOT NULL DEFAULT 0," +
                "`due_note_count` INT NOT NULL DEFAULT 0," +
                "`completed_count` INT NOT NULL DEFAULT 0," +
                "`created_at` DATETIME NOT NULL," +
                "`updated_at` DATETIME NOT NULL," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `uk_review_daily_user_date` (`user_id`,`stat_date`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
    }
}

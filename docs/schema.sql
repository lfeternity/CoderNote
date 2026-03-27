CREATE DATABASE IF NOT EXISTS `coder_note`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `coder_note`;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `nickname` VARCHAR(20) NOT NULL,
  `student_no` VARCHAR(50) DEFAULT NULL,
  `major` VARCHAR(100) NOT NULL,
  `password` VARCHAR(64) NOT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_nickname` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `user_oauth_bind` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `platform` VARCHAR(20) NOT NULL,
  `open_id` VARCHAR(128) NOT NULL,
  `union_id` VARCHAR(128) DEFAULT NULL,
  `platform_nickname` VARCHAR(100) DEFAULT NULL,
  `platform_avatar_url` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_oauth_platform_openid` (`platform`, `open_id`),
  UNIQUE KEY `uk_oauth_user_platform` (`user_id`, `platform`),
  KEY `idx_oauth_user_id` (`user_id`),
  CONSTRAINT `fk_oauth_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `creator_user_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`name`),
  KEY `idx_tag_creator_user_id` (`creator_user_id`),
  CONSTRAINT `fk_tag_creator_user` FOREIGN KEY (`creator_user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `error_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `language` VARCHAR(50) NOT NULL,
  `cover_path` VARCHAR(500) DEFAULT NULL,
  `mastery_status` VARCHAR(30) NOT NULL,
  `error_code` TEXT NOT NULL,
  `error_reason` TEXT NOT NULL,
  `correct_code` TEXT NOT NULL,
  `solution` TEXT NOT NULL,
  `source` VARCHAR(100) DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_question_user_id` (`user_id`),
  KEY `idx_question_language` (`language`),
  KEY `idx_question_mastery_status` (`mastery_status`),
  CONSTRAINT `fk_question_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `study_material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `material_type` VARCHAR(30) NOT NULL,
  `language` VARCHAR(50) NOT NULL,
  `cover_path` VARCHAR(500) DEFAULT NULL,
  `content` TEXT NOT NULL,
  `source` VARCHAR(100) DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_material_user_id` (`user_id`),
  KEY `idx_material_type` (`material_type`),
  KEY `idx_material_language` (`language`),
  CONSTRAINT `fk_material_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `question_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT NOT NULL,
  `tag_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_tag` (`question_id`, `tag_id`),
  KEY `idx_question_tag_tag_id` (`tag_id`),
  CONSTRAINT `fk_question_tag_question` FOREIGN KEY (`question_id`) REFERENCES `error_question` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_question_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `material_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `material_id` BIGINT NOT NULL,
  `tag_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_material_tag` (`material_id`, `tag_id`),
  KEY `idx_material_tag_tag_id` (`tag_id`),
  CONSTRAINT `fk_material_tag_material` FOREIGN KEY (`material_id`) REFERENCES `study_material` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_material_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `question_material_link` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `question_id` BIGINT NOT NULL,
  `material_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_material_link` (`user_id`, `question_id`, `material_id`),
  KEY `idx_link_question_id` (`question_id`),
  KEY `idx_link_material_id` (`material_id`),
  CONSTRAINT `fk_link_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_link_question` FOREIGN KEY (`question_id`) REFERENCES `error_question` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_link_material` FOREIGN KEY (`material_id`) REFERENCES `study_material` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `material_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `material_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_material_favorite` (`user_id`, `material_id`),
  KEY `idx_material_favorite_material_id` (`material_id`),
  CONSTRAINT `fk_material_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_material_favorite_material` FOREIGN KEY (`material_id`) REFERENCES `study_material` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE IF NOT EXISTS `study_note` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `content` LONGTEXT NOT NULL,
  `language` VARCHAR(50) NOT NULL,
  `cover_path` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_note_user_id` (`user_id`),
  KEY `idx_note_language` (`language`),
  CONSTRAINT `fk_note_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `note_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `note_id` BIGINT NOT NULL,
  `tag_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_tag` (`note_id`, `tag_id`),
  KEY `idx_note_tag_tag_id` (`tag_id`),
  CONSTRAINT `fk_note_tag_note` FOREIGN KEY (`note_id`) REFERENCES `study_note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_note_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `note_question_link` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `note_id` BIGINT NOT NULL,
  `question_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_question_link` (`user_id`, `note_id`, `question_id`),
  KEY `idx_nq_link_note_id` (`note_id`),
  KEY `idx_nq_link_question_id` (`question_id`),
  CONSTRAINT `fk_nq_link_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_nq_link_note` FOREIGN KEY (`note_id`) REFERENCES `study_note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_nq_link_question` FOREIGN KEY (`question_id`) REFERENCES `error_question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `note_material_link` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `note_id` BIGINT NOT NULL,
  `material_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_material_link` (`user_id`, `note_id`, `material_id`),
  KEY `idx_nm_link_note_id` (`note_id`),
  KEY `idx_nm_link_material_id` (`material_id`),
  CONSTRAINT `fk_nm_link_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_nm_link_note` FOREIGN KEY (`note_id`) REFERENCES `study_note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_nm_link_material` FOREIGN KEY (`material_id`) REFERENCES `study_material` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `note_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `note_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_favorite` (`user_id`, `note_id`),
  KEY `idx_note_favorite_note_id` (`note_id`),
  CONSTRAINT `fk_note_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_note_favorite_note` FOREIGN KEY (`note_id`) REFERENCES `study_note` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `note_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `note_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `version_no` INT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `content` LONGTEXT NOT NULL,
  `language` VARCHAR(50) NOT NULL,
  `tag_names_json` LONGTEXT NOT NULL,
  `summary` VARCHAR(200) NOT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_version_note_no` (`note_id`, `version_no`),
  KEY `idx_note_version_user_note` (`user_id`, `note_id`),
  KEY `idx_note_version_created_at` (`created_at`),
  CONSTRAINT `fk_note_version_note` FOREIGN KEY (`note_id`) REFERENCES `study_note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_note_version_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `note_version_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `note_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `action` VARCHAR(30) NOT NULL,
  `source_version_no` INT DEFAULT NULL,
  `target_version_no` INT DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_note_version_log_note` (`note_id`),
  KEY `idx_note_version_log_user_created` (`user_id`, `created_at`),
  CONSTRAINT `fk_note_version_log_note` FOREIGN KEY (`note_id`) REFERENCES `study_note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_note_version_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `review_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `content_type` VARCHAR(20) NOT NULL,
  `content_id` BIGINT NOT NULL,
  `mastery_status` VARCHAR(30) NOT NULL,
  `plan_mode` VARCHAR(20) NOT NULL,
  `review_points` LONGTEXT NOT NULL,
  `next_point_index` INT NOT NULL DEFAULT 0,
  `review_count` INT NOT NULL DEFAULT 0,
  `next_review_at` DATETIME DEFAULT NULL,
  `last_review_at` DATETIME DEFAULT NULL,
  `mastered_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_plan_user_content` (`user_id`, `content_type`, `content_id`),
  KEY `idx_review_plan_due` (`user_id`, `mastery_status`, `next_review_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `review_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `plan_id` BIGINT NOT NULL,
  `content_type` VARCHAR(20) NOT NULL,
  `content_id` BIGINT NOT NULL,
  `action` VARCHAR(20) NOT NULL,
  `mastery_status_before` VARCHAR(30) NOT NULL,
  `mastery_status_after` VARCHAR(30) NOT NULL,
  `next_review_before` DATETIME DEFAULT NULL,
  `next_review_after` DATETIME DEFAULT NULL,
  `reviewed_at` DATETIME NOT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_review_log_user_reviewed` (`user_id`, `reviewed_at`),
  KEY `idx_review_log_plan` (`plan_id`),
  CONSTRAINT `fk_review_log_plan` FOREIGN KEY (`plan_id`) REFERENCES `review_plan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `review_daily_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `stat_date` DATE NOT NULL,
  `due_total` INT NOT NULL DEFAULT 0,
  `due_question_count` INT NOT NULL DEFAULT 0,
  `due_note_count` INT NOT NULL DEFAULT 0,
  `completed_count` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_daily_user_date` (`user_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @db_name = DATABASE();

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'error_question'
        AND COLUMN_NAME = 'cover_path'
    ),
    'SELECT 1',
    'ALTER TABLE `error_question` ADD COLUMN `cover_path` VARCHAR(500) DEFAULT NULL AFTER `language`'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'study_material'
        AND COLUMN_NAME = 'cover_path'
    ),
    'SELECT 1',
    'ALTER TABLE `study_material` ADD COLUMN `cover_path` VARCHAR(500) DEFAULT NULL AFTER `language`'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'study_note'
        AND COLUMN_NAME = 'cover_path'
    ),
    'SELECT 1',
    'ALTER TABLE `study_note` ADD COLUMN `cover_path` VARCHAR(500) DEFAULT NULL AFTER `language`'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

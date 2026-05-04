DROP DATABASE IF EXISTS `logic_projector`;

CREATE DATABASE IF NOT EXISTS `logic_projector` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

USE `logic_projector`;

CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `credits_balance` INT NOT NULL,
    `frozen_credits_balance` INT NOT NULL,
    `status` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_email` (`email`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `generation_tasks` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `source_code` LONGTEXT NOT NULL,
    `language` VARCHAR(255) NOT NULL,
    `detected_algorithm` VARCHAR(255) DEFAULT NULL,
    `confidence_score` DOUBLE DEFAULT NULL,
    `visualization_payload_json` LONGTEXT DEFAULT NULL,
    `summary` VARCHAR(255) DEFAULT NULL,
    `error_message` VARCHAR(255) DEFAULT NULL,
    `retry_count` INT NOT NULL,
    `last_processed_at` DATETIME(6) DEFAULT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_generation_tasks_user_id` (`user_id`),
    KEY `idx_generation_tasks_status` (`status`),
    KEY `idx_generation_tasks_updated_at` (`updated_at`),
    CONSTRAINT `fk_generation_tasks_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `export_tasks` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `generation_task_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `progress` INT NOT NULL,
    `credits_frozen` INT NOT NULL,
    `credits_charged` INT DEFAULT NULL,
    `video_path` VARCHAR(255) DEFAULT NULL,
    `subtitle_path` VARCHAR(255) DEFAULT NULL,
    `audio_path` VARCHAR(255) DEFAULT NULL,
    `error_message` VARCHAR(255) DEFAULT NULL,
    `warning_message` VARCHAR(255) DEFAULT NULL,
    `retry_count` INT NOT NULL,
    `last_processed_at` DATETIME(6) DEFAULT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_export_tasks_generation_task_id` (`generation_task_id`),
    KEY `idx_export_tasks_user_id` (`user_id`),
    KEY `idx_export_tasks_status` (`status`),
    KEY `idx_export_tasks_updated_at` (`updated_at`),
    CONSTRAINT `fk_export_tasks_generation_task_id` FOREIGN KEY (`generation_task_id`) REFERENCES `generation_tasks` (`id`),
    CONSTRAINT `fk_export_tasks_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `recharge_orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `package_code` VARCHAR(64) NOT NULL,
    `package_name` VARCHAR(255) NOT NULL,
    `credits` INT NOT NULL,
    `amount_cents` INT NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `paid_at` DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_recharge_orders_user_id` (`user_id`),
    KEY `idx_recharge_orders_status` (`status`),
    KEY `idx_recharge_orders_created_at` (`created_at`),
    CONSTRAINT `fk_recharge_orders_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `billing_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `task_id` BIGINT DEFAULT NULL,
    `change_type` VARCHAR(255) NOT NULL,
    `credits_delta` INT NOT NULL,
    `balance_after` INT NOT NULL,
    `description` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_billing_records_user_id` (`user_id`),
    KEY `idx_billing_records_task_id` (`task_id`),
    CONSTRAINT `fk_billing_records_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_billing_records_task_id` FOREIGN KEY (`task_id`) REFERENCES `generation_tasks` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `system_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT DEFAULT NULL,
    `user_id` BIGINT DEFAULT NULL,
    `log_level` VARCHAR(255) NOT NULL,
    `module` VARCHAR(255) NOT NULL,
    `message` VARCHAR(255) NOT NULL,
    `details` LONGTEXT DEFAULT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_system_logs_task_id` (`task_id`),
    KEY `idx_system_logs_user_id` (`user_id`),
    KEY `idx_system_logs_created_at` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
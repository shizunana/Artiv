USE `artiv`;

CREATE TABLE IF NOT EXISTS `user_block` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `blocker_id` BIGINT NOT NULL,
  `blocked_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_block` (`blocker_id`, `blocked_id`),
  KEY `idx_user_block_blocker_created` (`blocker_id`, `created_at`),
  KEY `idx_user_block_blocked` (`blocked_id`)
) ENGINE=InnoDB COMMENT='用户黑名单';

CREATE TABLE IF NOT EXISTS `report_case` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `target_type` VARCHAR(20) NOT NULL DEFAULT 'USER',
  `target_id` BIGINT NOT NULL,
  `target_snapshot` TEXT DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `admin_id` BIGINT DEFAULT NULL,
  `decision_reason` VARCHAR(500) DEFAULT NULL,
  `handled_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_case_target` (`target_type`, `target_id`),
  KEY `idx_report_case_status_updated` (`status`, `updated_at`)
) ENGINE=InnoDB COMMENT='用户举报聚合案件';

CREATE TABLE IF NOT EXISTS `report_submission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `case_id` BIGINT NOT NULL,
  `reporter_id` BIGINT NOT NULL,
  `category` VARCHAR(30) NOT NULL,
  `reason` VARCHAR(500) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_submission_reporter` (`case_id`, `reporter_id`),
  KEY `idx_report_submission_case_created` (`case_id`, `created_at`),
  KEY `idx_report_submission_reporter` (`reporter_id`, `created_at`)
) ENGINE=InnoDB COMMENT='举报人提交';

CREATE TABLE IF NOT EXISTS `report_evidence` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `submission_id` BIGINT NOT NULL,
  `image_url` VARCHAR(255) NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_evidence_submission` (`submission_id`, `sort_order`, `id`)
) ENGINE=InnoDB COMMENT='举报图片证据';

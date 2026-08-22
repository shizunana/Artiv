-- F10 双方评价、评价图片与画师申诉增量迁移（MySQL 8）
-- 只新增表和索引，不删除或重置既有评价数据。

USE `artiv`;

CREATE TABLE IF NOT EXISTS `review_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `review_id` BIGINT NOT NULL COMMENT '评价ID',
  `image_url` VARCHAR(255) NOT NULL COMMENT '本站上传图片地址',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_review_image_review` (`review_id`, `sort_order`, `id`)
) ENGINE=InnoDB COMMENT='评价图片';

CREATE TABLE IF NOT EXISTS `review_appeal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `review_id` BIGINT NOT NULL COMMENT '被申诉评价ID',
  `applicant_id` BIGINT NOT NULL COMMENT '申诉画师ID',
  `reason` VARCHAR(500) NOT NULL COMMENT '申诉原因',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理、1批准删除、2驳回',
  `admin_id` BIGINT DEFAULT NULL COMMENT '处理管理员ID',
  `decision_reason` VARCHAR(500) DEFAULT NULL COMMENT '处理说明',
  `decided_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_appeal_review` (`review_id`),
  KEY `idx_review_appeal_status_created` (`status`, `created_at`),
  KEY `idx_review_appeal_applicant` (`applicant_id`, `created_at`)
) ENGINE=InnoDB COMMENT='评价删除申诉';

DROP PROCEDURE IF EXISTS `artiv_review_add_index_if_missing`;
DELIMITER $$
CREATE PROCEDURE `artiv_review_add_index_if_missing`(
  IN table_name_value VARCHAR(64), IN index_name_value VARCHAR(64), IN definition_value TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = table_name_value AND INDEX_NAME = index_name_value
  ) THEN
    SET @review_index_ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD INDEX `', index_name_value, '` ', definition_value);
    PREPARE review_index_statement FROM @review_index_ddl;
    EXECUTE review_index_statement;
    DEALLOCATE PREPARE review_index_statement;
  END IF;
END$$
DELIMITER ;

CALL `artiv_review_add_index_if_missing`('review', 'idx_review_recipient_created', '(`to_id`, `deleted`, `created_at`)');
CALL `artiv_review_add_index_if_missing`('review', 'idx_review_order_deleted', '(`order_id`, `deleted`)');

DROP PROCEDURE IF EXISTS `artiv_review_add_index_if_missing`;

USE `huiyue`;

ALTER TABLE `order_info`
  ADD COLUMN `stall_title` VARCHAR(100) DEFAULT NULL COMMENT '橱窗标题快照' AFTER `amount`,
  ADD COLUMN `stall_cover` VARCHAR(255) DEFAULT NULL COMMENT '橱窗封面快照' AFTER `stall_title`,
  ADD COLUMN `phase_snapshot` TEXT DEFAULT NULL COMMENT '创作阶段 JSON 快照' AFTER `stall_cover`,
  ADD COLUMN `cancel_reason` VARCHAR(500) DEFAULT NULL COMMENT '取消/拒单原因' AFTER `current_stage`,
  ADD COLUMN `after_sale_reason` VARCHAR(500) DEFAULT NULL COMMENT '售后申请原因' AFTER `cancel_reason`,
  ADD COLUMN `accepted_at` DATETIME DEFAULT NULL COMMENT '接单时间' AFTER `after_sale_reason`,
  ADD COLUMN `completed_at` DATETIME DEFAULT NULL COMMENT '完成结算时间' AFTER `accepted_at`,
  ADD COLUMN `cancelled_at` DATETIME DEFAULT NULL COMMENT '取消时间' AFTER `completed_at`;

ALTER TABLE `order_stage`
  ADD COLUMN `stage_index` TINYINT NOT NULL DEFAULT 0 COMMENT '阶段序号' AFTER `name`,
  ADD COLUMN `submission_no` INT NOT NULL DEFAULT 1 COMMENT '该阶段提交版本' AFTER `stage_index`,
  ADD COLUMN `originals` TEXT DEFAULT NULL COMMENT '高清原图地址，逗号分隔' AFTER `attachments`,
  ADD COLUMN `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '验收意见' AFTER `status`,
  ADD COLUMN `reviewed_at` DATETIME DEFAULT NULL COMMENT '验收时间' AFTER `review_remark`,
  ADD KEY `idx_order_stage_version` (`order_id`, `stage_index`, `submission_no`);

CREATE TABLE IF NOT EXISTS `order_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `actor_id` BIGINT DEFAULT NULL,
  `event_type` VARCHAR(50) NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_event` (`order_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单操作时间线';

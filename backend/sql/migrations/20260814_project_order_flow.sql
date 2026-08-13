USE `huiyue`;

ALTER TABLE `order_info`
  ADD COLUMN `application_id` BIGINT DEFAULT NULL COMMENT '企划订单对应的应征记录ID' AFTER `project_id`,
  ADD COLUMN `project_title` VARCHAR(200) DEFAULT NULL COMMENT '企划标题快照' AFTER `stall_cover`,
  ADD COLUMN `project_cover` VARCHAR(500) DEFAULT NULL COMMENT '企划封面快照' AFTER `project_title`,
  ADD COLUMN `project_snapshot` TEXT DEFAULT NULL COMMENT '企划与应征信息 JSON 快照' AFTER `project_cover`,
  ADD KEY `idx_application_status` (`application_id`, `status`, `deleted`);

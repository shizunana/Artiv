-- F9 沟通与消息模块增量迁移。
-- 在已有 huiyue 数据库执行；不删除任何业务或演示数据。
USE `huiyue`;

ALTER TABLE `message`
  ADD COLUMN `project_id` BIGINT DEFAULT NULL COMMENT '关联企划ID（应征私聊可为空）' AFTER `order_id`,
  ADD COLUMN `subtype` VARCHAR(50) DEFAULT NULL COMMENT '消息子类型，如 PRIVATE_CHAT / APPLICATION_CREATED' AFTER `type`,
  ADD COLUMN `conversation_key` VARCHAR(50) DEFAULT NULL COMMENT '双方会话键，按较小用户ID_较大用户ID归一化' AFTER `subtype`;

ALTER TABLE `message`
  ADD INDEX `idx_conversation` (`conversation_key`, `project_id`, `created_at`),
  ADD INDEX `idx_project` (`project_id`);

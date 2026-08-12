USE `huiyue`;

ALTER TABLE `message`
  ADD COLUMN `starter_key` VARCHAR(100) DEFAULT NULL COMMENT '项目会话发起标记；普通消息为空' AFTER `conversation_key`,
  ADD UNIQUE KEY `uk_project_conversation_starter` (`starter_key`);

USE `huiyue`;

ALTER TABLE `message`
  ADD COLUMN `starter_key` VARCHAR(100) DEFAULT NULL COMMENT '项目会话发起标记；普通消息为空' AFTER `conversation_key`;

UPDATE `message`
SET `starter_key` = CONCAT(`project_id`, ':', `conversation_key`, ':APPLICATION_CONVERSATION_STARTED')
WHERE `deleted` = 0
  AND `type` = 3
  AND `subtype` = 'APPLICATION_CONVERSATION_STARTED'
  AND `project_id` IS NOT NULL
  AND `conversation_key` IS NOT NULL;

ALTER TABLE `message`
  ADD UNIQUE KEY `uk_project_conversation_starter` (`starter_key`);

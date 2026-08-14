USE `artiv`;

ALTER TABLE `message`
  ADD COLUMN `starter_key` VARCHAR(100) DEFAULT NULL COMMENT '项目会话发起标记；普通消息为空' AFTER `conversation_key`;

-- Keep the earliest active starter per project/conversation. Later duplicate
-- markers retain a NULL starter_key; normal PRIVATE_CHAT replies never match.
UPDATE `message` AS duplicate_starter
JOIN `message` AS retained_starter
  ON retained_starter.`project_id` = duplicate_starter.`project_id`
  AND retained_starter.`conversation_key` = duplicate_starter.`conversation_key`
  AND retained_starter.`id` < duplicate_starter.`id`
  AND retained_starter.`deleted` = 0
  AND retained_starter.`type` = 3
  AND retained_starter.`subtype` = 'APPLICATION_CONVERSATION_STARTED'
SET duplicate_starter.`starter_key` = NULL
WHERE duplicate_starter.`deleted` = 0
  AND duplicate_starter.`type` = 3
  AND duplicate_starter.`subtype` = 'APPLICATION_CONVERSATION_STARTED'
  AND duplicate_starter.`project_id` IS NOT NULL
  AND duplicate_starter.`conversation_key` IS NOT NULL;

UPDATE `message` AS starter
LEFT JOIN `message` AS earlier_starter
  ON earlier_starter.`project_id` = starter.`project_id`
  AND earlier_starter.`conversation_key` = starter.`conversation_key`
  AND earlier_starter.`id` < starter.`id`
  AND earlier_starter.`deleted` = 0
  AND earlier_starter.`type` = 3
  AND earlier_starter.`subtype` = 'APPLICATION_CONVERSATION_STARTED'
SET starter.`starter_key` = CONCAT(starter.`project_id`, ':', starter.`conversation_key`, ':APPLICATION_CONVERSATION_STARTED')
WHERE starter.`deleted` = 0
  AND starter.`type` = 3
  AND starter.`subtype` = 'APPLICATION_CONVERSATION_STARTED'
  AND starter.`project_id` IS NOT NULL
  AND starter.`conversation_key` IS NOT NULL
  AND earlier_starter.`id` IS NULL;

ALTER TABLE `message`
  ADD UNIQUE KEY `uk_project_conversation_starter` (`starter_key`);

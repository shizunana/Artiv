USE `artiv`;

ALTER TABLE `invitation`
  ADD COLUMN `application_id` BIGINT DEFAULT NULL COMMENT '接受邀请后创建或恢复的应征ID' AFTER `status`,
  ADD COLUMN `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '婉拒理由' AFTER `application_id`,
  ADD COLUMN `responded_at` DATETIME DEFAULT NULL COMMENT '处理时间' AFTER `reject_reason`,
  ADD UNIQUE KEY `uk_invitation_project` (`project_id`),
  ADD KEY `idx_invitation_application` (`application_id`);

-- 旧流程仅修改邀请状态，没有生成应征和会话。此类记录不能继续保留为“已接受”，
-- 重置为待处理后由受邀画师按新流程重新确认报价，生成真实应征与会话。
UPDATE `invitation`
SET `status` = 0,
    `application_id` = NULL,
    `reject_reason` = NULL,
    `responded_at` = NULL
WHERE `status` = 1
  AND `application_id` IS NULL
  AND `deleted` = 0;

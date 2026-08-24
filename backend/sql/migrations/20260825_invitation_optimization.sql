USE `artiv`;

-- 历史接受记录此前只写入双方会话系统提示，补齐内容相同的独立系统消息。
INSERT INTO `message` (
  `from_id`, `to_id`, `project_id`, `type`, `subtype`, `content`,
  `read_at`, `deleted`, `created_at`, `updated_at`
)
SELECT
  NULL,
  p.`employer_id`,
  p.`id`,
  0,
  'INVITATION_ACCEPTED',
  CONCAT(
    '画师【', COALESCE(NULLIF(u.`nickname`, ''), CONCAT('画师 #', i.`painter_id`)),
    '】接受了您的企划【', p.`title`,
    '】的邀请，双方已建立会话，您可以开始与画师沟通后续合作内容。'
  ),
  NULL,
  0,
  COALESCE(i.`responded_at`, i.`updated_at`, CURRENT_TIMESTAMP),
  CURRENT_TIMESTAMP
FROM `invitation` i
JOIN `project` p ON p.`id` = i.`project_id` AND p.`deleted` = 0
LEFT JOIN `user` u ON u.`id` = i.`painter_id`
WHERE i.`deleted` = 0
  AND i.`status` = 1
  AND i.`application_id` IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `message` m
    WHERE m.`deleted` = 0
      AND m.`to_id` = p.`employer_id`
      AND m.`project_id` = p.`id`
      AND m.`type` = 0
      AND m.`subtype` = 'INVITATION_ACCEPTED'
  );

-- 历史拒绝记录按现行规则自动下架定向企划；邀请和拒绝消息继续保留。
UPDATE `project` p
JOIN `invitation` i ON i.`project_id` = p.`id`
SET p.`status` = 4,
    p.`updated_at` = CURRENT_TIMESTAMP
WHERE p.`deleted` = 0
  AND p.`mode` = 1
  AND p.`status` = 0
  AND i.`deleted` = 0
  AND i.`status` = 2;

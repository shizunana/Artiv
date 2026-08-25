-- 普通企划应征通知历史修复：将“画师 #ID”改为真实展示名。
-- 当前内容在画师 ID 后仍保留空格，因此可稳定提取 ID；更新后不再命中，脚本可幂等执行。

SET NAMES utf8mb4;

UPDATE `message` AS m
JOIN `user` AS u
  ON u.`id` = CAST(
    SUBSTRING_INDEX(
      SUBSTRING_INDEX(m.`content`, ' 应征了', 1),
      '画师 #',
      -1
    ) AS UNSIGNED
  )
JOIN `project` AS p ON p.`id` = m.`project_id`
SET m.`content` = CONCAT(
      CASE
        WHEN NULLIF(TRIM(u.`nickname`), '') IS NOT NULL THEN CONCAT('画师[', TRIM(u.`nickname`), ']')
        WHEN NULLIF(TRIM(u.`username`), '') IS NOT NULL THEN CONCAT('画师[', TRIM(u.`username`), ']')
        ELSE CONCAT('画师 #', u.`id`)
      END,
      '应征了您的企划[', p.`title`, ']。'
    ),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`deleted` = 0
  AND m.`subtype` = 'APPLICATION_CREATED'
  AND m.`content` REGEXP '^画师 #[0-9]+ 应征了你的企划「.*」。$';

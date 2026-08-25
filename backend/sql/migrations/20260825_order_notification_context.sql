-- 补齐历史接单与阶段提交通知中的画师名和订单标题。
-- 同一订单事件会生成 1 条 order_event、双方系统通知和 1 条会话系统提示，故两张表均需修正。
-- WHERE 仅匹配旧模板；更新后的正文不会再次命中，脚本可幂等执行。

SET NAMES utf8mb4;

UPDATE `order_event` AS e
JOIN `order_info` AS o ON o.`id` = e.`order_id`
JOIN `user` AS u ON u.`id` = o.`painter_id`
SET e.`content` = CONCAT(
      CASE
        WHEN NULLIF(TRIM(u.`nickname`), '') IS NOT NULL THEN CONCAT('画师[', TRIM(u.`nickname`), ']')
        WHEN NULLIF(TRIM(u.`username`), '') IS NOT NULL THEN CONCAT('画师[', TRIM(u.`username`), ']')
        ELSE CONCAT('画师[#', u.`id`, ']')
      END,
      '已确认接单，订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']已进入创作阶段。'
    )
WHERE e.`event_type` = 'ACCEPTED'
  AND e.`content` = '画师已确认接单，订单进入创作阶段。';

UPDATE `message` AS m
JOIN `order_info` AS o ON o.`id` = m.`order_id`
JOIN `user` AS u ON u.`id` = o.`painter_id`
SET m.`content` = CONCAT(
      CASE
        WHEN NULLIF(TRIM(u.`nickname`), '') IS NOT NULL THEN CONCAT('画师[', TRIM(u.`nickname`), ']')
        WHEN NULLIF(TRIM(u.`username`), '') IS NOT NULL THEN CONCAT('画师[', TRIM(u.`username`), ']')
        ELSE CONCAT('画师[#', u.`id`, ']')
      END,
      '已确认接单，订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']已进入创作阶段。'
    ),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`deleted` = 0
  AND m.`subtype` = 'ORDER_ACCEPTED'
  AND m.`content` = '画师已确认接单，订单进入创作阶段。';

UPDATE `order_event` AS e
JOIN `order_info` AS o ON o.`id` = e.`order_id`
JOIN `user` AS u ON u.`id` = o.`painter_id`
SET e.`content` = CONCAT(
      CASE
        WHEN NULLIF(TRIM(u.`nickname`), '') IS NOT NULL THEN CONCAT('画师[', TRIM(u.`nickname`), ']')
        WHEN NULLIF(TRIM(u.`username`), '') IS NOT NULL THEN CONCAT('画师[', TRIM(u.`username`), ']')
        ELSE CONCAT('画师[#', u.`id`, ']')
      END,
      '已提交订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']的[', SUBSTRING_INDEX(SUBSTRING_INDEX(e.`content`, '「', -1), '」', 1), ']，请验收。'
    )
WHERE e.`event_type` = 'STAGE_SUBMITTED'
  AND e.`content` REGEXP '^画师已提交「.*」第 [0-9]+ 版，请企划方验收。$';

UPDATE `message` AS m
JOIN `order_info` AS o ON o.`id` = m.`order_id`
JOIN `user` AS u ON u.`id` = o.`painter_id`
SET m.`content` = CONCAT(
      CASE
        WHEN NULLIF(TRIM(u.`nickname`), '') IS NOT NULL THEN CONCAT('画师[', TRIM(u.`nickname`), ']')
        WHEN NULLIF(TRIM(u.`username`), '') IS NOT NULL THEN CONCAT('画师[', TRIM(u.`username`), ']')
        ELSE CONCAT('画师[#', u.`id`, ']')
      END,
      '已提交订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']的[', SUBSTRING_INDEX(SUBSTRING_INDEX(m.`content`, '「', -1), '」', 1), ']，请验收。'
    ),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`deleted` = 0
  AND m.`subtype` = 'ORDER_STAGE_SUBMITTED'
  AND m.`content` REGEXP '^画师已提交「.*」第 [0-9]+ 版，请企划方验收。$';

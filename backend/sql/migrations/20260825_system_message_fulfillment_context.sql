-- 系统消息收尾：履约验收节点补齐订单标题，并将平台系统正文中的全角【】统一为半角[]。
-- 普通用户文字/图片私信不参与括号替换；WHERE 仅匹配旧履约模板，脚本可幂等执行。

SET NAMES utf8mb4;

UPDATE `order_event` AS e
JOIN `order_info` AS o ON o.`id` = e.`order_id`
SET e.`content` = CONCAT(
      '企划方已确认订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']当前交付并直接完成交易，后续创作阶段已跳过，款项已结算。'
    )
WHERE e.`event_type` = 'COMPLETED'
  AND e.`content` = '企划方已确认当前交付并直接完成交易，后续创作阶段已跳过，款项已结算。';

UPDATE `message` AS m
JOIN `order_info` AS o ON o.`id` = m.`order_id`
SET m.`content` = CONCAT(
      '企划方已确认订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']当前交付并直接完成交易，后续创作阶段已跳过，款项已结算。'
    ),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`deleted` = 0
  AND m.`subtype` = 'ORDER_COMPLETED'
  AND m.`content` = '企划方已确认当前交付并直接完成交易，后续创作阶段已跳过，款项已结算。';

UPDATE `order_event` AS e
JOIN `order_info` AS o ON o.`id` = e.`order_id`
SET e.`content` = CONCAT(
      '企划方已确认订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']的最终交付，订单已完成并结算。'
    )
WHERE e.`event_type` = 'COMPLETED'
  AND e.`content` = '企划方已确认最终交付，订单完成并结算。';

UPDATE `message` AS m
JOIN `order_info` AS o ON o.`id` = m.`order_id`
SET m.`content` = CONCAT(
      '企划方已确认订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']的最终交付，订单已完成并结算。'
    ),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`deleted` = 0
  AND m.`subtype` = 'ORDER_COMPLETED'
  AND m.`content` = '企划方已确认最终交付，订单完成并结算。';

UPDATE `order_event` AS e
JOIN `order_info` AS o ON o.`id` = e.`order_id`
SET e.`content` = CONCAT(
      '企划方要求修改订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']的[', SUBSTRING_INDEX(SUBSTRING_INDEX(e.`content`, '「', -1), '」', 1), ']：',
      SUBSTRING(e.`content`, LOCATE('」：', e.`content`) + CHAR_LENGTH('」：'))
    )
WHERE e.`event_type` = 'REVISION_REQUESTED'
  AND e.`content` REGEXP '^企划方要求修改「.*」：.*$';

UPDATE `message` AS m
JOIN `order_info` AS o ON o.`id` = m.`order_id`
SET m.`content` = CONCAT(
      '企划方要求修改订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']的[', SUBSTRING_INDEX(SUBSTRING_INDEX(m.`content`, '「', -1), '」', 1), ']：',
      SUBSTRING(m.`content`, LOCATE('」：', m.`content`) + CHAR_LENGTH('」：'))
    ),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`deleted` = 0
  AND m.`subtype` = 'ORDER_REVISION_REQUESTED'
  AND m.`content` REGEXP '^企划方要求修改「.*」：.*$';

UPDATE `order_event` AS e
JOIN `order_info` AS o ON o.`id` = e.`order_id`
SET e.`content` = CONCAT(
      '企划方已通过订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']的[', SUBSTRING_INDEX(SUBSTRING_INDEX(e.`content`, '「', -1), '」', 1), ']，已进入下一阶段。'
    )
WHERE e.`event_type` = 'STAGE_APPROVED'
  AND e.`content` REGEXP '^企划方已通过「.*」，进入下一阶段。$';

UPDATE `message` AS m
JOIN `order_info` AS o ON o.`id` = m.`order_id`
SET m.`content` = CONCAT(
      '企划方已通过订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']的[', SUBSTRING_INDEX(SUBSTRING_INDEX(m.`content`, '「', -1), '」', 1), ']，已进入下一阶段。'
    ),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`deleted` = 0
  AND m.`subtype` = 'ORDER_STAGE_APPROVED'
  AND m.`content` REGEXP '^企划方已通过「.*」，进入下一阶段。$';

-- 平台生成的通知和会话系统提示统一使用半角方括号；普通文字/图片私信保持用户原文。
UPDATE `message`
SET `content` = REPLACE(REPLACE(`content`, '【', '['), '】', ']'),
    `updated_at` = CURRENT_TIMESTAMP
WHERE `content` REGEXP '[【】]'
  AND (`type` <> 3 OR COALESCE(`subtype`, '') NOT IN ('PRIVATE_CHAT', 'PRIVATE_IMAGE'));

UPDATE `order_event`
SET `content` = REPLACE(REPLACE(`content`, '【', '['), '】', ']')
WHERE `content` REGEXP '[【】]';

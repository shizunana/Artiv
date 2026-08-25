-- 为企划方主动取消订单的历史事件与消息补齐订单标题。
-- 仅处理 ORDER_CANCELLED；画师拒单、封禁终止和仲裁退款继续保留各自文案。
-- WHERE 只匹配旧模板，脚本可幂等执行。

SET NAMES utf8mb4;

UPDATE `order_event` AS e
JOIN (
  SELECT DISTINCT `order_id`
  FROM `message`
  WHERE `deleted` = 0 AND `subtype` = 'ORDER_CANCELLED' AND `order_id` IS NOT NULL
) AS cancelled ON cancelled.`order_id` = e.`order_id`
JOIN `order_info` AS o ON o.`id` = e.`order_id`
SET e.`content` = CONCAT(
      '订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']已取消，托管金额已退回：',
      CASE
        WHEN e.`content` LIKE '订单已取消，托管金额已退回：%'
          THEN SUBSTRING(e.`content`, CHAR_LENGTH('订单已取消，托管金额已退回：') + 1)
        WHEN e.`content` LIKE '订单已取消，托管金额已退回、库存已恢复：%'
          THEN SUBSTRING(e.`content`, CHAR_LENGTH('订单已取消，托管金额已退回、库存已恢复：') + 1)
        WHEN e.`content` LIKE '%原因：%'
          THEN SUBSTRING_INDEX(e.`content`, '原因：', -1)
        ELSE '企划方取消订单'
      END
    )
WHERE e.`event_type` = 'CANCELLED'
  AND (
    e.`content` LIKE '订单已取消，托管金额已退回：%'
    OR e.`content` LIKE '订单已取消，托管金额已退回、库存已恢复：%'
    OR e.`content` LIKE '企划方已取消企划订单「%原因：%'
    OR e.`content` LIKE '企划方已取消橱窗订单「%原因：%'
  );

UPDATE `message` AS m
JOIN `order_info` AS o ON o.`id` = m.`order_id`
SET m.`content` = CONCAT(
      '订单[',
      CASE
        WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
          THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
        ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
      END,
      ']已取消，托管金额已退回：',
      CASE
        WHEN m.`content` LIKE '订单已取消，托管金额已退回：%'
          THEN SUBSTRING(m.`content`, CHAR_LENGTH('订单已取消，托管金额已退回：') + 1)
        WHEN m.`content` LIKE '订单已取消，托管金额已退回、库存已恢复：%'
          THEN SUBSTRING(m.`content`, CHAR_LENGTH('订单已取消，托管金额已退回、库存已恢复：') + 1)
        WHEN m.`content` LIKE '%原因：%'
          THEN SUBSTRING_INDEX(m.`content`, '原因：', -1)
        ELSE '企划方取消订单'
      END
    ),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`deleted` = 0
  AND m.`subtype` = 'ORDER_CANCELLED'
  AND (
    m.`content` LIKE '订单已取消，托管金额已退回：%'
    OR m.`content` LIKE '订单已取消，托管金额已退回、库存已恢复：%'
    OR m.`content` LIKE '企划方已取消企划订单「%原因：%'
    OR m.`content` LIKE '企划方已取消橱窗订单「%原因：%'
  );

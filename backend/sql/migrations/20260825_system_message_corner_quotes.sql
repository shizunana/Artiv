-- 系统消息对象格式统一：人名、标题、阶段由 [] / 【】 统一为中文单书名号「」。
-- 同时为应征、售后、争议、仲裁和评价通知补齐业务对象；普通用户私信不参与迁移。
-- MySQL 8，可重复执行。

SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS `_system_message_order_titles`;
CREATE TEMPORARY TABLE `_system_message_order_titles` AS
SELECT
  o.`id` AS `order_id`,
  CONCAT(
    '「',
    CASE
      WHEN CHAR_LENGTH(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))) > 10
        THEN CONCAT(LEFT(COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`)), 10), '...')
      ELSE COALESCE(NULLIF(TRIM(o.`project_title`), ''), NULLIF(TRIM(o.`stall_title`), ''), CONCAT('订单 #', o.`id`))
    END,
    '」'
  ) AS `marked_title`
FROM `order_info` AS o;

-- 平台生成的通知、业务系统提示和订单事件统一对象符号；用户文字/图片私信保持原文。
UPDATE `message`
SET `content` = REPLACE(REPLACE(REPLACE(REPLACE(`content`, '【', '「'), '】', '」'), '[', '「'), ']', '」'),
    `updated_at` = CURRENT_TIMESTAMP
WHERE (`type` IN (0, 1, 2)
       OR COALESCE(`subtype`, '') NOT IN ('', 'PRIVATE_CHAT', 'PRIVATE_IMAGE', 'PRIVATE_ORDER_CARD'))
  AND (LOCATE('[', `content`) > 0 OR LOCATE(']', `content`) > 0
       OR LOCATE('【', `content`) > 0 OR LOCATE('】', `content`) > 0);

UPDATE `order_event`
SET `content` = REPLACE(REPLACE(REPLACE(REPLACE(`content`, '【', '「'), '】', '」'), '[', '「'), ']', '」')
WHERE LOCATE('[', `content`) > 0 OR LOCATE(']', `content`) > 0
   OR LOCATE('【', `content`) > 0 OR LOCATE('】', `content`) > 0;

UPDATE `invitation`
SET `message` = REPLACE(REPLACE(REPLACE(REPLACE(`message`, '【', '「'), '】', '」'), '[', '「'), ']', '」'),
    `updated_at` = CURRENT_TIMESTAMP
WHERE LOCATE('[', `message`) > 0 OR LOCATE(']', `message`) > 0
   OR LOCATE('【', `message`) > 0 OR LOCATE('】', `message`) > 0;

-- 应征通知与会话系统提示补齐企划标题。
UPDATE `message` AS m
JOIN `project` AS p ON p.`id` = m.`project_id`
SET m.`content` = CONCAT(
      '您对企划「',
      IF(CHAR_LENGTH(TRIM(p.`title`)) > 10, CONCAT(LEFT(TRIM(p.`title`), 10), '...'), TRIM(p.`title`)),
      '」的应征已被企划方选中，可进入消息中心继续沟通。'
    ),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`subtype` = 'APPLICATION_SELECTED';

UPDATE `message` AS m
JOIN `project` AS p ON p.`id` = m.`project_id`
SET m.`content` = CONCAT(
      '企划方已就企划「',
      IF(CHAR_LENGTH(TRIM(p.`title`)) > 10, CONCAT(LEFT(TRIM(p.`title`), 10), '...'), TRIM(p.`title`)),
      '」发起沟通，双方可在这里继续沟通需求。'
    ),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`subtype` = 'APPLICATION_CONVERSATION_STARTED';

-- 售后、争议和仲裁补齐订单标题。
UPDATE `message` AS m
JOIN `_system_message_order_titles` AS t ON t.`order_id` = m.`order_id`
JOIN `order_info` AS o ON o.`id` = m.`order_id`
SET m.`content` = CONCAT('订单', t.`marked_title`, '已申请退款/售后。原因：', COALESCE(o.`after_sale_reason`, '未填写')),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`subtype` = 'ORDER_AFTER_SALE';

UPDATE `order_event` AS e
JOIN `_system_message_order_titles` AS t ON t.`order_id` = e.`order_id`
JOIN `order_info` AS o ON o.`id` = e.`order_id`
SET e.`content` = CONCAT('订单', t.`marked_title`, '已申请退款/售后。原因：', COALESCE(o.`after_sale_reason`, '未填写'))
WHERE e.`event_type` = 'AFTER_SALE';

UPDATE `message` AS m
JOIN `_system_message_order_titles` AS t ON t.`order_id` = m.`order_id`
JOIN `order_info` AS o ON o.`id` = m.`order_id`
SET m.`content` = CONCAT('订单', t.`marked_title`, '已发起争议并等待管理员仲裁。原因：', COALESCE(o.`after_sale_reason`, '未填写')),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`subtype` = 'ORDER_DISPUTE_CREATED';

UPDATE `order_event` AS e
JOIN `_system_message_order_titles` AS t ON t.`order_id` = e.`order_id`
JOIN `order_info` AS o ON o.`id` = e.`order_id`
SET e.`content` = CONCAT('订单', t.`marked_title`, '已发起争议并等待管理员仲裁。原因：', COALESCE(o.`after_sale_reason`, '未填写'))
WHERE e.`event_type` = 'DISPUTE_CREATED';

UPDATE `message` AS m
JOIN `_system_message_order_titles` AS t ON t.`order_id` = m.`order_id`
SET m.`content` = CONCAT('订单', t.`marked_title`, '的争议已补充图片证据。'),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`subtype` = 'ORDER_DISPUTE_EVIDENCE';

UPDATE `order_event` AS e
JOIN `_system_message_order_titles` AS t ON t.`order_id` = e.`order_id`
SET e.`content` = CONCAT('订单', t.`marked_title`, '的争议已补充图片证据。')
WHERE e.`event_type` = 'DISPUTE_EVIDENCE_ADDED';

UPDATE `message` AS m
JOIN `_system_message_order_titles` AS t ON t.`order_id` = m.`order_id`
SET m.`content` = CONCAT(
      '订单', t.`marked_title`, '已由管理员仲裁为全额结算给画师。说明：',
      CASE WHEN LOCATE('说明：', m.`content`) > 0 THEN SUBSTRING_INDEX(m.`content`, '说明：', -1) ELSE '管理员仲裁' END
    ),
    m.`updated_at` = CURRENT_TIMESTAMP
WHERE m.`subtype` = 'ORDER_ARBITRATION_SETTLED';

UPDATE `order_event` AS e
JOIN `_system_message_order_titles` AS t ON t.`order_id` = e.`order_id`
SET e.`content` = CONCAT(
      '订单', t.`marked_title`, '已由管理员仲裁为全额结算给画师。说明：',
      CASE WHEN LOCATE('说明：', e.`content`) > 0 THEN SUBSTRING_INDEX(e.`content`, '说明：', -1) ELSE '管理员仲裁' END
    )
WHERE e.`event_type` = 'ARBITRATION_SETTLED';

-- 评价通知通过相邻时间和接收人匹配原业务记录；有歧义的历史记录保持原文，避免误关联。
DROP TEMPORARY TABLE IF EXISTS `_review_received_notice_matches`;
CREATE TEMPORARY TABLE `_review_received_notice_matches` AS
SELECT m.`id` AS `message_id`, MIN(r.`order_id`) AS `order_id`
FROM `message` AS m
JOIN `review` AS r
  ON r.`to_id` = m.`to_id`
 AND ABS(TIMESTAMPDIFF(SECOND, r.`created_at`, m.`created_at`)) <= 10
WHERE m.`subtype` = 'REVIEW_RECEIVED'
GROUP BY m.`id`
HAVING COUNT(DISTINCT r.`id`) = 1;

UPDATE `message` AS m
JOIN `_review_received_notice_matches` AS x ON x.`message_id` = m.`id`
JOIN `_system_message_order_titles` AS t ON t.`order_id` = x.`order_id`
SET m.`content` = CONCAT('您收到一条关于订单', t.`marked_title`, '的交易评价，可在公开主页的合作评价中查看。'),
    m.`updated_at` = CURRENT_TIMESTAMP;

DROP TEMPORARY TABLE IF EXISTS `_review_appeal_notice_matches`;
CREATE TEMPORARY TABLE `_review_appeal_notice_matches` AS
SELECT m.`id` AS `message_id`, MIN(r.`order_id`) AS `order_id`
FROM `message` AS m
JOIN `review_appeal` AS a
  ON a.`applicant_id` = m.`to_id`
 AND (
      (m.`subtype` = 'REVIEW_APPEAL_SUBMITTED' AND ABS(TIMESTAMPDIFF(SECOND, a.`created_at`, m.`created_at`)) <= 10)
      OR (m.`subtype` IN ('REVIEW_APPEAL_APPROVED', 'REVIEW_APPEAL_REJECTED') AND ABS(TIMESTAMPDIFF(SECOND, a.`decided_at`, m.`created_at`)) <= 10)
    )
JOIN `review` AS r ON r.`id` = a.`review_id`
WHERE m.`subtype` IN ('REVIEW_APPEAL_SUBMITTED', 'REVIEW_APPEAL_APPROVED', 'REVIEW_APPEAL_REJECTED')
GROUP BY m.`id`
HAVING COUNT(DISTINCT a.`id`) = 1;

UPDATE `message` AS m
JOIN `_review_appeal_notice_matches` AS x ON x.`message_id` = m.`id`
JOIN `_system_message_order_titles` AS t ON t.`order_id` = x.`order_id`
SET m.`content` = CASE m.`subtype`
      WHEN 'REVIEW_APPEAL_SUBMITTED' THEN CONCAT('订单', t.`marked_title`, '的评价删除申诉已提交，管理员处理后会发送系统通知。')
      WHEN 'REVIEW_APPEAL_APPROVED' THEN CONCAT('订单', t.`marked_title`, '的评价删除申诉已通过。处理说明：', SUBSTRING_INDEX(m.`content`, '处理说明：', -1))
      ELSE CONCAT('订单', t.`marked_title`, '的评价删除申诉未通过。处理说明：', SUBSTRING_INDEX(m.`content`, '处理说明：', -1))
    END,
    m.`updated_at` = CURRENT_TIMESTAMP;

DROP TEMPORARY TABLE IF EXISTS `_review_hidden_notice_matches`;
CREATE TEMPORARY TABLE `_review_hidden_notice_matches` AS
SELECT m.`id` AS `message_id`, MIN(r.`order_id`) AS `order_id`
FROM `message` AS m
JOIN `review` AS r ON r.`from_id` = m.`to_id`
JOIN `review_appeal` AS a
  ON a.`review_id` = r.`id`
 AND ABS(TIMESTAMPDIFF(SECOND, a.`decided_at`, m.`created_at`)) <= 10
WHERE m.`subtype` = 'REVIEW_HIDDEN_AFTER_APPEAL'
GROUP BY m.`id`
HAVING COUNT(DISTINCT a.`id`) = 1;

UPDATE `message` AS m
JOIN `_review_hidden_notice_matches` AS x ON x.`message_id` = m.`id`
JOIN `_system_message_order_titles` AS t ON t.`order_id` = x.`order_id`
SET m.`content` = CONCAT(
      '您对订单', t.`marked_title`, '提交的一条交易评价经申诉审核后已被隐藏。处理说明：',
      SUBSTRING_INDEX(m.`content`, '处理说明：', -1)
    ),
    m.`updated_at` = CURRENT_TIMESTAMP;

DROP TEMPORARY TABLE IF EXISTS `_review_hidden_notice_matches`;
DROP TEMPORARY TABLE IF EXISTS `_review_appeal_notice_matches`;
DROP TEMPORARY TABLE IF EXISTS `_review_received_notice_matches`;
DROP TEMPORARY TABLE IF EXISTS `_system_message_order_titles`;

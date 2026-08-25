-- 当前有效应征关系增量迁移（MySQL 8）
-- application.status 保留用于旧状态兼容；active 是“我的应征”和企划应征列表的唯一展示依据。

USE `artiv`;

DROP PROCEDURE IF EXISTS `artiv_application_add_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `artiv_application_add_column_if_missing`(
  IN column_name_value VARCHAR(64), IN definition_value TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'application' AND COLUMN_NAME = column_name_value
  ) THEN
    SET @application_column_ddl = CONCAT(
      'ALTER TABLE `application` ADD COLUMN `', column_name_value, '` ', definition_value
    );
    PREPARE application_column_statement FROM @application_column_ddl;
    EXECUTE application_column_statement;
    DEALLOCATE PREPARE application_column_statement;
  END IF;
END$$
DELIMITER ;

CALL `artiv_application_add_column_if_missing`(
  'active', 'TINYINT NOT NULL DEFAULT 1 COMMENT ''是否为当前有效应征：0 否、1 是'' AFTER `status`'
);
CALL `artiv_application_add_column_if_missing`(
  'ended_reason', 'VARCHAR(30) DEFAULT NULL COMMENT ''关系终止原因：WITHDRAWN/REJECTED/ORDER_COMPLETED'' AFTER `active`'
);
CALL `artiv_application_add_column_if_missing`(
  'ended_at', 'DATETIME DEFAULT NULL COMMENT ''关系终止时间'' AFTER `ended_reason`'
);

DROP PROCEDURE IF EXISTS `artiv_application_add_column_if_missing`;

-- 旧数据中，只要状态仍有效，或仍有未结束的企划订单，应征关系就继续有效。
UPDATE `application` a
SET a.`active` = CASE
      WHEN a.`deleted` = 0 AND (
        a.`status` IN (0, 1)
        OR EXISTS (
          SELECT 1
          FROM `order_info` o
          WHERE o.`application_id` = a.`id`
            AND o.`deleted` = 0
            AND o.`status` IN (0, 1, 2, 5)
        )
      ) THEN 1
      ELSE 0
    END;

-- 修复“旧订单完成后又产生新订单”造成的 status=3 但关系仍有效数据。
UPDATE `application`
SET `status` = 0
WHERE `active` = 1
  AND `status` NOT IN (0, 1);

UPDATE `application`
SET `ended_reason` = NULL,
    `ended_at` = NULL
WHERE `active` = 1
  AND (`ended_reason` IS NOT NULL OR `ended_at` IS NOT NULL);

UPDATE `application` a
SET a.`ended_reason` = CASE
      WHEN a.`status` = 2 THEN 'REJECTED'
      WHEN EXISTS (
        SELECT 1
        FROM `order_info` o
        WHERE o.`application_id` = a.`id`
          AND o.`deleted` = 0
          AND o.`status` = 3
      ) THEN 'ORDER_COMPLETED'
      ELSE 'WITHDRAWN'
    END,
    a.`ended_at` = COALESCE(a.`updated_at`, CURRENT_TIMESTAMP)
WHERE a.`active` = 0
  AND a.`deleted` = 0;

DROP PROCEDURE IF EXISTS `artiv_application_add_index_if_missing`;
DELIMITER $$
CREATE PROCEDURE `artiv_application_add_index_if_missing`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'application'
      AND INDEX_NAME = 'idx_application_painter_active'
  ) THEN
    ALTER TABLE `application`
      ADD KEY `idx_application_painter_active` (`painter_id`, `active`, `created_at`);
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'application'
      AND INDEX_NAME = 'idx_application_project_active'
  ) THEN
    ALTER TABLE `application`
      ADD KEY `idx_application_project_active` (`project_id`, `active`, `created_at`);
  END IF;
END$$
DELIMITER ;

CALL `artiv_application_add_index_if_missing`();
DROP PROCEDURE IF EXISTS `artiv_application_add_index_if_missing`;

-- 企划预算区间增量迁移（MySQL 8）
-- 保留 legacy budget 字段作为最高预算兼容值；历史企划按固定预算回填为同值区间。

USE `artiv`;

DROP PROCEDURE IF EXISTS `artiv_budget_add_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `artiv_budget_add_column_if_missing`(
  IN table_name_value VARCHAR(64), IN column_name_value VARCHAR(64), IN definition_value TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = table_name_value AND COLUMN_NAME = column_name_value
  ) THEN
    SET @budget_column_ddl = CONCAT(
      'ALTER TABLE `', table_name_value, '` ADD COLUMN `', column_name_value, '` ', definition_value
    );
    PREPARE budget_column_statement FROM @budget_column_ddl;
    EXECUTE budget_column_statement;
    DEALLOCATE PREPARE budget_column_statement;
  END IF;
END$$
DELIMITER ;

CALL `artiv_budget_add_column_if_missing`(
  'project', 'budget_min', 'DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''预算区间下限（元）'' AFTER `budget`'
);
CALL `artiv_budget_add_column_if_missing`(
  'project', 'budget_max', 'DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''预算区间上限（元）'' AFTER `budget_min`'
);

UPDATE `project`
SET `budget_min` = `budget`,
    `budget_max` = `budget`
WHERE `budget_min` <= 0 OR `budget_max` <= 0;

DROP PROCEDURE IF EXISTS `artiv_budget_add_column_if_missing`;

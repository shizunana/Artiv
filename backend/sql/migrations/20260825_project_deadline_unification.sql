-- 企划时间规则统一（MySQL 8）
-- deadline 是唯一企划时间字段：当天仍可招募，次日起动态过期。
-- recruit_deadline 仅用于历史 deadline 为空时回填，随后彻底删除。

USE `artiv`;

DROP PROCEDURE IF EXISTS `artiv_unify_project_deadline`;
DELIMITER $$
CREATE PROCEDURE `artiv_unify_project_deadline`()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project' AND COLUMN_NAME = 'recruit_deadline'
  ) THEN
    SET @deadline_backfill_sql =
      'UPDATE `project` SET `deadline` = DATE(`recruit_deadline`) WHERE `deadline` IS NULL AND `recruit_deadline` IS NOT NULL';
    PREPARE deadline_backfill_statement FROM @deadline_backfill_sql;
    EXECUTE deadline_backfill_statement;
    DEALLOCATE PREPARE deadline_backfill_statement;
  END IF;

  UPDATE `project`
  SET `deadline` = DATE_ADD(DATE(COALESCE(`created_at`, NOW())), INTERVAL 10 DAY)
  WHERE `deadline` IS NULL;

  ALTER TABLE `project`
    MODIFY COLUMN `deadline` DATE NOT NULL
      COMMENT '截稿日期（招募截止及订单约定参考日期，当天有效）';

  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project' AND COLUMN_NAME = 'recruit_deadline'
  ) THEN
    ALTER TABLE `project` DROP COLUMN `recruit_deadline`;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project' AND INDEX_NAME = 'idx_project_public'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project'
      AND INDEX_NAME = 'idx_project_public' AND COLUMN_NAME = 'deadline'
  ) THEN
    ALTER TABLE `project` DROP INDEX `idx_project_public`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project' AND INDEX_NAME = 'idx_project_public'
  ) THEN
    ALTER TABLE `project`
      ADD INDEX `idx_project_public` (`moderation_status`, `status`, `deleted`, `deadline`);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project' AND INDEX_NAME = 'idx_project_deadline'
  ) THEN
    ALTER TABLE `project` DROP INDEX `idx_project_deadline`;
  END IF;
END$$
DELIMITER ;

CALL `artiv_unify_project_deadline`();
DROP PROCEDURE IF EXISTS `artiv_unify_project_deadline`;

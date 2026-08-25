-- 公开企划主动邀请画师（MySQL 8）
-- 新增邀请报价、带价邀请、已失效状态，并允许同一企划重复邀请同一画师。

USE `artiv`;

DROP PROCEDURE IF EXISTS `artiv_public_project_invitations`;
DELIMITER $$
CREATE PROCEDURE `artiv_public_project_invitations`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invitation' AND COLUMN_NAME = 'invitation_type'
  ) THEN
    ALTER TABLE `invitation`
      ADD COLUMN `invitation_type` TINYINT NOT NULL DEFAULT 0
        COMMENT '类型：0 定向邀请、1 邀请报价、2 带价邀请' AFTER `message`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invitation' AND COLUMN_NAME = 'offered_price'
  ) THEN
    ALTER TABLE `invitation`
      ADD COLUMN `offered_price` DECIMAL(10,2) DEFAULT NULL
        COMMENT '带价邀请价格' AFTER `invitation_type`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'application' AND COLUMN_NAME = 'quote_source'
  ) THEN
    ALTER TABLE `application`
      ADD COLUMN `quote_source` TINYINT NOT NULL DEFAULT 0
        COMMENT '报价来源：0 画师报价、1 企划方带价邀请' AFTER `quote`;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invitation' AND INDEX_NAME = 'uk_invitation_project'
  ) THEN
    ALTER TABLE `invitation` DROP INDEX `uk_invitation_project`;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invitation' AND INDEX_NAME = 'uk_project_painter'
  ) THEN
    ALTER TABLE `invitation` DROP INDEX `uk_project_painter`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invitation'
      AND INDEX_NAME = 'idx_invitation_project_painter_status'
  ) THEN
    ALTER TABLE `invitation`
      ADD INDEX `idx_invitation_project_painter_status`
        (`project_id`, `painter_id`, `status`, `created_at`);
  END IF;
END$$
DELIMITER ;

CALL `artiv_public_project_invitations`();
DROP PROCEDURE IF EXISTS `artiv_public_project_invitations`;

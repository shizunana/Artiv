-- 收到的邀请提醒箱：隐藏仅影响当前画师的提醒列表，不改变邀请业务状态。

USE `artiv`;

DROP PROCEDURE IF EXISTS `artiv_invitation_add_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `artiv_invitation_add_column_if_missing`(
  IN column_name_value VARCHAR(64), IN definition_value TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invitation' AND COLUMN_NAME = column_name_value
  ) THEN
    SET @invitation_column_ddl = CONCAT(
      'ALTER TABLE `invitation` ADD COLUMN `', column_name_value, '` ', definition_value
    );
    PREPARE invitation_column_statement FROM @invitation_column_ddl;
    EXECUTE invitation_column_statement;
    DEALLOCATE PREPARE invitation_column_statement;
  END IF;
END$$
DELIMITER ;

CALL `artiv_invitation_add_column_if_missing`(
  'painter_hidden', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''画师是否隐藏邀请提醒：0 否、1 是'' AFTER `responded_at`'
);
CALL `artiv_invitation_add_column_if_missing`(
  'painter_hidden_at', 'DATETIME DEFAULT NULL COMMENT ''画师隐藏邀请提醒时间'' AFTER `painter_hidden`'
);

DROP PROCEDURE IF EXISTS `artiv_invitation_add_column_if_missing`;

DROP PROCEDURE IF EXISTS `artiv_invitation_add_index_if_missing`;
DELIMITER $$
CREATE PROCEDURE `artiv_invitation_add_index_if_missing`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'invitation'
      AND INDEX_NAME = 'idx_invitation_painter_reminder'
  ) THEN
    ALTER TABLE `invitation`
      ADD KEY `idx_invitation_painter_reminder` (`painter_id`, `painter_hidden`, `status`, `created_at`);
  END IF;
END$$
DELIMITER ;

CALL `artiv_invitation_add_index_if_missing`();
DROP PROCEDURE IF EXISTS `artiv_invitation_add_index_if_missing`;

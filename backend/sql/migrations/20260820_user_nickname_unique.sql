-- 用户昵称唯一性迁移
-- 规则：nickname 在 utf8mb4_unicode_ci 排序规则下全表唯一。
-- 历史同名用户保留最早用户的昵称，后续用户依次追加 1、2、3……。

USE `artiv`;

DROP PROCEDURE IF EXISTS `migrate_unique_user_nicknames`;

DELIMITER $$
CREATE PROCEDURE `migrate_unique_user_nicknames`()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE target_id BIGINT;
  DECLARE original_nickname VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  DECLARE candidate_nickname VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  DECLARE candidate_suffix INT DEFAULT 1;

  DECLARE duplicate_cursor CURSOR FOR
    SELECT ranked.id, ranked.nickname
    FROM (
      SELECT
        u.id,
        u.nickname,
        ROW_NUMBER() OVER (PARTITION BY u.nickname ORDER BY u.id) AS duplicate_rank
      FROM `user` u
    ) ranked
    WHERE ranked.duplicate_rank > 1
    ORDER BY ranked.id;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN duplicate_cursor;
  rename_loop: LOOP
    FETCH duplicate_cursor INTO target_id, original_nickname;
    IF done = 1 THEN
      LEAVE rename_loop;
    END IF;

    SET candidate_suffix = 1;
    SET candidate_nickname = CONCAT(
      LEFT(TRIM(original_nickname), 50 - CHAR_LENGTH(candidate_suffix)),
      candidate_suffix
    );

    WHILE EXISTS (
      SELECT 1 FROM `user`
      WHERE `nickname` = candidate_nickname AND `id` <> target_id
    ) DO
      SET candidate_suffix = candidate_suffix + 1;
      SET candidate_nickname = CONCAT(
        LEFT(TRIM(original_nickname), 50 - CHAR_LENGTH(candidate_suffix)),
        candidate_suffix
      );
    END WHILE;

    UPDATE `user`
    SET `nickname` = candidate_nickname
    WHERE `id` = target_id;
  END LOOP;
  CLOSE duplicate_cursor;
END$$
DELIMITER ;

CALL `migrate_unique_user_nicknames`();
DROP PROCEDURE IF EXISTS `migrate_unique_user_nicknames`;

SET @has_unique_nickname = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'user'
    AND column_name = 'nickname'
    AND non_unique = 0
);
SET @add_unique_nickname_sql = IF(
  @has_unique_nickname > 0,
  'SELECT 1',
  'ALTER TABLE `user` ADD UNIQUE KEY `uk_nickname` (`nickname`)'
);
PREPARE add_unique_nickname_stmt FROM @add_unique_nickname_sql;
EXECUTE add_unique_nickname_stmt;
DEALLOCATE PREPARE add_unique_nickname_stmt;

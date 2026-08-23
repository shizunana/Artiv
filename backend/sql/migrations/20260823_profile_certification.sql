-- 统一账户资料与画师认证增量迁移（MySQL 8）
-- 只新增字段和索引，并迁移旧字段语义；不删除或重置业务数据。

USE `artiv`;

DROP PROCEDURE IF EXISTS `artiv_profile_add_column_if_missing`;
DROP PROCEDURE IF EXISTS `artiv_profile_add_index_if_missing`;
DELIMITER $$
CREATE PROCEDURE `artiv_profile_add_column_if_missing`(
  IN table_name_value VARCHAR(64), IN column_name_value VARCHAR(64), IN definition_value TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = table_name_value AND COLUMN_NAME = column_name_value
  ) THEN
    SET @profile_column_ddl = CONCAT(
      'ALTER TABLE `', table_name_value, '` ADD COLUMN `', column_name_value, '` ', definition_value
    );
    PREPARE profile_column_statement FROM @profile_column_ddl;
    EXECUTE profile_column_statement;
    DEALLOCATE PREPARE profile_column_statement;
  END IF;
END$$

CREATE PROCEDURE `artiv_profile_add_index_if_missing`(
  IN table_name_value VARCHAR(64), IN index_name_value VARCHAR(64), IN definition_value TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = table_name_value AND INDEX_NAME = index_name_value
  ) THEN
    SET @profile_index_ddl = CONCAT(
      'ALTER TABLE `', table_name_value, '` ADD INDEX `', index_name_value, '` ', definition_value
    );
    PREPARE profile_index_statement FROM @profile_index_ddl;
    EXECUTE profile_index_statement;
    DEALLOCATE PREPARE profile_index_statement;
  END IF;
END$$
DELIMITER ;

CALL `artiv_profile_add_column_if_missing`(
  'user', 'employer_bio', 'VARCHAR(500) DEFAULT NULL COMMENT ''企划方公开主页简介'' AFTER `bio`'
);
CALL `artiv_profile_add_column_if_missing`(
  'user', 'artist_name', 'VARCHAR(100) DEFAULT NULL COMMENT ''已审核生效的画师笔名'' AFTER `tags`'
);
CALL `artiv_profile_add_column_if_missing`(
  'user', 'artist_site', 'VARCHAR(500) DEFAULT NULL COMMENT ''已审核生效的画师常用站'' AFTER `artist_name`'
);

CALL `artiv_profile_add_column_if_missing`(
  'artist_certification', 'application_type',
  'TINYINT NOT NULL DEFAULT 0 COMMENT ''申请类型：0初次认证、1认证资料修改'' AFTER `user_id`'
);
CALL `artiv_profile_add_column_if_missing`(
  'artist_certification', 'previous_common_id',
  'VARCHAR(100) DEFAULT NULL COMMENT ''提交修改时已生效笔名快照'' AFTER `common_site`'
);
CALL `artiv_profile_add_column_if_missing`(
  'artist_certification', 'previous_common_site',
  'VARCHAR(500) DEFAULT NULL COMMENT ''提交修改时已生效常用站快照'' AFTER `previous_common_id`'
);
CALL `artiv_profile_add_index_if_missing`(
  'artist_certification', 'idx_cert_user_type_status',
  '(`user_id`, `application_type`, `status`, `deleted`, `created_at`)'
);

-- 旧版本只有一个 bio。仅将注册角色为企划方且尚未迁移的简介搬入企划方简介；
-- employer_bio 写为空串作为已迁移标记，保证脚本重复执行时不会覆盖后续新增的画师简介。
UPDATE `user`
SET `employer_bio` = COALESCE(`bio`, ''),
    `bio` = NULL,
    `updated_at` = NOW()
WHERE `role` = 1 AND `employer_bio` IS NULL;

-- 旧字段无法区分双身份。若企划方注册账号已经进入画师认证流程或已有作品，
-- 同时保留一份旧简介作为画师简介，避免迁移让既有画师主页资料丢失。
UPDATE `user` u
SET u.`bio` = NULLIF(u.`employer_bio`, ''),
    u.`updated_at` = NOW()
WHERE u.`role` = 1
  AND u.`bio` IS NULL
  AND NULLIF(u.`employer_bio`, '') IS NOT NULL
  AND (
    u.`cert_status` IN (1, 2)
    OR EXISTS (SELECT 1 FROM `work` w WHERE w.`user_id` = u.`id` AND w.`deleted` = 0)
  );

-- 旧的“已驳回”用户态合并回未认证；拒绝结果仍保留在申请历史中。
UPDATE `user`
SET `cert_status` = 0,
    `updated_at` = NOW()
WHERE `cert_status` = 3;

-- 旧库可能只留下 user.cert_status=1，却没有对应的待审申请记录。
-- 这种孤立状态无法在管理员后台处理，统一恢复为未认证，允许用户重新提交。
UPDATE `user` u
SET u.`cert_status` = 0,
    u.`updated_at` = NOW()
WHERE u.`cert_status` = 1
  AND u.`deleted` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `artist_certification` c
    WHERE c.`user_id` = u.`id`
      AND c.`application_type` = 0
      AND c.`status` = 0
      AND c.`deleted` = 0
  );

-- 为历史已认证用户补齐正式认证资料。优先采用最近一笔已通过申请，
-- 若旧数据没有申请记录，则以当前昵称作为笔名兜底。
UPDATE `user` u
LEFT JOIN `artist_certification` c ON c.`id` = (
  SELECT c2.`id`
  FROM `artist_certification` c2
  WHERE c2.`user_id` = u.`id`
    AND c2.`status` = 1
    AND c2.`deleted` = 0
  ORDER BY c2.`reviewed_at` DESC, c2.`id` DESC
  LIMIT 1
)
SET u.`artist_name` = COALESCE(NULLIF(u.`artist_name`, ''), c.`common_id`, u.`nickname`),
    u.`artist_site` = COALESCE(NULLIF(u.`artist_site`, ''), c.`common_site`),
    u.`updated_at` = NOW()
WHERE u.`cert_status` = 2 AND u.`deleted` = 0;

DROP PROCEDURE IF EXISTS `artiv_profile_add_column_if_missing`;
DROP PROCEDURE IF EXISTS `artiv_profile_add_index_if_missing`;

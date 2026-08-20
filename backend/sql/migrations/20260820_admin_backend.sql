-- 管理员后台增量迁移（MySQL 8.0.41）
-- 只新增字段、表与索引，不删除或重置现有业务数据。

USE `artiv`;

-- MySQL 8.0 不支持 ADD COLUMN/INDEX IF NOT EXISTS，使用临时过程保证重复执行安全。
DROP PROCEDURE IF EXISTS `artiv_add_column_if_missing`;
DROP PROCEDURE IF EXISTS `artiv_add_index_if_missing`;
DELIMITER $$
CREATE PROCEDURE `artiv_add_column_if_missing`(IN table_name_value VARCHAR(64), IN column_name_value VARCHAR(64), IN definition_value TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = table_name_value AND COLUMN_NAME = column_name_value
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD COLUMN `', column_name_value, '` ', definition_value);
    PREPARE migration_statement FROM @ddl;
    EXECUTE migration_statement;
    DEALLOCATE PREPARE migration_statement;
  END IF;
END$$
CREATE PROCEDURE `artiv_add_index_if_missing`(IN table_name_value VARCHAR(64), IN index_name_value VARCHAR(64), IN definition_value TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = table_name_value AND INDEX_NAME = index_name_value
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD INDEX `', index_name_value, '` ', definition_value);
    PREPARE migration_statement FROM @ddl;
    EXECUTE migration_statement;
    DEALLOCATE PREPARE migration_statement;
  END IF;
END$$
DELIMITER ;

CALL `artiv_add_column_if_missing`('user', 'ban_reason', 'VARCHAR(500) DEFAULT NULL COMMENT ''最近一次封禁原因'' AFTER `status`');
CALL `artiv_add_column_if_missing`('user', 'banned_by', 'BIGINT DEFAULT NULL COMMENT ''执行封禁的管理员ID'' AFTER `ban_reason`');
CALL `artiv_add_column_if_missing`('user', 'banned_at', 'DATETIME DEFAULT NULL COMMENT ''封禁时间'' AFTER `banned_by`');
CALL `artiv_add_column_if_missing`('user', 'banned_until', 'DATETIME DEFAULT NULL COMMENT ''限时封禁截止时间；NULL为永久封禁'' AFTER `banned_at`');
CALL `artiv_add_index_if_missing`('user', 'idx_user_status_ban', '(`status`, `banned_until`, `deleted`)');
CALL `artiv_add_index_if_missing`('user', 'idx_user_created', '(`created_at`, `deleted`)');
CALL `artiv_add_index_if_missing`('user', 'idx_user_cert', '(`cert_status`, `deleted`)');

CALL `artiv_add_column_if_missing`('project', 'moderation_status', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''审核状态：0正常、1违规下架'' AFTER `status`');
CALL `artiv_add_column_if_missing`('project', 'moderation_reason', 'VARCHAR(500) DEFAULT NULL COMMENT ''下架或恢复原因'' AFTER `moderation_status`');
CALL `artiv_add_column_if_missing`('project', 'moderated_by', 'BIGINT DEFAULT NULL COMMENT ''最近操作管理员ID'' AFTER `moderation_reason`');
CALL `artiv_add_column_if_missing`('project', 'moderated_at', 'DATETIME DEFAULT NULL COMMENT ''最近审核操作时间'' AFTER `moderated_by`');
CALL `artiv_add_index_if_missing`('project', 'idx_project_public', '(`moderation_status`, `status`, `deleted`)');
CALL `artiv_add_index_if_missing`('project', 'idx_project_created', '(`created_at`, `deleted`)');

CREATE TABLE IF NOT EXISTS `artist_certification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '申请画师ID',
  `common_id` VARCHAR(100) NOT NULL COMMENT '常用昵称或常用ID',
  `common_site` VARCHAR(500) DEFAULT NULL COMMENT '其他平台个人主页',
  `public_work_count` INT NOT NULL DEFAULT 0 COMMENT '提交时公开作品数量快照',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核、1通过、2驳回',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核管理员ID',
  `review_reason` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  `reviewed_at` DATETIME DEFAULT NULL COMMENT '审核时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_cert_user_created` (`user_id`, `created_at`),
  KEY `idx_cert_status_created` (`status`, `created_at`, `deleted`)
) ENGINE=InnoDB COMMENT='画师认证申请';

CREATE TABLE IF NOT EXISTS `order_dispute` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `initiator_id` BIGINT NOT NULL COMMENT '发起人ID',
  `reason` VARCHAR(500) NOT NULL COMMENT '争议原因',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待仲裁、1已处理',
  `result` TINYINT DEFAULT NULL COMMENT '1全额退款企划方、2全额结算画师、3预留部分退款',
  `resolution_reason` VARCHAR(500) DEFAULT NULL COMMENT '仲裁说明',
  `resolved_by` BIGINT DEFAULT NULL COMMENT '仲裁管理员ID',
  `resolved_at` DATETIME DEFAULT NULL COMMENT '仲裁时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dispute_order` (`order_id`),
  KEY `idx_dispute_status_created` (`status`, `created_at`, `deleted`)
) ENGINE=InnoDB COMMENT='订单争议';

CREATE TABLE IF NOT EXISTS `order_dispute_evidence` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dispute_id` BIGINT NOT NULL COMMENT '争议ID',
  `submitted_by` BIGINT NOT NULL COMMENT '证据提交人ID',
  `image` VARCHAR(255) NOT NULL COMMENT '证据图片地址',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '证据说明',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_evidence_dispute_user` (`dispute_id`, `submitted_by`, `created_at`)
) ENGINE=InnoDB COMMENT='订单争议图片证据';

-- 兼容迁移前的审核中认证与旧售后记录，避免后台出现“状态存在但队列为空”。
INSERT INTO `artist_certification`
  (`user_id`, `common_id`, `common_site`, `public_work_count`, `status`, `review_reason`, `reviewed_at`, `deleted`, `created_at`, `updated_at`)
SELECT u.`id`, u.`nickname`, NULL,
       (SELECT COUNT(*) FROM `work` w WHERE w.`user_id` = u.`id` AND w.`visibility` = 1 AND w.`deleted` = 0),
       CASE WHEN (SELECT COUNT(*) FROM `work` w WHERE w.`user_id` = u.`id` AND w.`visibility` = 1 AND w.`deleted` = 0) >= 4 THEN 0 ELSE 2 END,
       CASE WHEN (SELECT COUNT(*) FROM `work` w WHERE w.`user_id` = u.`id` AND w.`visibility` = 1 AND w.`deleted` = 0) >= 4 THEN NULL ELSE '历史申请公开作品不足4件，系统未进入审核队列' END,
       CASE WHEN (SELECT COUNT(*) FROM `work` w WHERE w.`user_id` = u.`id` AND w.`visibility` = 1 AND w.`deleted` = 0) >= 4 THEN NULL ELSE NOW() END,
       0, COALESCE(u.`updated_at`, NOW()), COALESCE(u.`updated_at`, NOW())
FROM `user` u
WHERE u.`role` = 2 AND u.`cert_status` = 1 AND u.`deleted` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `artist_certification` c WHERE c.`user_id` = u.`id` AND c.`deleted` = 0
  );

-- 早期版本曾允许不足 4 件作品的申请进入审核中。保留历史记录，但退出待审队列。
UPDATE `artist_certification`
SET `status` = 2,
    `review_reason` = COALESCE(`review_reason`, '历史申请公开作品不足4件，系统未进入审核队列'),
    `reviewed_at` = COALESCE(`reviewed_at`, NOW()),
    `updated_at` = NOW()
WHERE `status` = 0 AND `deleted` = 0 AND `public_work_count` < 4;

UPDATE `user` u
SET u.`cert_status` = 3, u.`updated_at` = NOW()
WHERE u.`cert_status` = 1 AND u.`deleted` = 0
  AND EXISTS (
    SELECT 1 FROM `artist_certification` c
    WHERE c.`user_id` = u.`id` AND c.`status` = 2 AND c.`public_work_count` < 4 AND c.`deleted` = 0
  )
  AND NOT EXISTS (
    SELECT 1 FROM `artist_certification` c
    WHERE c.`user_id` = u.`id` AND c.`status` = 0 AND c.`deleted` = 0
  );

INSERT INTO `order_dispute`
  (`order_id`, `initiator_id`, `reason`, `status`, `deleted`, `created_at`, `updated_at`)
SELECT o.`id`, o.`employer_id`, COALESCE(NULLIF(o.`after_sale_reason`, ''), '历史售后记录'),
       0, 0, COALESCE(o.`updated_at`, NOW()), COALESCE(o.`updated_at`, NOW())
FROM `order_info` o
WHERE o.`status` = 5 AND o.`deleted` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `order_dispute` d WHERE d.`order_id` = o.`id`
  );

CREATE TABLE IF NOT EXISTS `admin_action_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `admin_id` BIGINT DEFAULT NULL COMMENT '管理员ID；系统任务为空',
  `action_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `target_type` VARCHAR(50) NOT NULL COMMENT '目标资源类型',
  `target_id` BIGINT DEFAULT NULL COMMENT '目标资源ID',
  `before_data` TEXT DEFAULT NULL COMMENT '操作前JSON快照',
  `after_data` TEXT DEFAULT NULL COMMENT '操作后JSON快照',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '操作原因',
  `request_id` VARCHAR(64) DEFAULT NULL COMMENT '请求标识',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT '来源IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_log_created` (`created_at`),
  KEY `idx_admin_log_target` (`target_type`, `target_id`, `created_at`),
  KEY `idx_admin_log_admin` (`admin_id`, `created_at`)
) ENGINE=InnoDB COMMENT='管理员操作审计日志';

CALL `artiv_add_index_if_missing`('order_info', 'idx_order_status_created', '(`status`, `created_at`, `deleted`)');
CALL `artiv_add_index_if_missing`('order_info', 'idx_order_completed', '(`completed_at`, `deleted`)');
CALL `artiv_add_index_if_missing`('application', 'idx_application_project_created', '(`project_id`, `created_at`, `deleted`)');

DROP PROCEDURE IF EXISTS `artiv_add_column_if_missing`;
DROP PROCEDURE IF EXISTS `artiv_add_index_if_missing`;

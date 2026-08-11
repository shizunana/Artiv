-- =====================================================
-- 绘约（HuiYue）数据库初始化脚本
-- MySQL 8.0.41 ｜ 字符集 utf8mb4 ｜ 引擎 InnoDB
-- 执行方式：mysql -u root -p < init.sql 或在 Workbench 中执行
-- =====================================================

CREATE DATABASE IF NOT EXISTS `huiyue`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `huiyue`;

-- -----------------------------------------------------
-- 用户表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`   VARCHAR(50)  NOT NULL COMMENT '用户名',
  `password`   VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
  `nickname`   VARCHAR(50)  NOT NULL COMMENT '昵称',
  `avatar`     VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  `role`       TINYINT      NOT NULL DEFAULT 2 COMMENT '角色：1 企划方、2 画师、3 管理员',
  `status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 正常、1 封禁',
  `intro`      VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
  `deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删、1 已删',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB COMMENT = '用户表';

-- -----------------------------------------------------
-- 企划表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `planner_id`  BIGINT        NOT NULL COMMENT '企划方用户ID',
  `title`       VARCHAR(100)  NOT NULL COMMENT '标题',
  `description` TEXT          NOT NULL COMMENT '需求描述',
  `budget`      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '预算（元）',
  `deadline`    DATE          DEFAULT NULL COMMENT '截止时间',
  `tags`        VARCHAR(255)  DEFAULT NULL COMMENT '标签，逗号分隔',
  `images`      TEXT          DEFAULT NULL COMMENT '参考图，逗号分隔',
  `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 招募中、1 已选定、2 已完成、3 已下架',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_planner` (`planner_id`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT = '企划表';

-- -----------------------------------------------------
-- 应征表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `application`;
CREATE TABLE `application` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`  BIGINT        NOT NULL COMMENT '企划ID',
  `artist_id`   BIGINT        NOT NULL COMMENT '画师用户ID',
  `quote`       DECIMAL(10,2) DEFAULT NULL COMMENT '报价（元）',
  `description` VARCHAR(500)  DEFAULT NULL COMMENT '应征说明',
  `works`       TEXT          DEFAULT NULL COMMENT '作品地址，逗号分隔',
  `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 待处理、1 已通过、2 已拒绝',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_artist` (`project_id`, `artist_id`),
  KEY `idx_artist` (`artist_id`)
) ENGINE = InnoDB COMMENT = '应征表';

-- -----------------------------------------------------
-- 订单表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`  BIGINT        NOT NULL COMMENT '企划ID',
  `planner_id`  BIGINT        NOT NULL COMMENT '企划方用户ID',
  `artist_id`   BIGINT        NOT NULL COMMENT '画师用户ID',
  `amount`      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '成交金额（元）',
  `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 待接单、1 进行中、2 待验收、3 已完成、4 已取消、5 争议中',
  `current_stage` TINYINT     NOT NULL DEFAULT 0 COMMENT '当前阶段：0 初稿、1 修改、2 终稿、3 交付',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_planner` (`planner_id`),
  KEY `idx_artist` (`artist_id`),
  KEY `idx_project` (`project_id`)
) ENGINE = InnoDB COMMENT = '订单表';

-- -----------------------------------------------------
-- 订单阶段表（分阶段验收）
-- -----------------------------------------------------
DROP TABLE IF EXISTS `order_stage`;
CREATE TABLE `order_stage` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`    BIGINT       NOT NULL COMMENT '订单ID',
  `stage`       TINYINT      NOT NULL COMMENT '阶段：0 初稿、1 修改、2 终稿、3 交付',
  `submitter`   TINYINT      NOT NULL COMMENT '提交方：1 画师、2 企划方',
  `content`     TEXT         DEFAULT NULL COMMENT '提交内容 / 修改意见',
  `attachments` TEXT         DEFAULT NULL COMMENT '附件地址，逗号分隔',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 待确认、1 已通过、2 需修改',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE = InnoDB COMMENT = '订单阶段表';

-- -----------------------------------------------------
-- 消息表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `from_user_id` BIGINT      DEFAULT NULL COMMENT '发送方用户ID（系统通知为 NULL）',
  `to_user_id`  BIGINT       NOT NULL COMMENT '接收方用户ID',
  `type`        TINYINT      NOT NULL DEFAULT 0 COMMENT '类型：0 系统、1 应征、2 订单、3 私信',
  `content`     VARCHAR(500) NOT NULL COMMENT '消息内容',
  `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0 未读、1 已读',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_to_user` (`to_user_id`, `is_read`)
) ENGINE = InnoDB COMMENT = '消息表';

-- -----------------------------------------------------
-- 评价表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `evaluation`;
CREATE TABLE `evaluation` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`    BIGINT       NOT NULL COMMENT '订单ID',
  `from_user_id` BIGINT      NOT NULL COMMENT '评价方用户ID',
  `to_user_id`  BIGINT       NOT NULL COMMENT '被评价方用户ID',
  `score`       TINYINT      NOT NULL DEFAULT 5 COMMENT '评分：1-5',
  `content`     VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_from` (`order_id`, `from_user_id`)
) ENGINE = InnoDB COMMENT = '评价表';

-- =====================================================
-- 演示数据
-- 说明：BCrypt 密码均为 123456
-- =====================================================

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`, `intro`) VALUES
  (1, 'planner', '$2a$10$RvkldycPzn6PoC2cFW/.GOWrpFtZL.RyuLaYdPZKrFEIv1GiJAY1e', '企划小张', 1, 0, '资深品牌企划'),
  (2, 'artist',  '$2a$10$RvkldycPzn6PoC2cFW/.GOWrpFtZL.RyuLaYdPZKrFEIv1GiJAY1e', '画师阿绘', 2, 0, '国潮插画师，擅长人物与场景'),
  (3, 'admin',   '$2a$10$RvkldycPzn6PoC2cFW/.GOWrpFtZL.RyuLaYdPZKrFEIv1GiJAY1e', '平台管理员', 3, 0, NULL);

INSERT INTO `project` (`id`, `planner_id`, `title`, `description`, `budget`, `deadline`, `tags`, `status`) VALUES
  (1, 1, '国潮风节日海报插画', '需要一张国潮风格的节日海报插画，用于品牌宣传，希望风格大气、细节丰富。', 1500.00, '2026-09-01', '国潮,插画,海报', 0),
  (2, 1, '角色立绘约稿', '游戏角色立绘，半身像，需要分层 PSD 源文件。', 800.00, '2026-08-25', '立绘,游戏', 0);

INSERT INTO `application` (`id`, `project_id`, `artist_id`, `quote`, `description`, `status`) VALUES
  (1, 1, 2, 1200.00, '擅长国潮插画，可提供初稿预览，两周内交付。', 0);

INSERT INTO `order_info` (`id`, `project_id`, `planner_id`, `artist_id`, `amount`, `status`) VALUES
  (1, 1, 1, 2, 1200.00, 1);

INSERT INTO `order_stage` (`id`, `order_id`, `stage`, `submitter`, `content`, `status`) VALUES
  (1, 1, 0, 1, '初稿：整体构图与配色预览', 0);

INSERT INTO `message` (`id`, `from_user_id`, `to_user_id`, `type`, `content`, `is_read`) VALUES
  (1, NULL, 1, 0, '欢迎使用绘约，完善资料更容易接到订单哦！', 0),
  (2, 2, 1, 1, '画师「阿绘」应征了您的企划「国潮风节日海报插画」。', 0);

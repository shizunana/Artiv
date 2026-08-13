-- =====================================================
-- 绘约（HuiYue）数据库初始化脚本（v2.0）
-- 依据《开发计划.md》重新校准：10 张核心表
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
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
  `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
  `nickname`    VARCHAR(50)  NOT NULL COMMENT '昵称',
  `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  `balance`     DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '账户可用余额',
  `bio`         VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
  `tags`        VARCHAR(255) DEFAULT NULL COMMENT '风格标签，逗号分隔',
  `role`        TINYINT      NOT NULL DEFAULT 2 COMMENT '角色：1 企划方、2 画师、3 管理员',
  `cert_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '画师认证：0 未认证、1 审核中、2 已认证、3 已驳回',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '账号状态：0 正常、1 封禁',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删、1 已删',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB COMMENT = '用户表';

-- -----------------------------------------------------
-- 作品表（作品集）
-- -----------------------------------------------------
DROP TABLE IF EXISTS `work`;
CREATE TABLE `work` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL COMMENT '画师用户ID',
  `title`       VARCHAR(100) NOT NULL COMMENT '作品标题',
  `category`    VARCHAR(50)  DEFAULT NULL COMMENT '分类（国潮/立绘/场景等）',
  `subject`     VARCHAR(500) DEFAULT NULL COMMENT '题材风格，英文逗号分隔',
  `image`       VARCHAR(255) DEFAULT NULL COMMENT '作品图地址',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '作品说明',
  `visibility`  TINYINT      NOT NULL DEFAULT 1 COMMENT '可见范围：0 仅自己可见、1 公开',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT = '作品表';

-- -----------------------------------------------------
-- 内容服务板表（橱窗）
-- -----------------------------------------------------
DROP TABLE IF EXISTS `stall`;
CREATE TABLE `stall` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `painter_id`  BIGINT        NOT NULL COMMENT '画师用户ID',
  `title`       VARCHAR(100)  NOT NULL COMMENT '服务名称',
  `category`    VARCHAR(50)   DEFAULT NULL COMMENT '服务类型',
  `cover`       VARCHAR(255)  DEFAULT NULL COMMENT '封面图',
  `images`      TEXT          DEFAULT NULL COMMENT '橱窗详情图，逗号分隔',
  `price`       DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格（元）',
  `stock`       INT           NOT NULL DEFAULT 0 COMMENT '库存',
  `deliver_time` VARCHAR(50)  DEFAULT NULL COMMENT '截稿时间/交付周期',
  `phases`      VARCHAR(500)  DEFAULT NULL COMMENT '创作阶段JSON',
  `params`      TEXT          DEFAULT NULL COMMENT '稿件参数JSON',
  `description` VARCHAR(500)  DEFAULT NULL COMMENT '交付说明',
  `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0 已下架、1 上架中',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_painter` (`painter_id`)
) ENGINE = InnoDB COMMENT = '内容服务板表';

-- -----------------------------------------------------
-- 企划表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employer_id`     BIGINT        NOT NULL COMMENT '企划方用户ID',
  `title`           VARCHAR(100)  NOT NULL COMMENT '标题',
  `description`     TEXT          NOT NULL COMMENT '需求描述',
  `budget`          DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '预算（元）',
  `deadline`        DATE          DEFAULT NULL COMMENT '截稿日期',
  `tags`            VARCHAR(255)  DEFAULT NULL COMMENT '标签，逗号分隔',
  `images`          TEXT          DEFAULT NULL COMMENT '参考图，逗号分隔',
  `requirements`    TEXT          DEFAULT NULL COMMENT '结构化项目要求 JSON',
  `mode`            TINYINT       NOT NULL DEFAULT 0 COMMENT '合作方式：0 公开招募、1 定向邀请',
  `recruit_deadline` DATETIME     DEFAULT NULL COMMENT '招募截止时间（默认发布后10日）',
  `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 招募中、1 已选定、2 进行中、3 已完成、4 已关闭',
  `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_employer` (`employer_id`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT = '企划表';

-- -----------------------------------------------------
-- 应征表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `application`;
CREATE TABLE `application` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`  BIGINT        NOT NULL COMMENT '企划ID',
  `painter_id`  BIGINT        NOT NULL COMMENT '画师用户ID',
  `intro`       VARCHAR(500)  DEFAULT NULL COMMENT '应征介绍',
  `work_link`   VARCHAR(500)  DEFAULT NULL COMMENT '作品链接',
  `quote`       DECIMAL(10,2) DEFAULT NULL COMMENT '报价（元）',
  `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 待处理、1 已通过、2 已拒绝、3 已撤销',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_painter` (`project_id`, `painter_id`),
  KEY `idx_painter` (`painter_id`)
) ENGINE = InnoDB COMMENT = '应征表';

-- -----------------------------------------------------
-- 定向邀请表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `invitation`;
CREATE TABLE `invitation` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`  BIGINT       NOT NULL COMMENT '企划ID',
  `painter_id`  BIGINT       NOT NULL COMMENT '被邀画师用户ID',
  `message`     VARCHAR(500) DEFAULT NULL COMMENT '邀请附言',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 待处理、1 已接受、2 已拒绝',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_painter` (`project_id`, `painter_id`),
  KEY `idx_painter` (`painter_id`)
) ENGINE = InnoDB COMMENT = '定向邀请表';

-- -----------------------------------------------------
-- 订单表（order 为保留字，库表名用 order_info）
-- -----------------------------------------------------
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`  BIGINT        DEFAULT NULL COMMENT '企划ID（企划下单时有值）',
  `stall_id`    BIGINT        DEFAULT NULL COMMENT '服务板ID（橱窗下单时有值）',
  `employer_id` BIGINT        NOT NULL COMMENT '企划方用户ID',
  `painter_id`  BIGINT        NOT NULL COMMENT '画师用户ID',
  `amount`      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '成交金额（元）',
  `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 待接单、1 进行中、2 待验收、3 已完成、4 已取消、5 争议中',
  `current_stage` TINYINT     NOT NULL DEFAULT 0 COMMENT '当前阶段：0 初稿、1 修改、2 终稿、3 交付',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_employer` (`employer_id`),
  KEY `idx_painter` (`painter_id`),
  KEY `idx_project` (`project_id`)
) ENGINE = InnoDB COMMENT = '订单表';

-- -----------------------------------------------------
-- 订单阶段表（分阶段验收）
-- -----------------------------------------------------
DROP TABLE IF EXISTS `order_stage`;
CREATE TABLE `order_stage` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`    BIGINT       NOT NULL COMMENT '订单ID',
  `name`        VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '阶段名称（初稿/修改/终稿/交付）',
  `percent`     TINYINT      NOT NULL DEFAULT 0 COMMENT '阶段占比（%）',
  `submitter`   TINYINT      NOT NULL DEFAULT 1 COMMENT '提交方：1 画师、2 企划方',
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
  `from_id`     BIGINT       DEFAULT NULL COMMENT '发送方用户ID（系统通知为 NULL）',
  `to_id`       BIGINT       NOT NULL COMMENT '接收方用户ID',
  `order_id`    BIGINT       DEFAULT NULL COMMENT '关联订单ID（可为空）',
  `project_id`  BIGINT       DEFAULT NULL COMMENT '关联企划ID（应征私聊可为空）',
  `type`        TINYINT      NOT NULL DEFAULT 0 COMMENT '类型：0 系统、1 应征、2 订单、3 私信',
  `subtype`     VARCHAR(50)  DEFAULT NULL COMMENT '消息子类型，如 PRIVATE_CHAT / APPLICATION_CREATED',
  `conversation_key` VARCHAR(50) DEFAULT NULL COMMENT '双方会话键，按较小用户ID_较大用户ID归一化',
  `starter_key` VARCHAR(100) DEFAULT NULL COMMENT '项目会话发起标记；普通消息为空',
  `content`     VARCHAR(500) NOT NULL COMMENT '消息内容',
  `read_at`     DATETIME     DEFAULT NULL COMMENT '已读时间（NULL 表示未读）',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_to` (`to_id`, `read_at`),
  KEY `idx_conversation` (`conversation_key`, `project_id`, `created_at`),
  KEY `idx_project` (`project_id`),
  UNIQUE KEY `uk_project_conversation_starter` (`starter_key`)
) ENGINE = InnoDB COMMENT = '消息表';

-- -----------------------------------------------------
-- 评价表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`    BIGINT       NOT NULL COMMENT '订单ID',
  `from_id`     BIGINT       NOT NULL COMMENT '评价方用户ID',
  `to_id`       BIGINT       NOT NULL COMMENT '被评价方用户ID',
  `score`       TINYINT      NOT NULL DEFAULT 5 COMMENT '评分：1-5',
  `content`     VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_from` (`order_id`, `from_id`)
) ENGINE = InnoDB COMMENT = '评价表';

-- =====================================================
-- 演示数据（BCrypt 密码均为 123456）
-- =====================================================

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `bio`, `tags`, `role`, `cert_status`, `status`) VALUES
  (1, 'planner', '$2a$10$RvkldycPzn6PoC2cFW/.GOWrpFtZL.RyuLaYdPZKrFEIv1GiJAY1e', '企划小张', NULL, '资深品牌企划，长期寻找合作画师。', '品牌,插画', 1, 0, 0),
  (2, 'artist',  '$2a$10$RvkldycPzn6PoC2cFW/.GOWrpFtZL.RyuLaYdPZKrFEIv1GiJAY1e', '画师阿绘', NULL, '国潮插画师，擅长人物与场景，接稿中。', '国潮,插画,人物', 2, 2, 0),
  (3, 'admin',   '$2a$10$RvkldycPzn6PoC2cFW/.GOWrpFtZL.RyuLaYdPZKrFEIv1GiJAY1e', '平台管理员', NULL, NULL, NULL, 3, 0, 0);

INSERT INTO `work` (`id`, `user_id`, `title`, `category`, `subject`, `description`, `visibility`) VALUES
  (1, 2, '国潮·龙腾四海', '平面设计', '古风,有背景', '节日主题国潮插画，曾用于品牌海报宣传。', 1),
  (2, 2, '少女与猫·立绘', '立绘', '女性,日系', '游戏角色立绘练习，半身像。', 1);

INSERT INTO `stall` (`id`, `painter_id`, `title`, `category`, `price`, `description`, `status`) VALUES
  (1, 2, '国潮插画约稿（单张）', '国潮插画', 1200.00, '含初稿、2 次修改、终稿源文件，交付周期 2 周。', 1),
  (2, 2, '角色立绘（半身）', '角色立绘', 800.00, '半身像立绘，含分层 PSD 源文件。', 1);

INSERT INTO `project` (`id`, `employer_id`, `title`, `description`, `budget`, `deadline`, `tags`, `mode`, `recruit_deadline`, `status`) VALUES
  (1, 1, '国潮风节日海报插画', '需要一张国潮风格的节日海报插画，用于品牌宣传，希望风格大气、细节丰富。', 1500.00, '2026-09-01', '国潮,插画,海报', 0, DATE_ADD(NOW(), INTERVAL 10 DAY), 0),
  (2, 1, '角色立绘约稿（定向）', '游戏角色立绘，半身像，需要分层 PSD 源文件，定向邀请阿绘。', 800.00, '2026-08-25', '立绘,游戏', 1, DATE_ADD(NOW(), INTERVAL 10 DAY), 0);

INSERT INTO `application` (`id`, `project_id`, `painter_id`, `intro`, `work_link`, `quote`, `status`) VALUES
  (1, 1, 2, '擅长国潮插画，可提供初稿预览，两周内交付。', '/works/1', 1200.00, 0);

INSERT INTO `invitation` (`id`, `project_id`, `painter_id`, `message`, `status`) VALUES
  (1, 2, 2, '老朋友了，这个立绘想请你来画，报价可谈。', 0);

INSERT INTO `order_info` (`id`, `project_id`, `stall_id`, `employer_id`, `painter_id`, `amount`, `status`, `current_stage`) VALUES
  (1, 1, NULL, 1, 2, 1200.00, 1, 0);

INSERT INTO `order_stage` (`id`, `order_id`, `name`, `percent`, `submitter`, `content`, `status`) VALUES
  (1, 1, '初稿', 40, 1, '初稿：整体构图与配色预览', 0);

INSERT INTO `message` (`id`, `from_id`, `to_id`, `type`, `content`, `read_at`) VALUES
  (1, NULL, 1, 0, '欢迎使用绘约，完善资料更容易接到订单哦！', NULL),
  (2, 2, 1, 1, '画师「阿绘」应征了您的企划「国潮风节日海报插画」。', NULL);

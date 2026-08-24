-- =====================================================
-- 艺绘（Artiv）数据库初始化脚本（v2.0）
-- 依据当前运行数据库重新校准
-- MySQL 8.0.41 ｜ 字符集 utf8mb4 ｜ 引擎 InnoDB
-- 执行方式：mysql -u root -p < init.sql 或在 Workbench 中执行
-- =====================================================

CREATE DATABASE IF NOT EXISTS `artiv`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `artiv`;

-- 管理后台扩展表先删除，避免初始化时残留旧结构。
DROP TABLE IF EXISTS `order_dispute_evidence`;
DROP TABLE IF EXISTS `order_dispute`;
DROP TABLE IF EXISTS `artist_certification`;
DROP TABLE IF EXISTS `admin_action_log`;

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
  `bio`         VARCHAR(500) DEFAULT NULL COMMENT '画师公开主页简介',
  `employer_bio` VARCHAR(500) DEFAULT NULL COMMENT '企划方公开主页简介',
  `tags`        VARCHAR(255) DEFAULT NULL COMMENT '风格标签，逗号分隔',
  `artist_name` VARCHAR(100) DEFAULT NULL COMMENT '已审核生效的画师笔名',
  `artist_site` VARCHAR(500) DEFAULT NULL COMMENT '已审核生效的画师常用站',
  `role`        TINYINT      NOT NULL DEFAULT 2 COMMENT '账户入口：1 默认企划方工作台、2 默认画师工作台、3 管理员',
  `cert_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '画师认证：0 未认证、1 审核中、2 已认证',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '账号状态：0 正常、1 封禁',
  `ban_reason`  VARCHAR(500) DEFAULT NULL COMMENT '最近一次封禁原因',
  `banned_by`   BIGINT       DEFAULT NULL COMMENT '执行封禁的管理员ID',
  `banned_at`   DATETIME     DEFAULT NULL COMMENT '封禁时间',
  `banned_until` DATETIME    DEFAULT NULL COMMENT '限时封禁截止时间；NULL为永久封禁',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删、1 已删',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_nickname` (`nickname`),
  KEY `idx_user_status_ban` (`status`, `banned_until`, `deleted`),
  KEY `idx_user_created` (`created_at`, `deleted`),
  KEY `idx_user_cert` (`cert_status`, `deleted`)
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
-- 关注与收藏关系表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `user_follow`;
CREATE TABLE `user_follow` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT   NOT NULL COMMENT '关注人用户ID',
  `painter_id` BIGINT   NOT NULL COMMENT '被关注的认证画师用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`user_id`, `painter_id`),
  KEY `idx_follow_painter` (`painter_id`, `created_at`),
  KEY `idx_follow_user_created` (`user_id`, `created_at`)
) ENGINE = InnoDB COMMENT = '画师关注关系表';

DROP TABLE IF EXISTS `work_favorite`;
CREATE TABLE `work_favorite` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT   NOT NULL COMMENT '收藏人用户ID',
  `work_id`    BIGINT   NOT NULL COMMENT '作品ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_favorite` (`user_id`, `work_id`),
  KEY `idx_work_favorite_target` (`work_id`, `created_at`),
  KEY `idx_work_favorite_user_created` (`user_id`, `created_at`)
) ENGINE = InnoDB COMMENT = '作品收藏关系表';

DROP TABLE IF EXISTS `stall_favorite`;
CREATE TABLE `stall_favorite` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT   NOT NULL COMMENT '收藏人用户ID',
  `stall_id`   BIGINT   NOT NULL COMMENT '橱窗ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stall_favorite` (`user_id`, `stall_id`),
  KEY `idx_stall_favorite_target` (`stall_id`, `created_at`),
  KEY `idx_stall_favorite_user_created` (`user_id`, `created_at`)
) ENGINE = InnoDB COMMENT = '橱窗收藏关系表';

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
  `budget_min`      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '预算区间下限（元）',
  `budget_max`      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '预算区间上限（元）',
  `deadline`        DATE          DEFAULT NULL COMMENT '截稿日期',
  `tags`            VARCHAR(255)  DEFAULT NULL COMMENT '标签，逗号分隔',
  `category`        VARCHAR(50)   DEFAULT NULL COMMENT '稿件类型（与橱窗分类一致）',
  `subject`         VARCHAR(255)  DEFAULT NULL COMMENT '题材风格，逗号分隔',
  `images`          TEXT          DEFAULT NULL COMMENT '参考图，逗号分隔',
  `requirements`    TEXT          DEFAULT NULL COMMENT '结构化项目要求 JSON',
  `phases`          VARCHAR(1000) DEFAULT NULL COMMENT '创作阶段 JSON',
  `mode`            TINYINT       NOT NULL DEFAULT 0 COMMENT '合作方式：0 公开招募、1 定向邀请',
  `recruit_deadline` DATETIME     DEFAULT NULL COMMENT '招募截止时间（默认发布后10日）',
  `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 招募中、1 已选定、2 进行中、3 已完成、4 已关闭',
  `moderation_status` TINYINT     NOT NULL DEFAULT 0 COMMENT '审核状态：0正常、1违规下架',
  `moderation_reason` VARCHAR(500) DEFAULT NULL COMMENT '下架或恢复原因',
  `moderated_by`     BIGINT       DEFAULT NULL COMMENT '最近操作管理员ID',
  `moderated_at`     DATETIME     DEFAULT NULL COMMENT '最近审核操作时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_employer` (`employer_id`),
  KEY `idx_status` (`status`),
  KEY `idx_project_public` (`moderation_status`, `status`, `deleted`),
  KEY `idx_project_created` (`created_at`, `deleted`)
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
  KEY `idx_painter` (`painter_id`),
  KEY `idx_application_project_created` (`project_id`, `created_at`, `deleted`)
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
  `application_id` BIGINT    DEFAULT NULL COMMENT '接受邀请后创建或恢复的应征ID',
  `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '婉拒理由',
  `responded_at` DATETIME    DEFAULT NULL COMMENT '处理时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invitation_project` (`project_id`),
  UNIQUE KEY `uk_project_painter` (`project_id`, `painter_id`),
  KEY `idx_painter` (`painter_id`),
  KEY `idx_invitation_application` (`application_id`)
) ENGINE = InnoDB COMMENT = '定向邀请表';

-- -----------------------------------------------------
-- 订单表（order 为保留字，库表名用 order_info）
-- -----------------------------------------------------
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`  BIGINT        DEFAULT NULL COMMENT '企划ID（企划下单时有值）',
  `application_id` BIGINT     DEFAULT NULL COMMENT '企划订单对应的应征记录ID',
  `stall_id`    BIGINT        DEFAULT NULL COMMENT '服务板ID（橱窗下单时有值）',
  `employer_id` BIGINT        NOT NULL COMMENT '企划方用户ID',
  `painter_id`  BIGINT        NOT NULL COMMENT '画师用户ID',
  `amount`      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '成交金额（元）',
  `stall_title` VARCHAR(100) DEFAULT NULL COMMENT '橱窗标题快照',
  `stall_cover` VARCHAR(255) DEFAULT NULL COMMENT '橱窗封面快照',
  `project_title` VARCHAR(200) DEFAULT NULL COMMENT '企划标题快照',
  `project_cover` VARCHAR(500) DEFAULT NULL COMMENT '企划封面快照',
  `project_snapshot` TEXT DEFAULT NULL COMMENT '企划与应征信息 JSON 快照',
  `phase_snapshot` TEXT DEFAULT NULL COMMENT '创作阶段 JSON 快照',
  `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 待接单、1 进行中、2 待验收、3 已完成、4 已取消、5 争议中',
  `current_stage` TINYINT     NOT NULL DEFAULT 0 COMMENT '当前创作阶段序号，从 0 开始',
  `cancel_reason` VARCHAR(500) DEFAULT NULL COMMENT '取消/拒单原因',
  `after_sale_reason` VARCHAR(500) DEFAULT NULL COMMENT '售后申请原因',
  `accepted_at` DATETIME DEFAULT NULL COMMENT '接单时间',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成结算时间',
  `cancelled_at` DATETIME DEFAULT NULL COMMENT '取消时间',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_employer` (`employer_id`),
  KEY `idx_painter` (`painter_id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_application_status` (`application_id`, `status`, `deleted`),
  KEY `idx_order_status_created` (`status`, `created_at`, `deleted`),
  KEY `idx_order_completed` (`completed_at`, `deleted`)
) ENGINE = InnoDB COMMENT = '订单表';

-- -----------------------------------------------------
-- 订单阶段表（分阶段验收）
-- -----------------------------------------------------
DROP TABLE IF EXISTS `order_stage`;
CREATE TABLE `order_stage` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`    BIGINT       NOT NULL COMMENT '订单ID',
  `name`        VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '阶段名称（如初稿/修改/终稿）',
  `stage_index` TINYINT      NOT NULL DEFAULT 0 COMMENT '阶段序号',
  `submission_no` INT        NOT NULL DEFAULT 1 COMMENT '该阶段提交版本',
  `percent`     TINYINT      NOT NULL DEFAULT 0 COMMENT '历史兼容字段；新创作阶段不再使用占比',
  `submitter`   TINYINT      NOT NULL DEFAULT 1 COMMENT '提交方：1 画师、2 企划方',
  `content`     TEXT         DEFAULT NULL COMMENT '提交内容 / 修改意见',
  `attachments` TEXT         DEFAULT NULL COMMENT '附件地址，逗号分隔',
  `originals`   TEXT         DEFAULT NULL COMMENT '高清原图地址，逗号分隔',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 待确认、1 已通过、2 需修改',
  `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '验收意见',
  `reviewed_at` DATETIME DEFAULT NULL COMMENT '验收时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_order_stage_version` (`order_id`, `stage_index`, `submission_no`)
) ENGINE = InnoDB COMMENT = '订单阶段表';

DROP TABLE IF EXISTS `order_event`;
CREATE TABLE `order_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `actor_id` BIGINT DEFAULT NULL,
  `event_type` VARCHAR(50) NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_event` (`order_id`, `created_at`)
) ENGINE = InnoDB COMMENT = '订单操作时间线';

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
  UNIQUE KEY `uk_order_from` (`order_id`, `from_id`),
  KEY `idx_review_recipient_created` (`to_id`, `deleted`, `created_at`),
  KEY `idx_review_order_deleted` (`order_id`, `deleted`)
) ENGINE = InnoDB COMMENT = '评价表';

DROP TABLE IF EXISTS `review_image`;
CREATE TABLE `review_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `review_id` BIGINT NOT NULL COMMENT '评价ID',
  `image_url` VARCHAR(255) NOT NULL COMMENT '本站上传图片地址',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_review_image_review` (`review_id`, `sort_order`, `id`)
) ENGINE=InnoDB COMMENT='评价图片';

DROP TABLE IF EXISTS `review_appeal`;
CREATE TABLE `review_appeal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `review_id` BIGINT NOT NULL COMMENT '被申诉评价ID',
  `applicant_id` BIGINT NOT NULL COMMENT '申诉画师ID',
  `reason` VARCHAR(500) NOT NULL COMMENT '申诉原因',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理、1批准删除、2驳回',
  `admin_id` BIGINT DEFAULT NULL COMMENT '处理管理员ID',
  `decision_reason` VARCHAR(500) DEFAULT NULL COMMENT '处理说明',
  `decided_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_appeal_review` (`review_id`),
  KEY `idx_review_appeal_status_created` (`status`, `created_at`),
  KEY `idx_review_appeal_applicant` (`applicant_id`, `created_at`)
) ENGINE=InnoDB COMMENT='评价删除申诉';

-- -----------------------------------------------------
-- 画师认证申请
-- -----------------------------------------------------
CREATE TABLE `artist_certification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '申请画师ID',
  `application_type` TINYINT NOT NULL DEFAULT 0 COMMENT '申请类型：0初次认证、1认证资料修改',
  `common_id` VARCHAR(100) NOT NULL COMMENT '申请笔名',
  `common_site` VARCHAR(500) NOT NULL COMMENT '申请常用站个人主页',
  `previous_common_id` VARCHAR(100) DEFAULT NULL COMMENT '提交修改时已生效笔名快照',
  `previous_common_site` VARCHAR(500) DEFAULT NULL COMMENT '提交修改时已生效常用站快照',
  `public_work_count` INT NOT NULL DEFAULT 0 COMMENT '提交时公开作品数量快照',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核、1通过、2驳回',
  `reviewer_id` BIGINT DEFAULT NULL,
  `review_reason` VARCHAR(500) DEFAULT NULL,
  `reviewed_at` DATETIME DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cert_user_created` (`user_id`, `created_at`),
  KEY `idx_cert_user_type_status` (`user_id`, `application_type`, `status`, `deleted`, `created_at`),
  KEY `idx_cert_status_created` (`status`, `created_at`, `deleted`)
) ENGINE=InnoDB COMMENT='画师认证申请';

-- -----------------------------------------------------
-- 订单争议与双方图片证据
-- -----------------------------------------------------
CREATE TABLE `order_dispute` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `initiator_id` BIGINT NOT NULL,
  `reason` VARCHAR(500) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待仲裁、1已处理',
  `result` TINYINT DEFAULT NULL COMMENT '1全额退款企划方、2全额结算画师、3预留部分退款',
  `resolution_reason` VARCHAR(500) DEFAULT NULL,
  `resolved_by` BIGINT DEFAULT NULL,
  `resolved_at` DATETIME DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dispute_order` (`order_id`),
  KEY `idx_dispute_status_created` (`status`, `created_at`, `deleted`)
) ENGINE=InnoDB COMMENT='订单争议';

CREATE TABLE `order_dispute_evidence` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `dispute_id` BIGINT NOT NULL,
  `submitted_by` BIGINT NOT NULL,
  `image` VARCHAR(255) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_evidence_dispute_user` (`dispute_id`, `submitted_by`, `created_at`)
) ENGINE=InnoDB COMMENT='订单争议图片证据';

-- -----------------------------------------------------
-- 管理员操作审计日志
-- -----------------------------------------------------
CREATE TABLE `admin_action_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `admin_id` BIGINT DEFAULT NULL COMMENT '系统任务为空',
  `action_type` VARCHAR(50) NOT NULL,
  `target_type` VARCHAR(50) NOT NULL,
  `target_id` BIGINT DEFAULT NULL,
  `before_data` TEXT DEFAULT NULL,
  `after_data` TEXT DEFAULT NULL,
  `reason` VARCHAR(500) DEFAULT NULL,
  `request_id` VARCHAR(64) DEFAULT NULL,
  `ip_address` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_log_created` (`created_at`),
  KEY `idx_admin_log_target` (`target_type`, `target_id`, `created_at`),
  KEY `idx_admin_log_admin` (`admin_id`, `created_at`)
) ENGINE=InnoDB COMMENT='管理员操作审计日志';

-- =====================================================
-- 演示数据（BCrypt 密码均为 123456）
-- =====================================================

INSERT INTO `user`
  (`id`, `username`, `password`, `nickname`, `avatar`, `bio`, `employer_bio`, `tags`, `artist_name`, `artist_site`, `role`, `cert_status`, `status`)
VALUES
  (1, 'planner', '$2a$10$RvkldycPzn6PoC2cFW/.GOWrpFtZL.RyuLaYdPZKrFEIv1GiJAY1e', '企划小张', NULL, NULL, '资深品牌企划，长期寻找合作画师。', NULL, NULL, NULL, 1, 0, 0),
  (2, 'artist',  '$2a$10$RvkldycPzn6PoC2cFW/.GOWrpFtZL.RyuLaYdPZKrFEIv1GiJAY1e', '画师阿绘', NULL, '国潮插画师，擅长人物与场景，接稿中。', NULL, '国潮,插画,人物', '画师阿绘', 'https://www.pixiv.net/users/2', 2, 2, 0),
  (3, 'admin',   '$2a$10$RvkldycPzn6PoC2cFW/.GOWrpFtZL.RyuLaYdPZKrFEIv1GiJAY1e', '平台管理员', NULL, NULL, NULL, NULL, NULL, NULL, 3, 0, 0);

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
  (1, NULL, 1, 0, '欢迎使用艺绘，完善资料更容易接到订单哦！', NULL),
  (2, 2, 1, 1, '画师「阿绘」应征了您的企划「国潮风节日海报插画」。', NULL);

USE `artiv`;

CREATE TABLE IF NOT EXISTS `user_follow` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT   NOT NULL COMMENT '关注人用户ID',
  `painter_id` BIGINT   NOT NULL COMMENT '被关注的认证画师用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`user_id`, `painter_id`),
  KEY `idx_follow_painter` (`painter_id`, `created_at`),
  KEY `idx_follow_user_created` (`user_id`, `created_at`)
) ENGINE = InnoDB COMMENT = '画师关注关系表';

CREATE TABLE IF NOT EXISTS `work_favorite` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT   NOT NULL COMMENT '收藏人用户ID',
  `work_id`    BIGINT   NOT NULL COMMENT '作品ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_favorite` (`user_id`, `work_id`),
  KEY `idx_work_favorite_target` (`work_id`, `created_at`),
  KEY `idx_work_favorite_user_created` (`user_id`, `created_at`)
) ENGINE = InnoDB COMMENT = '作品收藏关系表';

CREATE TABLE IF NOT EXISTS `stall_favorite` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT   NOT NULL COMMENT '收藏人用户ID',
  `stall_id`   BIGINT   NOT NULL COMMENT '橱窗ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stall_favorite` (`user_id`, `stall_id`),
  KEY `idx_stall_favorite_target` (`stall_id`, `created_at`),
  KEY `idx_stall_favorite_user_created` (`user_id`, `created_at`)
) ENGINE = InnoDB COMMENT = '橱窗收藏关系表';

-- P0：企划创作阶段与上下架管理
-- 适用于已存在的 artiv 数据库；历史企划 phases 为空时由应用层使用三阶段默认值。
ALTER TABLE `project`
  ADD COLUMN `phases` VARCHAR(1000) DEFAULT NULL COMMENT '创作阶段 JSON' AFTER `requirements`;


-- Add public/private visibility and subject filters to works.
-- Existing works remain public so historical demo data keeps appearing in the works square.
USE `artiv`;

ALTER TABLE `work`
  ADD COLUMN `subject` VARCHAR(500) DEFAULT NULL COMMENT '题材风格，英文逗号分隔' AFTER `category`,
  ADD COLUMN `visibility` TINYINT NOT NULL DEFAULT 1 COMMENT '可见范围：0 仅自己可见、1 公开' AFTER `description`;

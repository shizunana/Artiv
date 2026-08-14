-- Store structured project requirements for project detail views and creation.
USE `artiv`;

ALTER TABLE `project`
  ADD COLUMN `requirements` TEXT DEFAULT NULL COMMENT 'Structured project requirements JSON' AFTER `images`;

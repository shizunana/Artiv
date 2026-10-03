-- MySQL 8. Back up the database before running; never rerun init.sql on existing data.
-- Adding the NOT NULL DEFAULT column assigns personal to ALL existing projects,
-- including directed, closed and soft-deleted projects. Historical orders stay unchanged.
-- Repeat execution is a no-op, so later commercial selections are never overwritten.
SET @usage_column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project' AND COLUMN_NAME = 'usage_type');
SET @usage_ddl = IF(@usage_column_exists = 0,
  'ALTER TABLE project ADD COLUMN usage_type VARCHAR(16) NOT NULL DEFAULT ''personal'' COMMENT ''personal / commercial'' AFTER project_category',
  'SELECT ''usage_type already exists; no data overwritten'' AS migration_status');
PREPARE usage_stmt FROM @usage_ddl;
EXECUTE usage_stmt;
DEALLOCATE PREPARE usage_stmt;

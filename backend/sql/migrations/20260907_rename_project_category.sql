-- MySQL 8: run only through scripts/migrate-project-category.ps1 after backup/rehearsal.
-- Existing databases: never rerun init.sql. No data UPDATE / DROP COLUMN / new dictionary table.
ALTER TABLE project RENAME COLUMN category TO project_category;
ALTER TABLE project MODIFY COLUMN project_category VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '企划类型（六类）';

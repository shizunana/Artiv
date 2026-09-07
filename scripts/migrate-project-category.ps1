[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Database,
    [Parameter(Mandatory)][string]$BackupPath,
    [switch]$ConfirmMigration
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if (-not $ConfirmMigration) { throw 'Migration requires -ConfirmMigration after backup and isolated rehearsal.' }
if ($Database -notmatch '^artiv(?:_category_test_[A-Za-z0-9_]+)?$') { throw 'Only artiv or an explicit artiv_category_test_* database is supported.' }
foreach ($key in @('SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD','SPRING_PROFILES_ACTIVE','SPRING_CONFIG_LOCATION')) {
    if ([Environment]::GetEnvironmentVariable($key)) { throw "Resolve active override $key before using this local migration runner." }
}
. (Join-Path $PSScriptRoot 'database-baseline.ps1') -Action Status -SnapshotPath $BackupPath
if ($dataSource.Host -notin @('localhost','127.0.0.1') -or $dataSource.Database -ne 'artiv') { throw 'Unexpected source; review the migration for this deployment first.' }
Assert-SnapshotChecksum -Path $BackupPath
Assert-SnapshotTarget -Path $BackupPath -DatabaseName 'artiv'
if ($Database -eq 'artiv' -and (Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue)) { throw 'Stop the backend and all other writers before migration.' }
$mysqlExecutable = Find-MySqlTool 'mysql'
$mysqlArguments = @('--protocol=tcp', "--host=$($dataSource.Host)", "--port=$($dataSource.Port)", "--user=$($dataSource.Username)", '--default-character-set=utf8mb4', '--batch', '--skip-column-names', "--database=$Database")
function Query([string]$Sql) {
    Invoke-WithMySqlPassword -Executable $mysqlExecutable -Arguments ($mysqlArguments + "--execute=$Sql") -Password $dataSource.Password
}
$columns = @(Query "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='project' AND COLUMN_NAME IN ('category','project_category')")
if ($columns.Count -ne 1) { throw 'Expected exactly one old/new column; both or neither is unsafe.' }
$column = $columns[0]
$properties = Query "SELECT CONCAT(COLUMN_TYPE,'|',IS_NULLABLE,'|',IFNULL(COLUMN_DEFAULT,'NULL'),'|',COLLATION_NAME,'|',EXTRA) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='project' AND COLUMN_NAME='$column'"
if ($properties -ne 'varchar(50)|YES|NULL|utf8mb4_unicode_ci|') { throw 'Unexpected column attributes; stop rather than implicitly change them.' }
$dependencies = Query "SELECT (SELECT COUNT(*) FROM information_schema.VIEWS WHERE TABLE_SCHEMA=DATABASE()) + (SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE())"
if ([int]$dependencies -ne 0) { throw 'Views/triggers require manual dependency review.' }
if ($column -eq 'category') {
    $rows = Query 'SELECT (SELECT COUNT(*) FROM project)+(SELECT COUNT(*) FROM order_info)+(SELECT COUNT(*) FROM application)+(SELECT COUNT(*) FROM invitation)+(SELECT COUNT(*) FROM order_stage)+(SELECT COUNT(*) FROM order_event)+(SELECT COUNT(*) FROM order_dispute)+(SELECT COUNT(*) FROM order_dispute_evidence)'
    if ([long]$rows -ne 0) { throw 'Old business data exists; do not clear it. Review a data compatibility plan first.' }
}
$migrationPath = Join-Path $projectRoot 'backend/sql/migrations/20260907_rename_project_category.sql'
$statements = @(Get-Content -Encoding UTF8 -LiteralPath $migrationPath | Where-Object { $_ -match '^ALTER TABLE' })
if ($statements.Count -ne 2) { throw 'Unexpected migration script.' }
if ($column -eq 'category') { Query $statements[0] }
$comment = Query "SELECT COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='project' AND COLUMN_NAME='project_category'"
if ($comment -ne '企划类型（六类）') { Query $statements[1] }
$oldCount = Query "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='project' AND COLUMN_NAME='category'"
$newCount = Query "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='project' AND COLUMN_NAME='project_category' AND COLUMN_TYPE='varchar(50)' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND COLLATION_NAME='utf8mb4_unicode_ci' AND COLUMN_COMMENT='企划类型（六类）'"
if ($oldCount -ne '0' -or $newCount -ne '1') { throw 'Post-migration verification failed; inspect actual structure before retrying.' }
Write-Host "Verified project.project_category on $Database; other tables were not altered."

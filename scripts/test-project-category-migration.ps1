[CmdletBinding()]
param([Parameter(Mandatory)][string]$Database, [Parameter(Mandatory)][string]$BackupPath)
$ErrorActionPreference='Stop'
if ($Database -notmatch '^artiv_category_test_[A-Za-z0-9_]+$') { throw 'Only an isolated restored test database is allowed.' }
. (Join-Path $PSScriptRoot 'database-baseline.ps1') -Action Status -SnapshotPath $BackupPath
$mysqlExecutable=Find-MySqlTool 'mysql'
$mysqlArguments=@('--protocol=tcp',"--host=$($dataSource.Host)","--port=$($dataSource.Port)","--user=$($dataSource.Username)",'--default-character-set=utf8mb4','--batch','--skip-column-names',"--database=$Database")
function Sql([string]$sql) { Invoke-WithMySqlPassword -Executable $mysqlExecutable -Arguments ($mysqlArguments+"--execute=$sql") -Password $dataSource.Password }
function Migrate { & (Join-Path $PSScriptRoot 'migrate-project-category.ps1') -Database $Database -BackupPath $BackupPath -ConfirmMigration }
function ExpectBlocked([string]$pattern) {
    $blocked=$false
    try { Migrate } catch { if ($_.Exception.Message -notmatch $pattern) { throw }; $blocked=$true }
    if (-not $blocked) { throw "Expected migration to stop: $pattern" }
}
if ((Sql 'SELECT COUNT(*) FROM project') -ne '0' -or (Sql 'SELECT COUNT(*) FROM order_info') -ne '0') { throw 'Use an empty-business restored test database.' }
Migrate
Migrate
# Both columns: added scratch column contains no data and is removed only from the isolated database.
Sql 'ALTER TABLE project ADD COLUMN category VARCHAR(50) DEFAULT NULL'
ExpectBlocked 'exactly one'
Sql 'ALTER TABLE project DROP COLUMN category'
# Neither expected name: preserve the original column under a temporary name.
Sql 'ALTER TABLE project RENAME COLUMN project_category TO migration_test_category'
ExpectBlocked 'exactly one'
Sql 'ALTER TABLE project RENAME COLUMN migration_test_category TO project_category'
# Partial completion: new name with old comment; runner finishes only the comment.
Sql "ALTER TABLE project MODIFY project_category VARCHAR(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'old comment'"
Migrate
# Rollback rehearsal while no business data exists, then nonempty-data guard.
Sql 'ALTER TABLE project RENAME COLUMN project_category TO category'
$fixtureId=Sql 'INSERT INTO order_info (employer_id,painter_id) VALUES (1,2); SELECT LAST_INSERT_ID()'
if ($fixtureId -notmatch '^\d+$') { throw 'Unexpected test fixture id' }
try { ExpectBlocked 'Old business data' } finally { Sql "DELETE FROM order_info WHERE id=$fixtureId" }
Migrate
Write-Host 'PASS: repeat, both/missing columns, partial comment, reverse rename and nonempty-data guards.'

[CmdletBinding()]
param(
    [ValidateSet('Capture', 'Restore', 'Status')]
    [string]$Action = 'Status',

    [string]$SnapshotPath,

    [string]$ConfigPath,

    [switch]$ReplaceBaseline,

    [switch]$ConfirmRestore,

    [switch]$AllowRemote
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $SnapshotPath) {
    $SnapshotPath = Join-Path $projectRoot '.artiv-data\database-baseline.sql'
}
if (-not $ConfigPath) {
    $ConfigPath = Join-Path $projectRoot 'backend\src\main\resources\application.yml'
}

$SnapshotPath = [System.IO.Path]::GetFullPath($SnapshotPath)
$ConfigPath = [System.IO.Path]::GetFullPath($ConfigPath)

function Resolve-SpringValue {
    param([Parameter(Mandatory)][string]$Value)

    $resolved = $Value.Trim()
    if (($resolved.StartsWith('"') -and $resolved.EndsWith('"')) -or
        ($resolved.StartsWith("'") -and $resolved.EndsWith("'"))) {
        $resolved = $resolved.Substring(1, $resolved.Length - 2)
    }

    if ($resolved -match '^\$\{([^:}]+):(.*)\}$') {
        $environmentValue = [Environment]::GetEnvironmentVariable($Matches[1])
        if ($null -ne $environmentValue -and $environmentValue -ne '') {
            return $environmentValue
        }
        return $Matches[2]
    }

    if ($resolved -match '^\$\{([^}]+)\}$') {
        $environmentValue = [Environment]::GetEnvironmentVariable($Matches[1])
        if ($null -ne $environmentValue -and $environmentValue -ne '') {
            return $environmentValue
        }
        throw "环境变量 $($Matches[1]) 未设置，且配置中没有默认值。"
    }

    return $resolved
}

function Get-DataSourceConfig {
    param([Parameter(Mandatory)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "找不到数据库配置文件：$Path"
    }

    $settings = @{}
    $insideDataSource = $false
    $dataSourceIndent = -1

    foreach ($line in Get-Content -LiteralPath $Path) {
        if (-not $insideDataSource -and $line -match '^(\s*)datasource:\s*$') {
            $insideDataSource = $true
            $dataSourceIndent = $Matches[1].Length
            continue
        }

        if (-not $insideDataSource) {
            continue
        }

        if ($line.Trim() -eq '' -or $line.TrimStart().StartsWith('#')) {
            continue
        }

        $currentIndent = $line.Length - $line.TrimStart().Length
        if ($currentIndent -le $dataSourceIndent) {
            break
        }

        if ($line -match '^\s+(url|username|password):\s*(.*)$') {
            $settings[$Matches[1]] = Resolve-SpringValue $Matches[2]
        }
    }

    foreach ($required in @('url', 'username', 'password')) {
        if (-not $settings.ContainsKey($required)) {
            throw "数据库配置缺少 spring.datasource.$required。"
        }
    }

    $jdbcMatch = [regex]::Match($settings.url, '^jdbc:mysql://([^/:?]+)(?::(\d+))?/([^?]+)')
    if (-not $jdbcMatch.Success) {
        throw '仅支持 jdbc:mysql://host:port/database 格式的数据源。'
    }

    $databaseHost = $jdbcMatch.Groups[1].Value
    $databasePort = if ($jdbcMatch.Groups[2].Success) { [int]$jdbcMatch.Groups[2].Value } else { 3306 }
    $databaseName = [Uri]::UnescapeDataString($jdbcMatch.Groups[3].Value)
    if ($databaseName -notmatch '^[A-Za-z0-9_]+$') {
        throw "数据库名包含不受支持的字符：$databaseName"
    }
    if ($databaseName -in @('mysql', 'information_schema', 'performance_schema', 'sys')) {
        throw "拒绝对 MySQL 系统数据库执行基线操作：$databaseName"
    }

    return [pscustomobject]@{
        Host = $databaseHost
        Port = $databasePort
        Database = $databaseName
        Username = $settings.username
        Password = $settings.password
    }
}

function Find-MySqlTool {
    param([Parameter(Mandatory)][string]$Name)

    $command = Get-Command "$Name.exe" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $service = Get-CimInstance Win32_Service -Filter "Name='MySQL80'" -ErrorAction SilentlyContinue
    if ($service -and $service.PathName) {
        $serviceExecutable = if ($service.PathName -match '^"([^"]+)"') { $Matches[1] } else { ($service.PathName -split '\s+')[0] }
        $serviceTool = Join-Path (Split-Path -Parent $serviceExecutable) "$Name.exe"
        if (Test-Path -LiteralPath $serviceTool -PathType Leaf) {
            return $serviceTool
        }
    }

    $programFilesCandidates = Get-ChildItem -Path "$env:ProgramFiles\MySQL\MySQL Server *\bin\$Name.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending
    if ($programFilesCandidates) {
        return $programFilesCandidates[0].FullName
    }

    throw "找不到 $Name.exe。请安装 MySQL 8 客户端，或将其 bin 目录加入 PATH。"
}

function Invoke-WithMySqlPassword {
    param(
        [Parameter(Mandatory)][string]$Executable,
        [Parameter(Mandatory)][string[]]$Arguments,
        [Parameter(Mandatory)][string]$Password
    )

    $previousPassword = [Environment]::GetEnvironmentVariable('MYSQL_PWD', 'Process')
    try {
        [Environment]::SetEnvironmentVariable('MYSQL_PWD', $Password, 'Process')
        & $Executable @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "MySQL 工具执行失败，退出码：$LASTEXITCODE"
        }
    }
    finally {
        [Environment]::SetEnvironmentVariable('MYSQL_PWD', $previousPassword, 'Process')
    }
}

function Write-SnapshotChecksum {
    param([Parameter(Mandatory)][string]$Path)

    $hash = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    Set-Content -LiteralPath "$Path.sha256" -Value $hash -Encoding ascii
}

function Assert-SnapshotChecksum {
    param([Parameter(Mandatory)][string]$Path)

    $checksumPath = "$Path.sha256"
    if (-not (Test-Path -LiteralPath $checksumPath -PathType Leaf)) {
        throw "缺少快照校验文件：$checksumPath"
    }

    $expected = (Get-Content -LiteralPath $checksumPath -Raw).Trim().ToLowerInvariant()
    $actual = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($expected -ne $actual) {
        throw '数据库基线快照校验失败，文件可能损坏或被修改。'
    }
}

function Assert-SnapshotTarget {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$DatabaseName
    )

    $databaseMarker = '-- Current Database: `' + $DatabaseName + '`'
    $markerFound = $false
    foreach ($line in Get-Content -LiteralPath $Path -TotalCount 80) {
        if ($line -eq $databaseMarker) {
            $markerFound = $true
            break
        }
    }
    if (-not $markerFound) {
        throw "快照目标与当前配置不一致，未找到数据库标记：$databaseMarker"
    }
}

function Save-DatabaseSnapshot {
    param(
        [Parameter(Mandatory)]$DataSource,
        [Parameter(Mandatory)][string]$OutputPath,
        [switch]$Replace
    )

    $outputDirectory = Split-Path -Parent $OutputPath
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

    if ((Test-Path -LiteralPath $OutputPath) -and -not $Replace) {
        throw "基线快照已经存在：$OutputPath。若要替换，请显式添加 -ReplaceBaseline。"
    }

    if ((Test-Path -LiteralPath $OutputPath) -and $Replace) {
        $oldBaseline = Join-Path $outputDirectory ("baseline-before-replace-{0}.sql" -f (Get-Date -Format 'yyyyMMdd-HHmmss'))
        Copy-Item -LiteralPath $OutputPath -Destination $oldBaseline
        if (Test-Path -LiteralPath "$OutputPath.sha256") {
            Copy-Item -LiteralPath "$OutputPath.sha256" -Destination "$oldBaseline.sha256"
        }
        Write-Host "旧基线已保留：$oldBaseline"
    }

    $temporaryPath = Join-Path $outputDirectory ("database-baseline-{0}.tmp" -f [Guid]::NewGuid().ToString('N'))
    $dumpTool = Find-MySqlTool 'mysqldump'
    $arguments = @(
        '--protocol=tcp',
        "--host=$($DataSource.Host)",
        "--port=$($DataSource.Port)",
        "--user=$($DataSource.Username)",
        '--default-character-set=utf8mb4',
        '--single-transaction',
        '--routines',
        '--events',
        '--triggers',
        '--hex-blob',
        '--set-gtid-purged=OFF',
        '--column-statistics=0',
        '--add-drop-database',
        '--databases',
        $DataSource.Database,
        "--result-file=$temporaryPath"
    )

    try {
        Invoke-WithMySqlPassword -Executable $dumpTool -Arguments $arguments -Password $DataSource.Password
        if (-not (Test-Path -LiteralPath $temporaryPath -PathType Leaf) -or (Get-Item -LiteralPath $temporaryPath).Length -eq 0) {
            throw 'mysqldump 未生成有效快照。'
        }
        Move-Item -LiteralPath $temporaryPath -Destination $OutputPath -Force
        Write-SnapshotChecksum $OutputPath
    }
    finally {
        if (Test-Path -LiteralPath $temporaryPath) {
            Remove-Item -LiteralPath $temporaryPath -Force
        }
    }
}

$dataSource = Get-DataSourceConfig $ConfigPath
$isLocalHost = $dataSource.Host -in @('localhost', '127.0.0.1', '::1')

switch ($Action) {
    'Capture' {
        Save-DatabaseSnapshot -DataSource $dataSource -OutputPath $SnapshotPath -Replace:$ReplaceBaseline
        $snapshot = Get-Item -LiteralPath $SnapshotPath
        Write-Host "数据库基线已保存：$($snapshot.FullName)"
        Write-Host ("目标数据库：{0}@{1}:{2}，快照大小：{3:N2} MB" -f $dataSource.Database, $dataSource.Host, $dataSource.Port, ($snapshot.Length / 1MB))
    }

    'Restore' {
        if (-not $ConfirmRestore) {
            throw '还原会覆盖当前数据库。请确认目标后添加 -ConfirmRestore。'
        }
        if (-not $isLocalHost -and -not $AllowRemote) {
            throw '默认禁止覆盖远程数据库。如已核对目标，请显式添加 -AllowRemote。'
        }
        if (-not (Test-Path -LiteralPath $SnapshotPath -PathType Leaf)) {
            throw "找不到数据库基线快照：$SnapshotPath"
        }

        Assert-SnapshotChecksum $SnapshotPath
        Assert-SnapshotTarget -Path $SnapshotPath -DatabaseName $dataSource.Database
        $backupDirectory = Split-Path -Parent $SnapshotPath
        $preRestoreBackup = Join-Path $backupDirectory ("pre-restore-{0}.sql" -f (Get-Date -Format 'yyyyMMdd-HHmmss'))
        Save-DatabaseSnapshot -DataSource $dataSource -OutputPath $preRestoreBackup
        Write-Host "覆盖前备份已保存：$preRestoreBackup"

        $mysqlTool = Find-MySqlTool 'mysql'
        $sourcePath = $SnapshotPath.Replace('\', '/')
        $arguments = @(
            '--protocol=tcp',
            "--host=$($dataSource.Host)",
            "--port=$($dataSource.Port)",
            "--user=$($dataSource.Username)",
            '--default-character-set=utf8mb4',
            "--execute=source $sourcePath"
        )

        try {
            Invoke-WithMySqlPassword -Executable $mysqlTool -Arguments $arguments -Password $dataSource.Password
            Write-Host "数据库已还原到基线：$SnapshotPath"
        }
        catch {
            Write-Error "还原失败。覆盖前备份位于：$preRestoreBackup。原始错误：$($_.Exception.Message)"
        }
    }

    'Status' {
        Write-Host "目标数据库：$($dataSource.Database)@$($dataSource.Host):$($dataSource.Port)"
        Write-Host "基线路径：$SnapshotPath"
        if (Test-Path -LiteralPath $SnapshotPath -PathType Leaf) {
            Assert-SnapshotChecksum $SnapshotPath
            Assert-SnapshotTarget -Path $SnapshotPath -DatabaseName $dataSource.Database
            $snapshot = Get-Item -LiteralPath $SnapshotPath
            Write-Host ("状态：可用；创建/更新时间：{0:yyyy-MM-dd HH:mm:ss}；大小：{1:N2} MB" -f $snapshot.LastWriteTime, ($snapshot.Length / 1MB))
        }
        else {
            Write-Host '状态：尚未创建。请使用 -Action Capture 保存当前数据库。'
        }
    }
}

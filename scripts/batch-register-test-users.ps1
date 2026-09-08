[CmdletBinding()]
param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string[]]$InputPath,
    [switch]$Apply,
    [switch]$ResolveNicknameConflicts,
    [switch]$SyncTestProfile,
    [string]$DatabaseHost = "127.0.0.1",
    [int]$DatabasePort = 3306,
    [string]$DatabaseName = "artiv",
    [string]$DatabaseUser = "root",
    [string]$MySqlExe = "mysql"
)

$ErrorActionPreference = "Stop"

function Get-RequiredValue {
    param([object]$Row, [string]$Name, [int]$RowNumber, [string]$Source)
    $value = [string]$Row.$Name
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "$Source 第 $RowNumber 行缺少列值：$Name"
    }
    return $value.Trim()
}

function Convert-ToSqlString {
    param([string]$Value)
    return "'" + $Value.Replace("'", "''") + "'"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $repoRoot
if (-not $InputPath -or $InputPath.Count -eq 0) {
    $InputPath = @(
        (Join-Path $workspaceRoot "outputs/artiv-project-test-data/01_企划测试账户_100.csv"),
        (Join-Path $workspaceRoot "outputs/artiv-stall-test-data/01_橱窗测试账户_60.csv")
    )
}

$rows = [System.Collections.Generic.List[object]]::new()
foreach ($path in $InputPath) {
    $resolved = Resolve-Path -LiteralPath $path -ErrorAction Stop
    $sourceRows = @(Import-Csv -LiteralPath $resolved.Path -Encoding UTF8)
    for ($index = 0; $index -lt $sourceRows.Count; $index++) {
        $rowNumber = $index + 2
        $source = $resolved.Path
        $username = Get-RequiredValue $sourceRows[$index] "登录账号(username)" $rowNumber $source
        $password = Get-RequiredValue $sourceRows[$index] "明文密码(password)" $rowNumber $source
        $nickname = Get-RequiredValue $sourceRows[$index] "随机用户名/昵称(nickname)" $rowNumber $source
        $roleText = Get-RequiredValue $sourceRows[$index] "角色(role)" $rowNumber $source
        $balanceText = Get-RequiredValue $sourceRows[$index] "初始余额(balance)" $rowNumber $source
        $certText = Get-RequiredValue $sourceRows[$index] "认证状态(cert_status)" $rowNumber $source
        $statusText = Get-RequiredValue $sourceRows[$index] "账号状态(status)" $rowNumber $source
        $deletedText = Get-RequiredValue $sourceRows[$index] "逻辑删除(deleted)" $rowNumber $source

        $role = 0
        $certStatus = 0
        $status = 0
        $deleted = 0
        $balance = [decimal]0
        if ($username.Length -lt 3 -or $username.Length -gt 20) { throw "$source 第 $rowNumber 行用户名长度不合法" }
        if ($password.Length -lt 6 -or $password.Length -gt 32) { throw "$source 第 $rowNumber 行密码长度不合法" }
        if (-not [int]::TryParse($roleText, [ref]$role) -or $role -notin 1, 2) { throw "$source 第 $rowNumber 行角色不合法" }
        if (-not [decimal]::TryParse($balanceText, [Globalization.NumberStyles]::Number, [Globalization.CultureInfo]::InvariantCulture, [ref]$balance) -or $balance -lt 0) { throw "$source 第 $rowNumber 行余额不合法" }
        if (-not [int]::TryParse($certText, [ref]$certStatus) -or $certStatus -notin 0, 1, 2) { throw "$source 第 $rowNumber 行认证状态不合法" }
        if (-not [int]::TryParse($statusText, [ref]$status) -or $status -notin 0, 1, 2) { throw "$source 第 $rowNumber 行账号状态不合法" }
        if (-not [int]::TryParse($deletedText, [ref]$deleted) -or $deleted -notin 0, 1) { throw "$source 第 $rowNumber 行逻辑删除值不合法" }

        $rows.Add([pscustomobject]@{
            Source = $source
            Row = $rowNumber
            Username = $username
            Password = $password
            Nickname = $nickname
            Role = $role
            Balance = $balance
            CertStatus = $certStatus
            Status = $status
            Deleted = $deleted
        })
    }
}

$duplicateUsernames = @($rows | Group-Object Username | Where-Object Count -gt 1)
$duplicateNicknames = @($rows | Group-Object Nickname | Where-Object Count -gt 1)
if ($duplicateUsernames.Count -gt 0) { throw "输入文件存在重复账号：$($duplicateUsernames.Name -join '、')" }
if ($duplicateNicknames.Count -gt 0) { throw "输入文件存在重复昵称：$($duplicateNicknames.Name -join '、')" }

Write-Host "预检通过：$($rows.Count) 个测试账号（企划方 $(@($rows | Where-Object Role -eq 1).Count)，画师 $(@($rows | Where-Object Role -eq 2).Count)）。"
if (-not $Apply) {
    Write-Host "当前为预检模式，未发送注册请求。确认后使用 -Apply；需要补齐余额和认证状态时同时使用 -SyncTestProfile。"
    exit 0
}

$registerUri = $BaseUrl.TrimEnd('/') + "/api/v1/auth/register"
$loginUri = $BaseUrl.TrimEnd('/') + "/api/v1/auth/login"
$results = [System.Collections.Generic.List[object]]::new()
foreach ($row in $rows) {
    $originalNickname = $row.Nickname
    $body = @{
        username = $row.Username
        password = $row.Password
        nickname = $row.Nickname
        role = $row.Role
    } | ConvertTo-Json -Compress
    try {
        $response = Invoke-RestMethod -Uri $registerUri -Method Post -ContentType "application/json; charset=utf-8" -Body $body -TimeoutSec 20
        if ([int]$response.code -eq 200) {
            $outcome = "Registered"
            $message = "注册成功"
        } elseif ([int]$response.code -eq 1001) {
            $outcome = "Existing"
            $message = [string]$response.message
            if ($ResolveNicknameConflicts) {
                $loginBody = @{ username = $row.Username; password = $row.Password } | ConvertTo-Json -Compress
                $loginResponse = Invoke-RestMethod -Uri $loginUri -Method Post -ContentType "application/json; charset=utf-8" -Body $loginBody -TimeoutSec 20
                $actualNickname = [string]$loginResponse.data.user.nickname
                if ([int]$loginResponse.code -ne 200 -or
                    ($actualNickname -ne $originalNickname -and -not $actualNickname.StartsWith($originalNickname + "·测试"))) {
                    $outcome = "Failed"
                    $message = "账号已存在，但密码或昵称与本测试数据不匹配"
                } else {
                    $row.Nickname = $actualNickname
                }
            }
        } elseif ([int]$response.code -eq 1004 -and $ResolveNicknameConflicts) {
            $outcome = "Failed"
            $message = [string]$response.message
            for ($suffix = 1; $suffix -le 99; $suffix++) {
                $suffixText = if ($suffix -eq 1) { "·测试" } else { "·测试$suffix" }
                $candidate = $originalNickname + $suffixText
                $retryBody = @{
                    username = $row.Username
                    password = $row.Password
                    nickname = $candidate
                    role = $row.Role
                } | ConvertTo-Json -Compress
                $retryResponse = Invoke-RestMethod -Uri $registerUri -Method Post -ContentType "application/json; charset=utf-8" -Body $retryBody -TimeoutSec 20
                if ([int]$retryResponse.code -eq 200) {
                    $row.Nickname = $candidate
                    $outcome = "Registered"
                    $message = "昵称冲突，已使用：$candidate"
                    break
                }
                if ([int]$retryResponse.code -ne 1004) {
                    $message = "[$($retryResponse.code)] $($retryResponse.message)"
                    break
                }
            }
        } else {
            $outcome = "Failed"
            $message = "[$($response.code)] $($response.message)"
        }
    } catch {
        $outcome = "Failed"
        $message = $_.Exception.Message
    }
    $results.Add([pscustomobject]@{
        Username = $row.Username
        OriginalNickname = $originalNickname
        ActualNickname = $row.Nickname
        Outcome = $outcome
        Message = $message
    })
}

$failed = @($results | Where-Object Outcome -eq "Failed")
$registered = @($results | Where-Object Outcome -eq "Registered")
$existing = @($results | Where-Object Outcome -eq "Existing")

$reportDirectory = Join-Path $repoRoot ".artiv-data/batch-register"
New-Item -ItemType Directory -Force -Path $reportDirectory | Out-Null
$reportPath = Join-Path $reportDirectory ("result-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".csv")
$results | Export-Csv -LiteralPath $reportPath -Encoding utf8BOM -NoTypeInformation
Write-Host "注册结果：新增 $($registered.Count)，已存在 $($existing.Count)，失败 $($failed.Count)。报告：$reportPath"

if ($SyncTestProfile) {
    if ($failed.Count -gt 0) { throw "存在注册失败项，已停止补齐测试资料；请先查看结果报告。" }
    if ($DatabaseName -notmatch '^[A-Za-z0-9_]+$') { throw "数据库名称仅允许字母、数字和下划线" }
    if ([string]::IsNullOrWhiteSpace($env:ARTIV_TEST_DB_PASSWORD)) { throw "使用 -SyncTestProfile 前请设置环境变量 ARTIV_TEST_DB_PASSWORD" }
    $mysql = (Get-Command $MySqlExe -ErrorAction Stop).Source
    $valueSql = $rows | ForEach-Object {
        $balanceSql = $_.Balance.ToString("0.00", [Globalization.CultureInfo]::InvariantCulture)
        "($(Convert-ToSqlString $_.Username),$(Convert-ToSqlString $_.Nickname),$balanceSql,$($_.CertStatus),$($_.Status),$($_.Deleted))"
    }
    $sql = @"
START TRANSACTION;
CREATE TEMPORARY TABLE batch_test_user_profile (
  username VARCHAR(64) NOT NULL,
  nickname VARCHAR(100) NOT NULL,
  balance DECIMAL(12,2) NOT NULL,
  cert_status TINYINT NOT NULL,
  status TINYINT NOT NULL,
  deleted TINYINT NOT NULL,
  PRIMARY KEY (username)
);
INSERT INTO batch_test_user_profile (username,nickname,balance,cert_status,status,deleted) VALUES
$($valueSql -join ",`n");
UPDATE user AS u
JOIN batch_test_user_profile AS t ON t.username = u.username AND t.nickname = u.nickname
SET u.balance = t.balance,
    u.cert_status = t.cert_status,
    u.status = t.status,
    u.deleted = t.deleted,
    u.updated_at = NOW();
SELECT CONCAT('MATCHED=', COUNT(*)) FROM user AS u
JOIN batch_test_user_profile AS t ON t.username = u.username AND t.nickname = u.nickname;
COMMIT;
"@
    $previousPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $env:ARTIV_TEST_DB_PASSWORD
        $profileOutput = $sql | & $mysql --host=$DatabaseHost --port=$DatabasePort --user=$DatabaseUser --database=$DatabaseName --batch --skip-column-names
        if ($LASTEXITCODE -ne 0) { throw "MySQL 资料补齐失败，退出码 $LASTEXITCODE" }
    } finally {
        $env:MYSQL_PWD = $previousPassword
    }
    $matchedLine = @($profileOutput | Where-Object { $_ -like "MATCHED=*" }) | Select-Object -Last 1
    if ($matchedLine -ne "MATCHED=$($rows.Count)") { throw "资料补齐匹配数异常：$matchedLine，预期 MATCHED=$($rows.Count)" }
    Write-Host "测试资料补齐完成：$matchedLine（余额、认证状态、账号状态和逻辑删除状态）。"
}

if ($failed.Count -gt 0) {
    $failed | Format-Table Username, OriginalNickname, ActualNickname, Message -AutoSize
    exit 1
}

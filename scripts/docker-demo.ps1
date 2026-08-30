[CmdletBinding()]
param(
    [ValidateSet('Build', 'Start', 'Stop', 'Status', 'Reset', 'Export', 'Import')]
    [string]$Action = 'Start',
    [string]$BundlePath,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repoRoot 'compose.demo.yml'
$envFile = Join-Path $repoRoot '.env.demo'
$defaultBundle = Join-Path $repoRoot 'dist/docker/artiv-demo-images.tar'
if (-not $BundlePath) { $BundlePath = $defaultBundle }

function New-RandomSecret([int]$byteCount = 32) {
    $bytes = New-Object byte[] $byteCount
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return [Convert]::ToBase64String($bytes).Replace('+', '-').Replace('/', '_').TrimEnd('=')
}

function Ensure-DemoEnvironment {
    if (Test-Path -LiteralPath $envFile) { return }
    $content = @(
        'ARTIV_DEMO_BIND=127.0.0.1'
        'ARTIV_DEMO_PORT=8088'
        "ARTIV_DEMO_DB_PASSWORD=$(New-RandomSecret 24)"
        "ARTIV_DEMO_DB_ROOT_PASSWORD=$(New-RandomSecret 24)"
        "ARTIV_DEMO_JWT_SECRET=$(New-RandomSecret 48)"
    )
    $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($envFile, $content, $utf8WithoutBom)
    Write-Host "已生成本机演示配置：$envFile"
}

function Invoke-Compose([string[]]$Arguments) {
    & docker compose --env-file $envFile -f $composeFile -p artiv-demo @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose 执行失败，退出码：$LASTEXITCODE"
    }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw '未找到 docker 命令，请先安装并启动 Docker Desktop。'
}

Ensure-DemoEnvironment

switch ($Action) {
    'Build' {
        Invoke-Compose @('build')
    }
    'Start' {
        Invoke-Compose @('up', '-d', '--build', '--remove-orphans')
        Write-Host 'Artiv 演示环境已启动：http://localhost:8088'
    }
    'Stop' {
        Invoke-Compose @('down', '--remove-orphans')
    }
    'Status' {
        Invoke-Compose @('ps')
    }
    'Reset' {
        if (-not $Force) {
            $answer = Read-Host '这会删除 Artiv 演示数据库和演示上传文件，恢复为三个空白账号。输入 RESET 继续'
            if ($answer -ne 'RESET') {
                Write-Host '已取消。'
                exit 0
            }
        }
        Invoke-Compose @('down', '--volumes', '--remove-orphans')
        Invoke-Compose @('up', '-d', '--build', '--remove-orphans')
        Write-Host 'Artiv 演示环境已重置：http://localhost:8088'
    }
    'Export' {
        Invoke-Compose @('build')
        $bundleDirectory = Split-Path -Parent $BundlePath
        New-Item -ItemType Directory -Force -Path $bundleDirectory | Out-Null
        & docker save --output $BundlePath artiv-demo-db:latest artiv-demo-backend:latest artiv-demo-frontend:latest
        if ($LASTEXITCODE -ne 0) { throw '镜像导出失败。' }
        Write-Host "镜像包已导出：$BundlePath"
    }
    'Import' {
        if (-not (Test-Path -LiteralPath $BundlePath)) {
            throw "找不到镜像包：$BundlePath"
        }
        & docker load --input $BundlePath
        if ($LASTEXITCODE -ne 0) { throw '镜像导入失败。' }
        Invoke-Compose @('up', '-d', '--no-build', '--remove-orphans')
        Write-Host '镜像已导入并启动：http://localhost:8088'
    }
}

param(
    [string]$DataSourceUrl = "http://127.0.0.1:9428/select/logsql/query",
    [string]$CliPath = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($CliPath)) {
    $CliPath = Join-Path $PSScriptRoot "tools/vlogscli-prod.exe"
}

if (-not (Test-Path -LiteralPath $CliPath -PathType Leaf)) {
    $fallback = Join-Path $PSScriptRoot "tools/vlutils-extract/vlogscli-windows-amd64-prod.exe"
    if (Test-Path -LiteralPath $fallback -PathType Leaf) {
        $CliPath = $fallback
    }
}

if (-not (Test-Path -LiteralPath $CliPath -PathType Leaf)) {
    throw "未找到 vlogscli。请下载 VictoriaLogs vlutils 发布包，并将 vlogscli-prod.exe 放到 devops/tools/。"
}

& $CliPath "-datasource.url=$DataSourceUrl"
exit $LASTEXITCODE

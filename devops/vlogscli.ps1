param(
    [string]$CliPath = "$PSScriptRoot\tools\vlogscli-prod.exe",
    [string]$DatasourceUrl = "http://127.0.0.1:9428/select/logsql/query"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $CliPath)) {
    throw "未找到 vlogscli：$CliPath。请从 VictoriaLogs vlutils 发布包获取 vlogscli-prod.exe。"
}

$arguments = @("-datasource.url=$DatasourceUrl")
& $CliPath @arguments
exit $LASTEXITCODE

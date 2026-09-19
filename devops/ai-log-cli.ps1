param(
    [string]$GatewayUrl = "http://127.0.0.1:18080/api/v1/logs/search",
    [Parameter(Mandatory = $true)][string]$Token,
    [Parameter(Mandatory = $true)][string]$Service,
    [Parameter(Mandatory = $true)][string]$Environment,
    [string]$Since = "15m",
    [string]$Keyword,
    [string]$TraceId,
    [ValidateRange(1, 200)][int]$Limit = 100,
    [ValidateSet("json", "markdown")][string]$Format = "json"
)

$ErrorActionPreference = "Stop"

$request = [ordered]@{
    service = $Service
    environment = $Environment
    since = $Since
    limit = $Limit
}
if ($Keyword) { $request.keyword = $Keyword }
if ($TraceId) { $request.traceId = $TraceId }

$headers = @{ Authorization = "Bearer $Token" }
$response = Invoke-RestMethod -Method Post -Uri $GatewayUrl -Headers $headers `
    -ContentType "application/json" -Body ($request | ConvertTo-Json -Compress)

if ($Format -eq "json") {
    $response | ConvertTo-Json -Depth 20
    exit 0
}

Write-Output "# Log query"
Write-Output ""
Write-Output "- Request: $($response.requestId)"
Write-Output "- Took: $($response.tookMs) ms"
Write-Output "- Results: $($response.total)"
Write-Output ""
foreach ($log in $response.logs) {
    $time = $log._time
    $level = $log.severity_text
    $message = $log._msg
    Write-Output "- [$time] [$level] $message"
}

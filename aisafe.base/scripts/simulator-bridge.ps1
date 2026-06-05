param(
    [Parameter(Mandatory=$true, Position=0)]
    [string]$InputJson,
    [Parameter(Mandatory=$true, Position=1)]
    [string]$OutputReport
)

$SharedFolder = "C:\ARQCP\partilha\SCOMP\SPRINT3"
$TempDir = Join-Path -LiteralPath $SharedFolder -ChildPath "temp"
$LinuxShared = "/mnt/c/ARQCP/partilha/SCOMP/SPRINT3"

if (-not (Test-Path -LiteralPath $SharedFolder)) {
    Write-Error "Shared folder not found: $SharedFolder"
    exit 1
}

if (-not (Test-Path -LiteralPath $TempDir)) {
    New-Item -ItemType Directory -Path $TempDir -Force | Out-Null
}

$timestamp = [DateTime]::Now.ToString("yyyyMMdd_HHmmss_fff")
$ext = [System.IO.Path]::GetExtension($InputJson)
if ([string]::IsNullOrEmpty($ext)) { $ext = ".json" }
$localInput = Join-Path -LiteralPath $TempDir -ChildPath "sim_input_$timestamp$ext"
$localOutput = Join-Path -LiteralPath $TempDir -ChildPath "sim_output_$timestamp.txt"

Copy-Item -LiteralPath $InputJson -Destination $localInput -Force

$linuxInput = "$LinuxShared/temp/sim_input_${timestamp}${ext}"
$linuxOutput = "$LinuxShared/temp/sim_output_${timestamp}.txt"

$wslCmd = "cd $LinuxShared && ./simulation '$linuxInput' 0 1 '$linuxOutput'"
Write-Host "[BRIDGE] Running: wsl $wslCmd"

$proc = Start-Process -FilePath "wsl.exe" -ArgumentList @("bash", "-c", $wslCmd) -NoNewWindow -Wait -PassThru

if ($proc.ExitCode -ne 0) {
    Write-Error "[BRIDGE] Simulator failed with exit code $($proc.ExitCode)"
    Remove-Item -LiteralPath $localInput -Force -ErrorAction SilentlyContinue
    exit $proc.ExitCode
}

if (Test-Path -LiteralPath $localOutput) {
    Copy-Item -LiteralPath $localOutput -Destination $OutputReport -Force
    Write-Host "[BRIDGE] Report copied to $OutputReport"
    Remove-Item -LiteralPath $localOutput -Force -ErrorAction SilentlyContinue
} else {
    Write-Warning "[BRIDGE] Output not found at $localOutput — looking for report_*.txt..."
    $reportFiles = Get-ChildItem -LiteralPath $SharedFolder -Filter "report_*.txt" | Sort-Object LastWriteTime -Descending
    if ($reportFiles) {
        Copy-Item -LiteralPath $reportFiles[0].FullName -Destination $OutputReport -Force
        Write-Host "[BRIDGE] Report copied from $($reportFiles[0].Name) to $OutputReport"
        Remove-Item -LiteralPath $reportFiles[0].FullName -Force -ErrorAction SilentlyContinue
    } else {
        Write-Error "[BRIDGE] No report file found!"
        Remove-Item -LiteralPath $localInput -Force -ErrorAction SilentlyContinue
        exit 1
    }
}

Remove-Item -LiteralPath $localInput -Force -ErrorAction SilentlyContinue
exit 0

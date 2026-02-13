param(
    [string]$PackageName = "com.zhongxul.birthkeeper",
    [string]$Activity = ".MainActivity",
    [int]$Runs = 20,
    [int]$ForceStopDelayMs = 700,
    [int]$BetweenRunsDelayMs = 300,
    [string]$OutputCsv = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-NearestRankPercentile {
    param(
        [int[]]$Values,
        [double]$Percentile
    )

    if ($Values.Count -eq 0) {
        throw "Cannot compute percentile for empty samples."
    }

    $sorted = $Values | Sort-Object
    $rank = [Math]::Ceiling($sorted.Count * $Percentile)
    $index = [Math]::Max($rank - 1, 0)
    return [int]$sorted[$index]
}

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "adb not found. Please load environment first."
}

$deviceState = (adb get-state 2>$null).Trim()
if ($deviceState -ne "device") {
    throw "No online device. adb get-state=$deviceState"
}

$packageLine = adb shell pm list packages $PackageName
if (-not ($packageLine | Select-String -SimpleMatch "package:$PackageName")) {
    throw "App is not installed on device: $PackageName"
}

try {
    adb shell pm grant $PackageName android.permission.POST_NOTIFICATIONS | Out-Null
} catch {
}
try {
    adb shell appops set $PackageName POST_NOTIFICATION allow | Out-Null
} catch {
}

$component = "$PackageName/$Activity"
$samples = New-Object System.Collections.Generic.List[object]

for ($i = 1; $i -le $Runs; $i++) {
    adb shell am force-stop $PackageName | Out-Null
    Start-Sleep -Milliseconds $ForceStopDelayMs

    $output = adb shell am start -W $component
    $launchState = ""
    $activityName = ""
    $totalTimeMs = $null
    $waitTimeMs = $null

    foreach ($line in $output) {
        if ($line -match "LaunchState:\s+(\S+)") {
            $launchState = $Matches[1]
            continue
        }
        if ($line -match "Activity:\s+(\S+)") {
            $activityName = $Matches[1]
            continue
        }
        if ($line -match "TotalTime:\s+(\d+)") {
            $totalTimeMs = [int]$Matches[1]
            continue
        }
        if ($line -match "WaitTime:\s+(\d+)") {
            $waitTimeMs = [int]$Matches[1]
            continue
        }
    }

    if ($null -eq $totalTimeMs) {
        $joined = ($output -join " | ")
        throw "Round $i does not contain TotalTime. Raw output: $joined"
    }

    if ($null -eq $waitTimeMs) {
        $waitTimeMs = $totalTimeMs
    }

    $samples.Add([pscustomobject]@{
            Run        = $i
            LaunchState = $launchState
            Activity   = $activityName
            TotalTimeMs = $totalTimeMs
            WaitTimeMs = $waitTimeMs
        })

    Start-Sleep -Milliseconds $BetweenRunsDelayMs
}

$totalValues = @($samples | ForEach-Object { [int]$_.TotalTimeMs })
$waitValues = @($samples | ForEach-Object { [int]$_.WaitTimeMs })

$summary = [pscustomobject]@{
    Timestamp       = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    PackageName     = $PackageName
    Activity        = $Activity
    Runs            = $Runs
    TotalTimeAvgMs  = [Math]::Round((($totalValues | Measure-Object -Average).Average), 1)
    TotalTimeP95Ms  = (Get-NearestRankPercentile -Values $totalValues -Percentile 0.95)
    TotalTimeMinMs  = (($totalValues | Measure-Object -Minimum).Minimum)
    TotalTimeMaxMs  = (($totalValues | Measure-Object -Maximum).Maximum)
    WaitTimeAvgMs   = [Math]::Round((($waitValues | Measure-Object -Average).Average), 1)
    WaitTimeP95Ms   = (Get-NearestRankPercentile -Values $waitValues -Percentile 0.95)
    WaitTimeMinMs   = (($waitValues | Measure-Object -Minimum).Minimum)
    WaitTimeMaxMs   = (($waitValues | Measure-Object -Maximum).Maximum)
}

if (-not [string]::IsNullOrWhiteSpace($OutputCsv)) {
    $csvPath = Resolve-Path -LiteralPath (Split-Path -Path $OutputCsv -Parent) -ErrorAction SilentlyContinue
    if ($null -eq $csvPath) {
        New-Item -ItemType Directory -Path (Split-Path -Path $OutputCsv -Parent) -Force | Out-Null
    }
    $samples | Export-Csv -Path $OutputCsv -NoTypeInformation -Encoding UTF8
}

$samples | Format-Table -AutoSize
Write-Host ""
Write-Host "Summary:"
$summary | Format-List
Write-Host "SUMMARY_JSON_BEGIN"
$summary | ConvertTo-Json -Compress
Write-Host "SUMMARY_JSON_END"

if (-not [string]::IsNullOrWhiteSpace($OutputCsv)) {
    Write-Host "CSV exported: $OutputCsv"
}

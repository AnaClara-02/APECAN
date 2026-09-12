[CmdletBinding()]
param(
    [string]$JavaPath = "java",
    [string]$JarPath = "C:\APECAN\app\apecan.jar",
    [string]$ConfigFile = "C:\APECAN\config\application-prod.properties",
    [string]$ServiceName = "APECAN",
    [int]$RecoveryPort = 8090
)

$ErrorActionPreference = "Stop"
$service = if ([string]::IsNullOrWhiteSpace($ServiceName)) {
    $null
} else {
    Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
}
$serviceWasRunning = $service -and $service.Status -ne "Stopped"

try {
    if ($serviceWasRunning) {
        Stop-Service -Name $ServiceName -Force
        $service.WaitForStatus("Stopped", [TimeSpan]::FromSeconds(30))
    }

    $configUri = ([System.Uri](Resolve-Path -LiteralPath $ConfigFile).Path).AbsoluteUri
    $arguments = @(
        "-jar", (Resolve-Path -LiteralPath $JarPath).Path,
        "--spring.profiles.active=recovery",
        "--spring.config.additional-location=$configUri",
        "--server.port=$RecoveryPort"
    )
    $process = Start-Process -FilePath $JavaPath -ArgumentList $arguments -WindowStyle Hidden -PassThru

    $url = "http://127.0.0.1:$RecoveryPort/recuperacao"
    $available = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        Start-Sleep -Seconds 1
        try {
            Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2 | Out-Null
            $available = $true
            break
        }
        catch {
            if ($process.HasExited) {
                throw "O modo de recuperacao foi encerrado antes de iniciar."
            }
        }
    }
    if (-not $available) {
        throw "O modo de recuperacao nao respondeu no tempo esperado."
    }

    Start-Process $url
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        throw "O modo de recuperacao terminou com falha. Consulte os logs protegidos do servidor."
    }
}
finally {
    if ($serviceWasRunning) {
        Start-Service -Name $ServiceName
    }
}

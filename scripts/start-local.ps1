$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$maven = Join-Path $projectRoot '..\apache-maven-3.9.9\bin\mvn.cmd'
$backendPath = Join-Path $projectRoot 'backend'
$mockBankPath = Join-Path $projectRoot 'mock-bank-api'
$webPath = Join-Path $projectRoot 'legacy-web-dashboard'

if (-not (Test-Path -LiteralPath $maven)) {
    throw "Maven was not found at '$maven'. Set up Maven and update this script path."
}
if (-not (Test-Path -LiteralPath (Join-Path $webPath 'node_modules'))) {
    throw "Web dependencies are missing. Run 'npm install' in '$webPath' first."
}

$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtBytes = New-Object byte[] 32
$rng.GetBytes($jwtBytes)
$env:PAYSENTRY_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$env:PAYSENTRY_MOCK_BANK_KEY = [Guid]::NewGuid().ToString('N')
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:VITE_API_BASE_URL = 'http://127.0.0.1:8080/api'
$env:VITE_WEBSOCKET_URL = 'ws://127.0.0.1:8080/ws'

if ($env:JAVA_HOME) {
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
}

$backendCommand = "Set-Location -LiteralPath '$backendPath'; & '$maven' spring-boot:run"
$mockBankCommand = "Set-Location -LiteralPath '$mockBankPath'; & '$maven' spring-boot:run"
$webCommand = "Set-Location -LiteralPath '$webPath'; npm run dev"

Start-Process -FilePath 'powershell.exe' -ArgumentList @('-NoExit', '-Command', $backendCommand) -WorkingDirectory $projectRoot | Out-Null
Start-Process -FilePath 'powershell.exe' -ArgumentList @('-NoExit', '-Command', $mockBankCommand) -WorkingDirectory $projectRoot | Out-Null
Start-Process -FilePath 'powershell.exe' -ArgumentList @('-NoExit', '-Command', $webCommand) -WorkingDirectory $projectRoot | Out-Null

Write-Output 'Started PaySentry backend (8080), mock bank (8081), and React dashboard (5173).'
Write-Output 'Opening http://127.0.0.1:5173 after the services become ready...'

$backendReady = $false
$webReady = $false
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    if (-not $backendReady) {
        try {
            $health = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/health' -TimeoutSec 2
            $backendReady = $health.status -eq 'PaySentry backend is running'
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    if (-not $webReady) {
        try {
            $webResponse = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:5173' -TimeoutSec 2
            $webReady = $webResponse.StatusCode -eq 200
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    if ($backendReady -and $webReady) {
        break
    }
}

if (-not $backendReady) {
    throw 'Backend did not become ready. Check the backend PowerShell window.'
}
if (-not $webReady) {
    throw 'Web dashboard did not become ready. Check the Vite PowerShell window.'
}

Start-Process 'http://127.0.0.1:5173'

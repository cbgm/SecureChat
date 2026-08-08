[CmdletBinding()]
param(
    [string]$OutputDirectory = "dist-control-plane",

    [Parameter(Mandatory = $true)]
    [string]$ImagePrefix,

    [Parameter(Mandatory = $true)]
    [string]$ImageTag
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($ImagePrefix -notmatch '^[a-z0-9.-]+(?:/[a-z0-9._-]+)+$') {
    throw "ImagePrefix is not a valid lowercase container-image prefix."
}

if ($ImageTag -notmatch '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$') {
    throw "ImageTag is not a valid container-image tag."
}

$repositoryRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$outputPath = [System.IO.Path]::GetFullPath((Join-Path $repositoryRoot $OutputDirectory))
$stagingRoot = Join-Path ([System.IO.Path]::GetTempPath()) "securechat-control-plane-bundle"
$bundleRoot = Join-Path $stagingRoot "securechat-control-plane"
$controlPlaneRoot = Join-Path $repositoryRoot "server/control-plane"

Remove-Item -LiteralPath $stagingRoot -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $outputPath -Recurse -Force -ErrorAction SilentlyContinue

New-Item -ItemType Directory -Path $bundleRoot -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $bundleRoot "secrets") -Force | Out-Null
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null

$requiredFiles = @(
    "docker-compose.yml",
    "docker-compose.release.yml",
    "Caddyfile"
)

foreach ($relativePath in $requiredFiles) {
    $sourcePath = Join-Path $controlPlaneRoot $relativePath
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Missing control-plane bundle file: $sourcePath"
    }

    Copy-Item `
        -LiteralPath $sourcePath `
        -Destination (Join-Path $bundleRoot $relativePath) `
        -Force
}

$optionalFiles = @(
    "README.md"
)

foreach ($relativePath in $optionalFiles) {
    $sourcePath = Join-Path $controlPlaneRoot $relativePath
    if (Test-Path -LiteralPath $sourcePath -PathType Leaf) {
        Copy-Item `
            -LiteralPath $sourcePath `
            -Destination (Join-Path $bundleRoot $relativePath) `
            -Force
    }
}

$releaseEnvironment = @(
    "SECURECHAT_IMAGE_PREFIX=$ImagePrefix",
    "SECURECHAT_IMAGE_TAG=$ImageTag"
)

[System.IO.File]::WriteAllLines(
    (Join-Path $bundleRoot "release.env"),
    $releaseEnvironment,
    [System.Text.UTF8Encoding]::new($false)
)

$startCmd = @'
@echo off
setlocal
cd /d "%~dp0"
start "" powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "%~dp0Bootstrap-ControlPlane.ps1"
exit /b 0
'@

[System.IO.File]::WriteAllText(
    (Join-Path $bundleRoot "Start-SecureChatControlPlane.cmd"),
    $startCmd,
    [System.Text.UTF8Encoding]::new($false)
)

$bootstrap = @'
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$deploymentDirectory = $PSScriptRoot
$runtimeEnvironmentPath = Join-Path $deploymentDirectory ".env.runtime"
$releaseEnvironmentPath = Join-Path $deploymentDirectory "release.env"
$secretsDirectory = Join-Path $deploymentDirectory "secrets"
$composePath = Join-Path $deploymentDirectory "docker-compose.yml"
$releaseComposePath = Join-Path $deploymentDirectory "docker-compose.release.yml"
$logPath = Join-Path $deploymentDirectory "bootstrap-control-plane.log"
$controlPlanePort = 8390

$form = New-Object System.Windows.Forms.Form
$form.Text = "SecureChat Control Plane"
$form.Size = New-Object System.Drawing.Size(520, 180)
$form.StartPosition = "CenterScreen"
$form.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::FixedDialog
$form.MaximizeBox = $false
$form.TopMost = $true

$title = New-Object System.Windows.Forms.Label
$title.Location = New-Object System.Drawing.Point(24, 22)
$title.Size = New-Object System.Drawing.Size(465, 28)
$title.Font = New-Object System.Drawing.Font("Segoe UI", 12, [System.Drawing.FontStyle]::Bold)
$title.Text = "Starting SecureChat control plane"
$form.Controls.Add($title)

$status = New-Object System.Windows.Forms.Label
$status.Location = New-Object System.Drawing.Point(24, 58)
$status.Size = New-Object System.Drawing.Size(465, 32)
$status.Text = "Preparing..."
$form.Controls.Add($status)

$progress = New-Object System.Windows.Forms.ProgressBar
$progress.Location = New-Object System.Drawing.Point(24, 100)
$progress.Size = New-Object System.Drawing.Size(465, 20)
$progress.Style = [System.Windows.Forms.ProgressBarStyle]::Marquee
$form.Controls.Add($progress)

$form.Show()
[System.Windows.Forms.Application]::DoEvents()

function Set-Status {
    param([string]$Message)
    $status.Text = $Message
    Add-Content -LiteralPath $logPath -Value "[$(Get-Date -Format o)] $Message"
    [System.Windows.Forms.Application]::DoEvents()
}

function Fail {
    param([string]$Message)
    Add-Content -LiteralPath $logPath -Value "[$(Get-Date -Format o)] FAILED: $Message"
    [System.Windows.Forms.MessageBox]::Show(
        "$Message`n`nLog:`n$logPath",
        "SecureChat Control Plane",
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Error
    ) | Out-Null
    $form.Close()
    exit 1
}

function Read-EnvironmentFile {
    param([string]$Path)

    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")
        if ($separator -gt 0) {
            $values[$trimmed.Substring(0, $separator).Trim()] =
                $trimmed.Substring($separator + 1).Trim()
        }
    }
    return $values
}

function New-Secret {
    $bytes = New-Object byte[] 48
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    return [Convert]::ToBase64String($bytes)
}

function Ensure-Secret {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        [System.IO.File]::WriteAllText(
            $Path,
            (New-Secret),
            [System.Text.UTF8Encoding]::new($false)
        )
    }
}

function Find-Docker {
    $command = Get-Command docker -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    $candidate = Join-Path $env:ProgramFiles "Docker\Docker\resources\bin\docker.exe"
    if (Test-Path -LiteralPath $candidate) {
        return $candidate
    }

    throw "Docker Desktop is not installed."
}

function Ensure-Docker {
    $docker = Find-Docker

    & $docker info *> $null
    if ($LASTEXITCODE -eq 0) {
        return $docker
    }

    $desktop = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"
    if (-not (Test-Path -LiteralPath $desktop)) {
        throw "Docker Desktop could not be found."
    }

    Start-Process -FilePath $desktop | Out-Null

    $deadline = [DateTime]::UtcNow.AddMinutes(5)
    while ([DateTime]::UtcNow -lt $deadline) {
        Start-Sleep -Seconds 2
        & $docker info *> $null
        if ($LASTEXITCODE -eq 0) {
            return $docker
        }
    }

    throw "Docker Desktop did not become ready."
}

function Find-FirebaseCredentials {
    $candidate = Join-Path $secretsDirectory "firebase-admin.json"
    if (Test-Path -LiteralPath $candidate -PathType Leaf) {
        return $candidate
    }

    $downloads = Join-Path $env:USERPROFILE "Downloads"
    if (Test-Path -LiteralPath $downloads) {
        $matches = @(
            Get-ChildItem -LiteralPath $downloads -File -Filter "*.json" -ErrorAction SilentlyContinue |
                Where-Object {
                    $_.Name -match "(?i)firebase|service.?account"
                }
        )

        if ($matches.Count -eq 1) {
            Copy-Item -LiteralPath $matches[0].FullName -Destination $candidate -Force
            return $candidate
        }
    }

    throw "Firebase Admin credentials were not found. Place firebase-admin.json in the bundle's secrets folder and start again."
}

try {
    Set-Content -LiteralPath $logPath -Value "" -Encoding UTF8

    Set-Status "Starting Docker Desktop..."
    $docker = Ensure-Docker

    Set-Status "Preparing SecureChat secrets..."
    New-Item -ItemType Directory -Path $secretsDirectory -Force | Out-Null

    $registryPassword = Join-Path $secretsDirectory "node-registry-database-password.txt"
    $presencePassword = Join-Path $secretsDirectory "presence-redis-password.txt"
    $pushPassword = Join-Path $secretsDirectory "push-database-password.txt"
    $pushToken = Join-Path $secretsDirectory "push-internal-api-token.txt"

    Ensure-Secret $registryPassword
    Ensure-Secret $presencePassword
    Ensure-Secret $pushPassword
    Ensure-Secret $pushToken

    $firebaseCredentials = Find-FirebaseCredentials
    $release = Read-EnvironmentFile $releaseEnvironmentPath

    $runtime = @(
        "CONTROL_PLANE_PROJECT_NAME=securechat-control-plane",
        "CONTROL_PLANE_BIND_ADDRESS=0.0.0.0",
        "CONTROL_PLANE_HTTP_PORT=$controlPlanePort",
        "CONTROL_PLANE_SITE_ADDRESS=:80",
        "FIREBASE_ADMIN_CREDENTIALS=$($firebaseCredentials.Replace('\','/'))",
        "NODE_REGISTRY_DATABASE_PASSWORD=$((Get-Content $registryPassword -Raw).Trim())",
        "PRESENCE_REDIS_PASSWORD=$((Get-Content $presencePassword -Raw).Trim())",
        "PUSH_DATABASE_PASSWORD=$((Get-Content $pushPassword -Raw).Trim())",
        "PUSH_INTERNAL_API_TOKEN=$((Get-Content $pushToken -Raw).Trim())",
        "SECURECHAT_IMAGE_PREFIX=$($release['SECURECHAT_IMAGE_PREFIX'])",
        "SECURECHAT_IMAGE_TAG=$($release['SECURECHAT_IMAGE_TAG'])"
    )

    [System.IO.File]::WriteAllLines(
        $runtimeEnvironmentPath,
        $runtime,
        [System.Text.UTF8Encoding]::new($false)
    )

    Set-Status "Pulling SecureChat images..."

    Push-Location $deploymentDirectory
    try {
        & $docker compose `
            --env-file $runtimeEnvironmentPath `
            -f $composePath `
            -f $releaseComposePath `
            pull

        if ($LASTEXITCODE -ne 0) {
            throw "Docker image pull failed."
        }

        Set-Status "Starting SecureChat services..."

        & $docker compose `
            --env-file $runtimeEnvironmentPath `
            -f $composePath `
            -f $releaseComposePath `
            up -d --remove-orphans --wait --wait-timeout 300

        if ($LASTEXITCODE -ne 0) {
            throw "SecureChat services failed to start."
        }
    } finally {
        Pop-Location
    }

    $address = @(
        Get-NetIPConfiguration |
            Where-Object { $null -ne $_.IPv4DefaultGateway -and $null -ne $_.IPv4Address } |
            ForEach-Object { $_.IPv4Address.IPAddress }
    ) | Select-Object -First 1

    if ([string]::IsNullOrWhiteSpace($address)) {
        $address = "localhost"
    }

    $url = "http://$address`:$controlPlanePort"

    $progress.Style = [System.Windows.Forms.ProgressBarStyle]::Blocks
    $progress.Value = 100
    $title.Text = "SecureChat control plane is running"
    $status.Text = $url
    [System.Windows.Forms.Application]::DoEvents()

    Start-Sleep -Seconds 3
    $form.Close()
    exit 0
} catch {
    Fail $_.Exception.Message
}
'@

[System.IO.File]::WriteAllText(
    (Join-Path $bundleRoot "Bootstrap-ControlPlane.ps1"),
    $bootstrap,
    [System.Text.UTF8Encoding]::new($false)
)

[System.IO.File]::WriteAllText(
    (Join-Path $bundleRoot "secrets/.gitignore"),
    "*`n!.gitignore`n",
    [System.Text.UTF8Encoding]::new($false)
)

Add-Type -AssemblyName System.IO.Compression.FileSystem

$archivePath = Join-Path $outputPath "securechat-control-plane.zip"

[System.IO.Compression.ZipFile]::CreateFromDirectory(
    $bundleRoot,
    $archivePath,
    [System.IO.Compression.CompressionLevel]::Optimal,
    $true
)

$hash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()

[System.IO.File]::WriteAllText(
    (Join-Path $outputPath "SHA256SUMS.txt"),
    "$hash  securechat-control-plane.zip`n",
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Created $archivePath"

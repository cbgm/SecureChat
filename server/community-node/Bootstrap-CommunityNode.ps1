[CmdletBinding()]
param(
    [switch]$PrepareOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$deploymentDirectory = $PSScriptRoot
$releaseEnvironmentPath = Join-Path $deploymentDirectory "release.env"
$runtimeEnvironmentPath = Join-Path $deploymentDirectory ".env.runtime"
$secretsDirectory = Join-Path $deploymentDirectory "secrets"
$composeFiles = @(
    (Join-Path $deploymentDirectory "docker-compose.yml"),
    (Join-Path $deploymentDirectory "docker-compose.release.yml")
)

function Read-EnvironmentFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $values = @{}
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line.Length -gt 0 -and -not $line.StartsWith("#")) {
            $separatorIndex = $line.IndexOf('=')
            if ($separatorIndex -gt 0) {
                $name = $line.Substring(0, $separatorIndex).Trim()
                $value = $line.Substring($separatorIndex + 1).Trim()
                $values[$name] = $value
            }
        }
    }
    return $values
}

function Get-PrimaryIpv4Address {
    param(
        [Parameter(Mandatory = $true)]
        [Uri]$ControlPlaneUri
    )

    $port =
        if ($ControlPlaneUri.IsDefaultPort) {
            if ($ControlPlaneUri.Scheme -eq "https") { 443 } else { 80 }
        } else {
            $ControlPlaneUri.Port
        }

    $routeProbe =
        Test-NetConnection `
            -ComputerName $ControlPlaneUri.Host `
            -Port $port `
            -InformationLevel Detailed `
            -WarningAction SilentlyContinue

    if (
        $routeProbe.TcpTestSucceeded -and
        $null -ne $routeProbe.SourceAddress -and
        $routeProbe.SourceAddress.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetwork
    ) {
        return $routeProbe.SourceAddress.IPAddressToString
    }

    throw "The SecureChat control plane is not reachable from this machine."
}

function New-RandomSecret {
    $bytes = New-Object byte[] 48
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return [Convert]::ToBase64String($bytes)
}

function Ensure-SecretFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        [System.IO.File]::WriteAllText(
            $Path,
            (New-RandomSecret),
            [System.Text.UTF8Encoding]::new($false)
        )
    }
}

function Invoke-Docker {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed: docker $($Arguments -join ' ')"
    }
}

if (-not (Test-Path -LiteralPath $releaseEnvironmentPath -PathType Leaf)) {
    throw "The deployment bundle is incomplete: release.env is missing."
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker is not installed or is not available to the current user."
}

$composeVersionOutput = (& docker compose version --short | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $composeVersionOutput -notmatch '(\d+\.\d+\.\d+)') {
    throw "Docker Compose is not available."
}
$composeVersion = [Version]$Matches[1]
if ($composeVersion -lt [Version]"2.24.4") {
    throw "Docker Compose 2.24.4 or newer is required."
}

$release = Read-EnvironmentFile -Path $releaseEnvironmentPath
$requiredReleaseValues = @(
    "CONTROL_PLANE_URL",
    "SECURECHAT_IMAGE_PREFIX",
    "SECURECHAT_IMAGE_TAG"
)
foreach ($requiredValue in $requiredReleaseValues) {
    if ([string]::IsNullOrWhiteSpace($release[$requiredValue])) {
        throw "release.env is missing $requiredValue."
    }
}

$controlPlaneUri = [Uri]$release["CONTROL_PLANE_URL"]
$hostAddress = Get-PrimaryIpv4Address -ControlPlaneUri $controlPlaneUri
$publicPort = 8490
$projectName = "securechat-community-node"

New-Item -ItemType Directory -Path $secretsDirectory -Force | Out-Null
$mailboxDatabasePasswordPath = Join-Path $secretsDirectory "mailbox-database-password.txt"
$federationDatabasePasswordPath = Join-Path $secretsDirectory "federation-database-password.txt"
$federationInternalTokenPath = Join-Path $secretsDirectory "federation-internal-api-token.txt"
$gatewayInternalTokenPath = Join-Path $secretsDirectory "gateway-internal-api-token.txt"

@(
    $mailboxDatabasePasswordPath,
    $federationDatabasePasswordPath,
    $federationInternalTokenPath,
    $gatewayInternalTokenPath
) | ForEach-Object { Ensure-SecretFile -Path $_ }

$runtimeEnvironment = @(
    "COMMUNITY_NODE_PROJECT_NAME=$projectName",
    "COMMUNITY_NODE_BIND_ADDRESS=0.0.0.0",
    "COMMUNITY_NODE_HTTP_PORT=$publicPort",
    "COMMUNITY_NODE_SITE_ADDRESS=:80",
    "CONTROL_PLANE_URL=$($release['CONTROL_PLANE_URL'])",
    "CLIENT_ENDPOINT=ws://$hostAddress`:$publicPort/relay",
    "FEDERATION_ENDPOINT=http://$hostAddress`:$publicPort",
    "MAILBOX_ENDPOINT=http://$hostAddress`:$publicPort",
    "SECURECHAT_IMAGE_PREFIX=$($release['SECURECHAT_IMAGE_PREFIX'])",
    "SECURECHAT_IMAGE_TAG=$($release['SECURECHAT_IMAGE_TAG'])",
    "SECURECHAT_UPDATE_INTERVAL_SECONDS=300",
    "MAILBOX_DATABASE_PASSWORD_FILE=./secrets/mailbox-database-password.txt",
    "FEDERATION_DATABASE_PASSWORD_FILE=./secrets/federation-database-password.txt",
    "FEDERATION_INTERNAL_API_TOKEN_FILE=./secrets/federation-internal-api-token.txt",
    "GATEWAY_INTERNAL_API_TOKEN_FILE=./secrets/gateway-internal-api-token.txt"
)

[System.IO.File]::WriteAllLines(
    $runtimeEnvironmentPath,
    $runtimeEnvironment,
    [System.Text.UTF8Encoding]::new($false)
)

$composeArguments = @("compose", "--env-file", $runtimeEnvironmentPath)
foreach ($composeFile in $composeFiles) {
    $composeArguments += @("-f", $composeFile)
}

Push-Location $deploymentDirectory
try {
    Invoke-Docker -Arguments ($composeArguments + @("config", "--quiet"))

    if (-not $PrepareOnly) {
        Invoke-Docker -Arguments ($composeArguments + @("pull"))
        Invoke-Docker -Arguments (
            $composeArguments + @(
                "up",
                "-d",
                "--remove-orphans",
                "--wait",
                "--wait-timeout",
                "300"
            )
        )
    }
} finally {
    Pop-Location
}

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
$script:DockerExecutable = $null

function Show-Result {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message,
        [Parameter(Mandatory = $true)]
        [bool]$IsError
    )

    try {
        Add-Type -AssemblyName PresentationFramework -ErrorAction Stop
        $icon =
            if ($IsError) {
                [System.Windows.MessageBoxImage]::Error
            } else {
                [System.Windows.MessageBoxImage]::Information
            }

        [System.Windows.MessageBox]::Show(
            $Message,
            "SecureChat Community Node",
            [System.Windows.MessageBoxButton]::OK,
            $icon
        ) | Out-Null
    } catch {
        if ($IsError) {
            Write-Error $Message
        } else {
            Write-Host $Message
        }
    }
}

function Read-EnvironmentFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $values = @{}
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line.Length -gt 0 -and -not $line.StartsWith("#")) {
            $separatorIndex = $line.IndexOf("=")
            if ($separatorIndex -gt 0) {
                $name = $line.Substring(0, $separatorIndex).Trim()
                $value = $line.Substring($separatorIndex + 1).Trim()
                $values[$name] = $value
            }
        }
    }
    return $values
}

function Resolve-DockerExecutable {
    $command = Get-Command docker -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    $candidates = @(
        (Join-Path $env:ProgramFiles "Docker\Docker\resources\bin\docker.exe"),
        (Join-Path $env:LOCALAPPDATA "Docker\Docker\resources\bin\docker.exe")
    )

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return $candidate
        }
    }

    throw "Docker Desktop is not installed."
}

function Test-DockerEngine {
    & $script:DockerExecutable info *> $null
    return $LASTEXITCODE -eq 0
}

function Ensure-DockerEngine {
    $script:DockerExecutable = Resolve-DockerExecutable

    if (Test-DockerEngine) {
        return
    }

    $desktopCandidates = @(
        (Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"),
        (Join-Path $env:LOCALAPPDATA "Docker\Docker Desktop.exe")
    )

    $desktop = $desktopCandidates | Where-Object {
        Test-Path -LiteralPath $_ -PathType Leaf
    } | Select-Object -First 1

    if ($null -eq $desktop) {
        throw "Docker Desktop is installed incompletely or cannot be found."
    }

    Start-Process -FilePath $desktop | Out-Null

    $deadline = [DateTime]::UtcNow.AddMinutes(3)
    while ([DateTime]::UtcNow -lt $deadline) {
        Start-Sleep -Seconds 2
        if (Test-DockerEngine) {
            return
        }
    }

    throw "Docker Desktop did not become ready."
}

function Assert-ComposeVersion {
    $versionOutput = (& $script:DockerExecutable compose version --short | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $versionOutput -notmatch '(\d+\.\d+\.\d+)') {
        throw "Docker Compose is not available."
    }

    if ([Version]$Matches[1] -lt [Version]"2.24.4") {
        throw "Docker Compose 2.24.4 or newer is required."
    }
}

function Test-ControlPlane {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url
    )

    try {
        $uri = [Uri]$Url
        $directoryUri = [Uri]::new($uri, "/v1/nodes")
        $response = Invoke-RestMethod `
            -Uri $directoryUri.AbsoluteUri `
            -Method Get `
            -TimeoutSec 2

        return $null -ne $response
    } catch {
        return $false
    }
}

function Get-LocalIpv4Address {
    $addresses = @(
        Get-NetIPConfiguration |
            Where-Object {
                $null -ne $_.IPv4DefaultGateway -and
                $null -ne $_.IPv4Address
            } |
            ForEach-Object { $_.IPv4Address.IPAddress } |
            Where-Object {
                $_ -and
                $_ -notlike "127.*" -and
                $_ -notlike "169.254.*"
            }
    )

    if ($addresses.Count -gt 0) {
        return $addresses[0]
    }

    throw "No usable IPv4 network connection was found."
}

function Find-ControlPlaneOnLan {
    $localAddress = Get-LocalIpv4Address
    $parts = $localAddress.Split(".")
    if ($parts.Count -ne 4) {
        return $null
    }

    $prefix = "$($parts[0]).$($parts[1]).$($parts[2])"

    foreach ($lastOctet in 1..254) {
        $candidate = "http://$prefix.$lastOctet`:8390"

        $client = [System.Net.Sockets.TcpClient]::new()
        try {
            $task = $client.ConnectAsync("$prefix.$lastOctet", 8390)
            if (-not $task.Wait(120)) {
                continue
            }

            if ($client.Connected -and (Test-ControlPlane -Url $candidate)) {
                return $candidate
            }
        } catch {
        } finally {
            $client.Dispose()
        }
    }

    return $null
}

function Resolve-ControlPlaneUrl {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ConfiguredUrl
    )

    if (Test-ControlPlane -Url $ConfiguredUrl) {
        return $ConfiguredUrl.TrimEnd("/")
    }

    $discovered = Find-ControlPlaneOnLan
    if (-not [string]::IsNullOrWhiteSpace($discovered)) {
        return $discovered.TrimEnd("/")
    }

    throw "The SecureChat control plane is not reachable and no control plane was found on the local network."
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

    return Get-LocalIpv4Address
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

    & $script:DockerExecutable @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker failed while starting the SecureChat node."
    }
}

try {
    if (-not (Test-Path -LiteralPath $releaseEnvironmentPath -PathType Leaf)) {
        throw "The deployment bundle is incomplete: release.env is missing."
    }

    foreach ($composeFile in $composeFiles) {
        if (-not (Test-Path -LiteralPath $composeFile -PathType Leaf)) {
            throw "The deployment bundle is incomplete: $([System.IO.Path]::GetFileName($composeFile)) is missing."
        }
    }

    Ensure-DockerEngine
    Assert-ComposeVersion

    $release = Read-EnvironmentFile -Path $releaseEnvironmentPath
    $requiredReleaseValues = @(
        "CONTROL_PLANE_URL",
        "SECURECHAT_IMAGE_PREFIX",
        "SECURECHAT_IMAGE_TAG"
    )

    foreach ($requiredValue in $requiredReleaseValues) {
        if (
            -not $release.ContainsKey($requiredValue) -or
            [string]::IsNullOrWhiteSpace($release[$requiredValue])
        ) {
            throw "release.env is missing $requiredValue."
        }
    }

    $controlPlaneUrl = Resolve-ControlPlaneUrl -ConfiguredUrl $release["CONTROL_PLANE_URL"]
    $controlPlaneUri = [Uri]$controlPlaneUrl
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
        "CONTROL_PLANE_URL=$controlPlaneUrl",
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

    if (-not $PrepareOnly) {
        Show-Result `
            -Message "SecureChat node is running.`n`nhttp://$hostAddress`:$publicPort" `
            -IsError $false
    }

    exit 0
} catch {
    Show-Result -Message $_.Exception.Message -IsError $true
    exit 1
}

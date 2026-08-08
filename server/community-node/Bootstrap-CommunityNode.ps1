[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$deploymentDirectory = $PSScriptRoot
$releaseEnvironmentPath = Join-Path $deploymentDirectory "release.env"
$runtimeEnvironmentPath = Join-Path $deploymentDirectory ".env.runtime"
$secretsDirectory = Join-Path $deploymentDirectory "secrets"
$composePath = Join-Path $deploymentDirectory "docker-compose.yml"
$releaseComposePath = Join-Path $deploymentDirectory "docker-compose.release.yml"
$logPath = Join-Path $deploymentDirectory "bootstrap-community-node.log"

$publicPort = 8490
$script:Docker = $null

$form = New-Object System.Windows.Forms.Form
$form.Text = "SecureChat Community Node"
$form.Size = New-Object System.Drawing.Size(560, 190)
$form.StartPosition = "CenterScreen"
$form.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::FixedDialog
$form.MaximizeBox = $false
$form.TopMost = $true

$title = New-Object System.Windows.Forms.Label
$title.Location = New-Object System.Drawing.Point(24, 22)
$title.Size = New-Object System.Drawing.Size(505, 28)
$title.Font = New-Object System.Drawing.Font("Segoe UI", 12, [System.Drawing.FontStyle]::Bold)
$title.Text = "Starting SecureChat community node"
$form.Controls.Add($title)

$status = New-Object System.Windows.Forms.Label
$status.Location = New-Object System.Drawing.Point(24, 58)
$status.Size = New-Object System.Drawing.Size(505, 38)
$status.Text = "Preparing..."
$form.Controls.Add($status)

$progress = New-Object System.Windows.Forms.ProgressBar
$progress.Location = New-Object System.Drawing.Point(24, 108)
$progress.Size = New-Object System.Drawing.Size(505, 20)
$progress.Style = [System.Windows.Forms.ProgressBarStyle]::Marquee
$progress.MarqueeAnimationSpeed = 25
$form.Controls.Add($progress)

$form.Show()
[System.Windows.Forms.Application]::DoEvents()

function Write-Log {
    param([Parameter(Mandatory = $true)][string]$Message)

    Add-Content `
        -LiteralPath $logPath `
        -Value "[$(Get-Date -Format o)] $Message" `
        -Encoding UTF8
}

function Set-Status {
    param([Parameter(Mandatory = $true)][string]$Message)

    $status.Text = $Message
    Write-Log $Message
    [System.Windows.Forms.Application]::DoEvents()
}

function Read-EnvironmentFile {
    param([Parameter(Mandatory = $true)][string]$Path)

    $values = @{}

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()

        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")

        if ($separator -gt 0) {
            $name = $trimmed.Substring(0, $separator).Trim()
            $value = $trimmed.Substring($separator + 1).Trim()
            $values[$name] = $value
        }
    }

    return $values
}

function Find-Docker {
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
    & $script:Docker info *> $null
    return $LASTEXITCODE -eq 0
}

function Ensure-Docker {
    $script:Docker = Find-Docker

    Remove-Item Env:DOCKER_HOST -ErrorAction SilentlyContinue
    Remove-Item Env:DOCKER_TLS_VERIFY -ErrorAction SilentlyContinue
    Remove-Item Env:DOCKER_CERT_PATH -ErrorAction SilentlyContinue
    Remove-Item Env:DOCKER_CONTEXT -ErrorAction SilentlyContinue

    if (Test-DockerEngine) {
        return
    }

    $desktopCandidates = @(
        (Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"),
        (Join-Path $env:LOCALAPPDATA "Docker\Docker Desktop.exe")
    )

    $desktop = $desktopCandidates |
        Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } |
        Select-Object -First 1

    if ($null -eq $desktop) {
        throw "Docker Desktop could not be found."
    }

    if ($null -eq (Get-Process -Name "Docker Desktop" -ErrorAction SilentlyContinue)) {
        Start-Process -FilePath $desktop | Out-Null
    }

    $deadline = [DateTime]::UtcNow.AddMinutes(5)

    while ([DateTime]::UtcNow -lt $deadline) {
        Start-Sleep -Seconds 2
        [System.Windows.Forms.Application]::DoEvents()

        if (Test-DockerEngine) {
            return
        }
    }

    throw "Docker Desktop did not become ready."
}

function Assert-ComposeVersion {
    $versionOutput = (& $script:Docker compose version --short | Out-String).Trim()

    if ($LASTEXITCODE -ne 0 -or $versionOutput -notmatch '(\d+\.\d+\.\d+)') {
        throw "Docker Compose is not available."
    }

    if ([Version]$Matches[1] -lt [Version]"2.24.4") {
        throw "Docker Compose 2.24.4 or newer is required."
    }
}

function Get-LocalIpv4Addresses {
    return @(
        Get-NetIPConfiguration |
            Where-Object {
                $null -ne $_.IPv4Address
            } |
            ForEach-Object {
                $_.IPv4Address.IPAddress
            } |
            Where-Object {
                $_ -and
                $_ -notlike "127.*" -and
                $_ -notlike "169.254.*"
            }
    )
}

function Get-PrimaryIpv4Address {
    $addresses = @(
        Get-NetIPConfiguration |
            Where-Object {
                $null -ne $_.IPv4DefaultGateway -and
                $null -ne $_.IPv4Address
            } |
            ForEach-Object {
                $_.IPv4Address.IPAddress
            } |
            Where-Object {
                $_ -and
                $_ -notlike "127.*" -and
                $_ -notlike "169.254.*"
            }
    )

    if ($addresses.Count -gt 0) {
        return $addresses[0]
    }

    $fallback = Get-LocalIpv4Addresses

    if ($fallback.Count -gt 0) {
        return $fallback[0]
    }

    throw "No usable IPv4 address was found."
}

function Test-IsLocalHostAddress {
    param([Parameter(Mandatory = $true)][string]$HostName)

    if ($HostName -eq "localhost" -or $HostName -eq "127.0.0.1") {
        return $true
    }

    return (Get-LocalIpv4Addresses) -contains $HostName
}

function Test-ControlPlane {
    param([Parameter(Mandatory = $true)][string]$Url)

    try {
        $base = $Url.TrimEnd("/")
        $response = Invoke-RestMethod `
            -Uri "$base/v1/nodes" `
            -Method Get `
            -TimeoutSec 3

        return $null -ne $response
    } catch {
        return $false
    }
}

function Resolve-ControlPlaneUrls {
    param([Parameter(Mandatory = $true)][string]$ConfiguredUrl)

    $uri = [Uri]$ConfiguredUrl

    if (-not $uri.IsAbsoluteUri -or $uri.Scheme -notin @("http", "https")) {
        throw "CONTROL_PLANE_URL must be an absolute HTTP or HTTPS URL."
    }

    $port =
        if ($uri.IsDefaultPort) {
            if ($uri.Scheme -eq "https") { 443 } else { 80 }
        } else {
            $uri.Port
        }

    if (Test-IsLocalHostAddress -HostName $uri.Host) {
        $hostProbeUrl = "$($uri.Scheme)://127.0.0.1`:$port"

        if (-not (Test-ControlPlane -Url $hostProbeUrl)) {
            throw "The local SecureChat control plane is not reachable at $hostProbeUrl."
        }

        $containerUrl =
            if ($uri.Scheme -eq "http") {
                "http://host.docker.internal`:$port"
            } else {
                $ConfiguredUrl.TrimEnd("/")
            }

        return [PSCustomObject]@{
            HostProbeUrl = $hostProbeUrl
            ContainerUrl = $containerUrl
        }
    }

    if (-not (Test-ControlPlane -Url $ConfiguredUrl)) {
        throw "The SecureChat control plane is not reachable at $ConfiguredUrl."
    }

    return [PSCustomObject]@{
        HostProbeUrl = $ConfiguredUrl.TrimEnd("/")
        ContainerUrl = $ConfiguredUrl.TrimEnd("/")
    }
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
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        [System.IO.File]::WriteAllText(
            $Path,
            (New-RandomSecret),
            [System.Text.UTF8Encoding]::new($false)
        )
    }
}

function Invoke-Compose {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    # Windows PowerShell 5.1 can turn normal native stderr output such as
    # "Image ... Pulling" into a terminating NativeCommandError when the
    # script uses ErrorActionPreference=Stop. Docker Compose writes progress
    # to stderr even on success, so temporarily disable terminating handling
    # only for the native Docker invocation and decide success from LASTEXITCODE.
    $previousErrorActionPreference = $ErrorActionPreference

    try {
        $ErrorActionPreference = "Continue"

        $output = @(
            & $script:Docker compose `
                --env-file $runtimeEnvironmentPath `
                -f $composePath `
                -f $releaseComposePath `
                @Arguments 2>&1
        )

        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    foreach ($line in $output) {
        Write-Log "compose: $($line.ToString())"
    }

    if ($exitCode -ne 0) {
        $detail = (
            $output |
                ForEach-Object { $_.ToString() } |
                Select-Object -Last 12 |
                Out-String
        ).Trim()

        if ([string]::IsNullOrWhiteSpace($detail)) {
            throw "Docker Compose failed: $($Arguments -join ' ')"
        }

        throw "Docker Compose failed: $($Arguments -join ' ')`n`n$detail"
    }
}

function Wait-ForContainerRunning {
    param(
        [Parameter(Mandatory = $true)][string]$Service,
        [int]$TimeoutSeconds = 60
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)

    while ([DateTime]::UtcNow -lt $deadline) {
        $containerId = (
            & $script:Docker compose `
                --env-file $runtimeEnvironmentPath `
                -f $composePath `
                -f $releaseComposePath `
                ps -q $Service 2>$null |
                Select-Object -First 1
        )

        if (-not [string]::IsNullOrWhiteSpace($containerId)) {
            $state = (
                & $script:Docker inspect `
                    --format "{{.State.Status}}" `
                    $containerId 2>$null |
                    Out-String
            ).Trim()

            if ($state -eq "running") {
                return
            }
        }

        Start-Sleep -Seconds 1
        [System.Windows.Forms.Application]::DoEvents()
    }

    throw "$Service did not start."
}

function Escape-SqlLiteral {
    param([Parameter(Mandatory = $true)][string]$Value)

    return $Value.Replace("'", "''")
}

function Synchronize-PostgresPassword {
    param(
        [Parameter(Mandatory = $true)][string]$Service,
        [Parameter(Mandatory = $true)][string]$DatabaseUser,
        [Parameter(Mandatory = $true)][string]$DatabaseName,
        [Parameter(Mandatory = $true)][string]$Password
    )

    Set-Status "Synchronizing $Service credentials..."

    $escapedPassword = Escape-SqlLiteral -Value $Password
    $sql = "ALTER ROLE `"$DatabaseUser`" WITH PASSWORD '$escapedPassword';"

    $output = & $script:Docker compose `
        --env-file $runtimeEnvironmentPath `
        -f $composePath `
        -f $releaseComposePath `
        exec -T `
        $Service `
        psql `
        -v ON_ERROR_STOP=1 `
        -U $DatabaseUser `
        -d $DatabaseName `
        -c $sql 2>&1

    $exitCode = $LASTEXITCODE

    foreach ($line in $output) {
        Write-Log "$Service password sync: $line"
    }

    if ($exitCode -ne 0) {
        throw "Could not synchronize the password in $Service."
    }
}

function Test-HttpReady {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [int]$TimeoutMilliseconds = 1500
    )

    try {
        $request = [System.Net.HttpWebRequest]::Create($Url)
        $request.Method = "GET"
        $request.Timeout = $TimeoutMilliseconds
        $request.ReadWriteTimeout = $TimeoutMilliseconds
        $request.AllowAutoRedirect = $false

        $response = $request.GetResponse()

        try {
            return [int]$response.StatusCode -ge 200 -and
                [int]$response.StatusCode -lt 300
        } finally {
            $response.Close()
        }
    } catch {
        return $false
    }
}

function Wait-ForEndpoint {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Url,
        [int]$TimeoutSeconds = 120
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)

    while ([DateTime]::UtcNow -lt $deadline) {
        Set-Status "Waiting for $Name..."

        if (Test-HttpReady -Url $Url) {
            Write-Log "$Name ready: $Url"
            return
        }

        Start-Sleep -Seconds 2
    }

    throw "$Name did not become ready: $Url"
}

function Collect-Diagnostics {
    try {
        Write-Log "----- docker compose ps -----"
        (& $script:Docker compose `
            --env-file $runtimeEnvironmentPath `
            -f $composePath `
            -f $releaseComposePath `
            ps --all 2>&1) | ForEach-Object {
                Write-Log $_.ToString()
            }

        Write-Log "----- docker compose logs -----"
        (& $script:Docker compose `
            --env-file $runtimeEnvironmentPath `
            -f $composePath `
            -f $releaseComposePath `
            logs --tail 180 --no-color 2>&1) | ForEach-Object {
                Write-Log $_.ToString()
            }
    } catch {
        Write-Log "Could not collect Docker diagnostics: $($_.Exception.Message)"
    }
}

function Fail {
    param([Parameter(Mandatory = $true)][string]$Message)

    Write-Log "FAILED: $Message"

    if ($null -ne $script:Docker) {
        Push-Location $deploymentDirectory
        try {
            Collect-Diagnostics
        } finally {
            Pop-Location
        }
    }

    [System.Windows.Forms.MessageBox]::Show(
        "$Message`n`nDiagnostic log:`n$logPath",
        "SecureChat Community Node",
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Error
    ) | Out-Null

    $form.Close()
    exit 1
}

try {
    Set-Content -LiteralPath $logPath -Value "" -Encoding UTF8

    foreach ($requiredFile in @(
        $releaseEnvironmentPath,
        $composePath,
        $releaseComposePath
    )) {
        if (-not (Test-Path -LiteralPath $requiredFile -PathType Leaf)) {
            throw "The deployment bundle is incomplete: $([System.IO.Path]::GetFileName($requiredFile)) is missing."
        }
    }

    Set-Status "Starting Docker Desktop..."
    Ensure-Docker
    Assert-ComposeVersion

    Set-Status "Checking SecureChat control plane..."
    $release = Read-EnvironmentFile -Path $releaseEnvironmentPath

    foreach ($requiredValue in @(
        "CONTROL_PLANE_URL",
        "SECURECHAT_IMAGE_PREFIX",
        "SECURECHAT_IMAGE_TAG"
    )) {
        if (
            -not $release.ContainsKey($requiredValue) -or
            [string]::IsNullOrWhiteSpace($release[$requiredValue])
        ) {
            throw "release.env is missing $requiredValue."
        }
    }

    $controlPlane = Resolve-ControlPlaneUrls `
        -ConfiguredUrl $release["CONTROL_PLANE_URL"]

    $hostAddress = Get-PrimaryIpv4Address

    Set-Status "Preparing SecureChat node secrets..."
    New-Item -ItemType Directory -Path $secretsDirectory -Force | Out-Null

    $mailboxPasswordPath = Join-Path $secretsDirectory "mailbox-database-password.txt"
    $federationPasswordPath = Join-Path $secretsDirectory "federation-database-password.txt"
    $federationTokenPath = Join-Path $secretsDirectory "federation-internal-api-token.txt"
    $gatewayTokenPath = Join-Path $secretsDirectory "gateway-internal-api-token.txt"

    @(
        $mailboxPasswordPath,
        $federationPasswordPath,
        $federationTokenPath,
        $gatewayTokenPath
    ) | ForEach-Object {
        Ensure-SecretFile -Path $_
    }

    $mailboxPassword = (Get-Content -LiteralPath $mailboxPasswordPath -Raw).Trim()
    $federationPassword = (Get-Content -LiteralPath $federationPasswordPath -Raw).Trim()

    $runtimeEnvironment = @(
        "COMMUNITY_NODE_PROJECT_NAME=securechat-community-node",
        "COMMUNITY_NODE_BIND_ADDRESS=0.0.0.0",
        "COMMUNITY_NODE_HTTP_PORT=$publicPort",
        "COMMUNITY_NODE_SITE_ADDRESS=:80",
        "CONTROL_PLANE_URL=$($controlPlane.ContainerUrl)",
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

    Push-Location $deploymentDirectory
    try {
        Set-Status "Validating node configuration..."
        Invoke-Compose -Arguments @("config", "--quiet")

        Set-Status "Pulling SecureChat node images..."
        Invoke-Compose -Arguments @("pull")

        Set-Status "Starting node databases..."
        Invoke-Compose -Arguments @(
            "up",
            "-d",
            "mailbox-database",
            "federation-database"
        )

        Wait-ForContainerRunning -Service "mailbox-database"
        Wait-ForContainerRunning -Service "federation-database"

        Start-Sleep -Seconds 3

        Synchronize-PostgresPassword `
            -Service "mailbox-database" `
            -DatabaseUser "securechat_mailbox" `
            -DatabaseName "securechat_mailbox" `
            -Password $mailboxPassword

        Synchronize-PostgresPassword `
            -Service "federation-database" `
            -DatabaseUser "securechat_federation" `
            -DatabaseName "securechat_federation" `
            -Password $federationPassword

        Set-Status "Starting SecureChat node services..."
        Invoke-Compose -Arguments @(
            "up",
            "-d",
            "--remove-orphans"
        )

        Set-Status "Reloading community-node routing..."
        Invoke-Compose -Arguments @(
            "up",
            "-d",
            "--force-recreate",
            "caddy"
        )
    } finally {
        Pop-Location
    }

    Wait-ForEndpoint `
        -Name "mailbox" `
        -Url "http://127.0.0.1:$publicPort/health/mailbox"

    Wait-ForEndpoint `
        -Name "federation" `
        -Url "http://127.0.0.1:$publicPort/health/federation"

    Wait-ForEndpoint `
        -Name "gateway" `
        -Url "http://127.0.0.1:$publicPort/health/gateway"

    Set-Status "Verifying control-plane registration..."

    if (-not (Test-ControlPlane -Url $controlPlane.HostProbeUrl)) {
        throw "The SecureChat control plane became unreachable."
    }

    $progress.Style = [System.Windows.Forms.ProgressBarStyle]::Blocks
    $progress.Value = 100
    $title.Text = "SecureChat community node is running"
    $status.Text = "http://$hostAddress`:$publicPort"
    Write-Log "SUCCESS: http://$hostAddress`:$publicPort"

    [System.Windows.Forms.Application]::DoEvents()
    Start-Sleep -Seconds 3

    $form.Close()
    exit 0
} catch {
    Fail -Message $_.Exception.Message
}

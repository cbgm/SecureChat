[CmdletBinding()]
param(
    [string]$OutputDirectory = "dist",
    [string]$ControlPlaneUrl = "http://192.168.178.60:8390",
    [string]$ImagePrefix = "ghcr.io/cbgm/securechat",
    [string]$ImageTag = "latest"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$outputPath = [System.IO.Path]::GetFullPath((Join-Path $repositoryRoot $OutputDirectory))
$stagingRoot = Join-Path ([System.IO.Path]::GetTempPath()) "securechat-community-node-bundle"
$bundleRoot = Join-Path $stagingRoot "securechat-community-node"
$communityNodeRoot = Join-Path $repositoryRoot "server/community-node"

$controlPlaneUri = $null
if (-not [Uri]::TryCreate($ControlPlaneUrl, [UriKind]::Absolute, [ref]$controlPlaneUri)) {
    throw "CONTROL_PLANE_URL must be an absolute URL."
}
if ($controlPlaneUri.Scheme -notin @("http", "https")) {
    throw "CONTROL_PLANE_URL must use http or https."
}
if ($ImagePrefix -notmatch '^[a-z0-9.-]+(?:/[a-z0-9._-]+)+$') {
    throw "ImagePrefix is not a valid lowercase container-image prefix."
}
if ($ImageTag -notmatch '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$') {
    throw "ImageTag is not a valid container-image tag."
}

$bundleFiles = @(
    "docker-compose.yml",
    "docker-compose.release.yml",
    "Caddyfile",
    "Bootstrap-CommunityNode.ps1",
    "Start-SecureChatNode.cmd",
    "bootstrap-community-node.sh",
    "start-securechat-node.sh",
    "Start-SecureChatNode.command",
    "README.md"
)

Remove-Item -LiteralPath $stagingRoot -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $bundleRoot -Force | Out-Null
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null

foreach ($relativePath in $bundleFiles) {
    $sourcePath = Join-Path $communityNodeRoot $relativePath
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Deployment bundle source file does not exist: $sourcePath"
    }

    Copy-Item -LiteralPath $sourcePath -Destination (Join-Path $bundleRoot $relativePath) -Force
}

$releaseEnvironment = @(
    "CONTROL_PLANE_URL=$ControlPlaneUrl",
    "SECURECHAT_IMAGE_PREFIX=$ImagePrefix",
    "SECURECHAT_IMAGE_TAG=$ImageTag"
)
[System.IO.File]::WriteAllLines(
    (Join-Path $bundleRoot "release.env"),
    $releaseEnvironment,
    [System.Text.UTF8Encoding]::new($false)
)

New-Item -ItemType Directory -Path (Join-Path $bundleRoot "secrets") -Force | Out-Null

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archivePath = Join-Path $outputPath "securechat-community-node.zip"
Remove-Item -LiteralPath $archivePath -Force -ErrorAction SilentlyContinue
[System.IO.Compression.ZipFile]::CreateFromDirectory(
    $bundleRoot,
    $archivePath,
    [System.IO.Compression.CompressionLevel]::Optimal,
    $true
)

$tarArchivePath = Join-Path $outputPath "securechat-community-node.tar.gz"
Remove-Item -LiteralPath $tarArchivePath -Force -ErrorAction SilentlyContinue
& tar -czf $tarArchivePath -C $stagingRoot "securechat-community-node"
if ($LASTEXITCODE -ne 0) {
    throw "Could not create $tarArchivePath"
}

$zipChecksum = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
$tarChecksum = (Get-FileHash -LiteralPath $tarArchivePath -Algorithm SHA256).Hash.ToLowerInvariant()
$checksumLines = @(
    "$zipChecksum  securechat-community-node.zip",
    "$tarChecksum  securechat-community-node.tar.gz"
)
[System.IO.File]::WriteAllLines(
    (Join-Path $outputPath "SHA256SUMS.txt"),
    $checksumLines,
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Created $archivePath"
Write-Host "Created $tarArchivePath"

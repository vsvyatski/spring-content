#Requires -Version 6.2
<#
.SYNOPSIS
  Set the CI-friendly reactor version (revision property and .mvn/maven.config).

.EXAMPLE
  ./scripts/Set-Revision.ps1 4.2.0-SNAPSHOT

.EXAMPLE
  ./scripts/Set-Revision.ps1 4.2.0-SNAPSHOT -BackupPoms
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory, Position = 0)]
    [string] $Version,

    [switch] $BackupPoms
)

$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$mvnw = Join-Path $repoRoot 'mvnw'
if ($IsWindows -or ($env:OS -match 'Windows')) {
    $mvnw += '.cmd'
}

$mvnArgs = @('-N', 'versions:set-property', '-Dproperty=revision', "-DnewVersion=$Version")
if (-not $BackupPoms) {
    $mvnArgs += '-DgenerateBackupPoms=false'
}

Push-Location -LiteralPath $repoRoot
try {
    & $mvnw @mvnArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    $configDir = Join-Path $repoRoot '.mvn'
    $configPath = Join-Path $configDir 'maven.config'
    if (-not (Test-Path -LiteralPath $configDir)) {
        New-Item -ItemType Directory -Path $configDir -Force | Out-Null
    }
    Set-Content -Path $configPath -Value "-Drevision=$Version" -Encoding utf8NoBOM
}
finally {
    Pop-Location
}

<#
.SYNOPSIS
    Prepares an OSM PBF file for use with the OSRM routing engine.

.DESCRIPTION
    Runs the three OSRM data-preparation stages (extract, partition, customize)
    in sequence using the official osrm/osrm-backend:v5.27 Docker image.
    The processed files are written to the same directory as the input file.

    All three stages must complete successfully before OSRM can serve routes.
    The script exits with a non-zero code immediately if any stage fails.

.PARAMETER OsmPbfPath
    Path to the .osm.pbf input file (absolute or relative).
    Example: ".\osrm-data\ethiopia-latest.osm.pbf"

.EXAMPLE
    .\scripts\prep-osrm.ps1 -OsmPbfPath ".\osrm-data\ethiopia-latest.osm.pbf"

.EXAMPLE
    .\scripts\prep-osrm.ps1 "C:\maps\addis-ababa.osm.pbf"

.NOTES
    Requires Docker Desktop to be installed and running.
    Run this script from the project root directory.
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0,
        HelpMessage = "Path to the .osm.pbf file to process")]
    [ValidateNotNullOrEmpty()]
    [string]$OsmPbfPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------
# Resolve and validate the input file
# ---------------------------------------------------------------------------
$resolvedPath = Resolve-Path -Path $OsmPbfPath -ErrorAction SilentlyContinue
if (-not $resolvedPath) {
    Write-Error "Input file not found: $OsmPbfPath"
    exit 1
}

$inputFile = Get-Item -Path $resolvedPath.Path
if ($inputFile.Extension -ne ".pbf") {
    Write-Error "Input file must have a .pbf extension. Got: $($inputFile.Name)"
    exit 1
}

$dataDir      = $inputFile.DirectoryName
$fileBaseName = $inputFile.BaseName        # e.g. "ethiopia-latest"
$osrmFile     = "/data/$fileBaseName.osrm"
$pbfFile      = "/data/$($inputFile.Name)"

Write-Host ""
Write-Host "=== OSRM Data Preparation ===" -ForegroundColor Cyan
Write-Host "Input file : $($inputFile.FullName)"
Write-Host "Data dir   : $dataDir"
Write-Host "OSRM image : osrm/osrm-backend:v5.27"
Write-Host ""

# ---------------------------------------------------------------------------
# Helper: run one Docker stage and exit on failure
# ---------------------------------------------------------------------------
function Invoke-OsrmStage {
    param(
        [string]$StageName,
        [string]$StageCommand
    )

    Write-Host "--- Stage: $StageName ---" -ForegroundColor Yellow
    Write-Host "Command: $StageCommand"
    Write-Host ""

    $dockerArgs = @(
        "run", "--rm", "-t",
        "-v", "${dataDir}:/data",
        "osrm/osrm-backend:v5.27"
    ) + ($StageCommand -split " ")

    & docker @dockerArgs

    if ($LASTEXITCODE -ne 0) {
        Write-Error "OSRM stage '$StageName' failed with exit code $LASTEXITCODE."
        exit $LASTEXITCODE
    }

    Write-Host ""
    Write-Host "Stage '$StageName' completed successfully." -ForegroundColor Green
    Write-Host ""
}

# ---------------------------------------------------------------------------
# Verify Docker is available
# ---------------------------------------------------------------------------
Write-Host "Checking Docker availability..." -ForegroundColor Gray
& docker info > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker is not running or not installed. Please start Docker Desktop and try again."
    exit 1
}
Write-Host "Docker is available." -ForegroundColor Green
Write-Host ""

# ---------------------------------------------------------------------------
# Stage 1: Extract
# ---------------------------------------------------------------------------
Invoke-OsrmStage -StageName "Extract" -StageCommand "osrm-extract -p /opt/car.lua $pbfFile"

# ---------------------------------------------------------------------------
# Stage 2: Partition
# ---------------------------------------------------------------------------
Invoke-OsrmStage -StageName "Partition" -StageCommand "osrm-partition $osrmFile"

# ---------------------------------------------------------------------------
# Stage 3: Customize
# ---------------------------------------------------------------------------
Invoke-OsrmStage -StageName "Customize" -StageCommand "osrm-customize $osrmFile"

# ---------------------------------------------------------------------------
# Done
# ---------------------------------------------------------------------------
Write-Host "=== All OSRM preparation stages completed successfully ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Processed files are in: $dataDir"
Write-Host "You can now start the full stack with:"
Write-Host "  docker compose up" -ForegroundColor White
Write-Host ""
exit 0

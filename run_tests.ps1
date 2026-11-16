# Run Tests for Lifelane Mini Project
# This script refreshes environment variables, ensures Docker is running, and executes Maven tests.

# Refresh PATH after installing tools (e.g., Maven via Chocolatey)
if (Get-Command refreshenv -ErrorAction SilentlyContinue) {
    refreshenv
} else {
    Write-Host "refreshenv not available. Please open a new terminal if PATH changes were recent."
}

# Change to the Maven module directory
$projectRoot = "C:\Users\HP\Desktop\lifelane mini project"
$backendDir = Join-Path $projectRoot "backend"
Set-Location $backendDir

# Verify Docker is running
Write-Host "Checking Docker status..."
$dockerInfo = docker version 2>&1
if ($dockerInfo -match "Server:\s*Version") {
    Write-Host "Docker is running."
} else {
    Write-Warning "Docker does not appear to be running. Please start Docker Desktop and ensure the Docker daemon is active before proceeding."
    exit 1
}

# Run Maven tests
Write-Host "Running Maven tests..."
$mvnResult = mvn test
if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven tests failed. Review the output above."
    exit $LASTEXITCODE
}
Write-Host "All tests passed successfully."


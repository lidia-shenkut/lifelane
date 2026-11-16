@echo off
rem Wrapper to execute the PowerShell test script with proper execution policy

rem Change to the project root (adjust if needed)
cd /d "C:\Users\HP\Desktop\lifelane mini project"

rem Run the PowerShell script
powershell -NoProfile -ExecutionPolicy Bypass -File "run_tests.ps1"

pause

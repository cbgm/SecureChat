@echo off
cd /d "%~dp0"

echo =====================================
echo SecureChat Setup
echo =====================================

call gradlew.bat setup

echo.
echo Setup complete.
pause

@echo off
setlocal

:skip
This path has to be adapted for your windows machine
The script starts two instances of emulators (one has to be named first and one second) and
changes the phone numbers to unique for testing
:skip

set "EMU=%LOCALAPPDATA%\Android\Sdk\emulator.backup\emulator.exe"

if not exist "%EMU%" (
    echo Emulator wurde nicht gefunden:
    echo %EMU%
    pause
    exit /b 1
)

start "User 1" "%EMU%" -avd first -port 5554 -phone-number 15550000001 -no-snapshot-load

timeout /t 5 /nobreak >nul

start "User 2" "%EMU%" -avd second -port 5556 -phone-number 15550000002 -no-snapshot-load

endlocal

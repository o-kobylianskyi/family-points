@echo off
setlocal
cd /d "%~dp0..\.."
echo FamilyPoints Android APK - Contabo publisher
echo Uses existing SIGNED android-release-output APK + manifest.
echo Does not rebuild Android and does not touch Docker or database.
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0publish-to-contabo.ps1" %*
set "RESULT=%ERRORLEVEL%"
echo.
if not "%RESULT%"=="0" (
  echo ERROR: Publishing failed or was cancelled. Exit code: %RESULT%
) else (
  echo SUCCESS: APK release was published and verified.
)
exit /b %RESULT%

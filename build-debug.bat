@echo off
REM Saigai — build the debug APK with the toolchain the project is verified against.
REM
REM Why this file exists: this machine's default `java` is JDK 25, and Gradle
REM 8.11.1 / AGP 8.7.3 cannot run on it (it fails with the unhelpful message
REM "What went wrong: 25.0.1"). This script points JAVA_HOME at JDK 17 for the
REM duration of the build only, so nothing else on your machine changes.
REM
REM Usage:  build-debug.bat            (debug APK)
REM         build-debug.bat clean      (wipe build output first, then build)

setlocal
set "JAVA_HOME=C:\Users\preeb\Yazhi\tools\jdk17\jdk-17.0.21.1+2"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: JDK 17 not found at %JAVA_HOME%
    echo Install Temurin 17 or edit JAVA_HOME in this file.
    exit /b 1
)

cd /d "%~dp0"

if /i "%~1"=="clean" (
    call gradlew.bat :app:clean
    if errorlevel 1 exit /b 1
)

call gradlew.bat :app:assembleDebug
if errorlevel 1 (
    echo.
    echo BUILD FAILED - see the messages above.
    exit /b 1
)

echo.
echo BUILD OK
echo.
echo Install on an arm64 phone (this is the APK to use for the demo):
echo   %~dp0app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
echo Emulator / x86_64 build:
echo   %~dp0app\build\outputs\apk\debug\app-x86_64-debug.apk
endlocal

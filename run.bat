@echo off
cd /d "%~dp0"
title Threat Detector - Signal Intelligence

if not exist "target\threat-detector-java-0.1.0.jar" (
    echo Building the application...
    call mvn -q clean package -DskipTests
    if errorlevel 1 (
        echo Build failed.
        pause
        exit /b 1
    )
)

echo Starting Threat Detector...
java -jar target\threat-detector-java-0.1.0.jar
if errorlevel 1 (
    echo.
    echo The application exited with an error.
    pause
)

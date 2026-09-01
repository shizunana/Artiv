@echo off
setlocal

set "PROJECT_DIR=%~dp0"
set "JAVA_HOME=D:\CodeTool\Java\jdk-21.0.12"
set "PATH=%JAVA_HOME%\bin;D:\CodeTool\Maven\apache-maven-3.9.16\bin;D:\CodeTool\NodeJS\node-v24.19.0-win-x64;%PATH%"

if not exist "%PROJECT_DIR%backend\pom.xml" (
    echo [ERROR] Project backend not found: %PROJECT_DIR%backend
    pause
    exit /b 1
)

if not exist "%PROJECT_DIR%frontend\package.json" (
    echo [ERROR] Project frontend not found: %PROJECT_DIR%frontend
    pause
    exit /b 1
)

echo Checking Java...
java -version
if errorlevel 1 (
    echo [ERROR] JDK 21 is unavailable.
    pause
    exit /b 1
)

echo Checking MySQL service...
sc query "MySQL80-Artiv" | findstr /C:"RUNNING" >nul
if errorlevel 1 (
    echo Starting MySQL80-Artiv...
    net start "MySQL80-Artiv"
    if errorlevel 1 (
        echo [ERROR] MySQL could not be started. Right-click this script and choose Run as administrator.
        pause
        exit /b 1
    )
)

netstat -ano | findstr /R /C:":8080 .*LISTENING" >nul
if errorlevel 1 (
    echo Starting backend on port 8080...
    start "Artiv Backend - port 8080" /D "%PROJECT_DIR%backend" cmd.exe /d /k "mvn.cmd spring-boot:run"
) else (
    echo Backend port 8080 is already in use; skipping backend startup.
)

netstat -ano | findstr /R /C:":5173 .*LISTENING" >nul
if errorlevel 1 (
    echo Starting frontend on port 5173...
    start "Artiv Frontend - port 5173" /D "%PROJECT_DIR%frontend" cmd.exe /d /k "npm.cmd run dev -- --host 127.0.0.1"
) else (
    echo Frontend port 5173 is already in use; skipping frontend startup.
)

echo Waiting for services, then opening the site...
timeout /t 8 /nobreak >nul
start "" "http://127.0.0.1:5173/"

echo.
echo Startup commands have been sent.
echo Keep the backend and frontend windows open while using Artiv.
echo To stop, press Ctrl+C once in each window and answer Y if prompted.
pause

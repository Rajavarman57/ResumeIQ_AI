@echo off
title ResumeIQ AI Launcher
color 0A
echo.
echo  ============================================
echo    ResumeIQ - AI Recruitment Platform
echo    Powered by Claude AI (Anthropic)
echo  ============================================
echo.

:: Load from .env if it exists
if exist "%~dp0.env" (
  for /f "usebackq tokens=1,2 delims==" %%i in ("%~dp0.env") do (
    if "%%i"=="ANTHROPIC_API_KEY" set ANTHROPIC_API_KEY=%%j
  )
)

if "%ANTHROPIC_API_KEY%"=="" (
  echo  [WARN] ANTHROPIC_API_KEY is not set in environment or .env file.
  echo  AI features will not work without it.
  echo  Get your key at: https://console.anthropic.com
  echo.
  set /p ANTHROPIC_API_KEY=Enter your API key (or press Enter to skip): 
)

java -version >nul 2>&1
if errorlevel 1 ( echo [ERROR] Java not found. Install Java 17+ & pause & exit /b 1 )
node --version >nul 2>&1
if errorlevel 1 ( echo [ERROR] Node.js not found. Install from nodejs.org & pause & exit /b 1 )

echo  [OK] Prerequisites found
echo.
echo  Starting Backend with AI...
start "ResumeIQ Backend" cmd /k "cd /d %~dp0backend && set ANTHROPIC_API_KEY=%ANTHROPIC_API_KEY% && mvn spring-boot:run"
echo  Waiting 25 seconds for Spring Boot...
timeout /t 25 /nobreak >nul
echo  Starting Frontend...
start "ResumeIQ Frontend" cmd /k "cd /d %~dp0frontend && node server.js"
timeout /t 3 /nobreak >nul
echo.
echo  Backend:  http://localhost:8080/api
echo  Frontend: http://localhost:5173
echo.
start http://localhost:5173
pause

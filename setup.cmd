@echo off
title ResumeIQ AI Launcher
color 0A
echo.
echo  ============================================
echo    ResumeIQ - AI Recruitment Platform
echo    Powered by Gemini AI (Google)
echo  ============================================
echo.

REM Load from .env if it exists
if not exist "%~dp0.env" goto env_done
for /f "tokens=2 delims==" %%A in ('findstr /i "GEMINI_API_KEY" "%~dp0.env"') do (
  set GEMINI_API_KEY=%%A
)
:env_done

if not "%GEMINI_API_KEY%"=="" goto key_done
echo  [WARN] GEMINI_API_KEY is not set in environment or .env file.
echo  AI features will not work without it.
echo  Get your key at: https://aistudio.google.com
echo.
set /p GEMINI_API_KEY=Enter your API key (or press Enter to skip): 
:key_done

java -version >nul 2>&1
if not errorlevel 1 goto java_ok
echo [ERROR] Java not found. Install Java 17+
pause
exit /b 1
:java_ok

node -v >nul 2>&1
if not errorlevel 1 goto node_ok
echo [ERROR] Node.js not found. Install from nodejs.org
pause
exit /b 1
:node_ok

echo  [OK] Prerequisites found
echo.
echo  Starting Backend with AI...
start "ResumeIQ Backend" cmd /k "cd /d %~dp0backend && set GEMINI_API_KEY=%GEMINI_API_KEY% && mvn spring-boot:run"
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

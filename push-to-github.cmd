@echo off
title ResumeIQ GitHub Push
color 0A
echo.
echo  ResumeIQ -- Push to GitHub
echo  ===========================
echo.
set /p GH_USER=GitHub username: 
set /p GH_REPO=Repository name [resumeiq]: 
if "%GH_REPO%"=="" set GH_REPO=resumeiq

set REMOTE=https://github.com/%GH_USER%/%GH_REPO%.git
echo.
echo  Remote: %REMOTE%
echo.

if not exist ".git" (
    git init
    git branch -M main
)

git add .
set /p MSG=Commit message [feat: initial ResumeIQ AI platform]: 
if "%MSG%"=="" set MSG=feat: initial ResumeIQ AI platform
git commit -m "%MSG%"

git remote remove origin 2>nul
git remote add origin %REMOTE%
git push -u origin main

echo.
echo  [OK] Pushed to https://github.com/%GH_USER%/%GH_REPO%
echo.
echo  Next - Add these GitHub Secrets:
echo    Settings - Secrets - Actions - New repository secret
echo    ANTHROPIC_API_KEY  = your Anthropic key
echo    DOCKER_USERNAME    = your Docker Hub username
echo    DOCKER_PASSWORD    = your Docker Hub password
echo.
pause

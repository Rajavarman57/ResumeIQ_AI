#!/bin/bash
# ── ResumeIQ GitHub Push Script ───────────────────────────────
set -e
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

echo ""
echo "  ╔═══════════════════════════════════════╗"
echo "  ║   ResumeIQ — Push to GitHub           ║"
echo "  ╚═══════════════════════════════════════╝"
echo ""

# Ask for GitHub details
read -p "  GitHub username: " GH_USER
read -p "  Repository name [resumeiq]: " GH_REPO
GH_REPO=${GH_REPO:-resumeiq}
read -p "  Use HTTPS or SSH? [https/ssh]: " GH_PROTO
GH_PROTO=${GH_PROTO:-https}

if [ "$GH_PROTO" = "ssh" ]; then
    REMOTE="git@github.com:${GH_USER}/${GH_REPO}.git"
else
    REMOTE="https://github.com/${GH_USER}/${GH_REPO}.git"
fi

echo ""
echo -e "  ${YELLOW}Remote:${NC} $REMOTE"
echo ""

# Init git if needed
if [ ! -d ".git" ]; then
    echo -e "  ${GREEN}Initialising git repository...${NC}"
    git init
    git branch -M main
fi

# Stage all files
git add .
git status --short

echo ""
read -p "  Commit message [feat: initial ResumeIQ AI platform]: " MSG
MSG=${MSG:-"feat: initial ResumeIQ AI platform"}

git commit -m "$MSG"

# Set remote
if git remote | grep -q origin; then
    git remote set-url origin "$REMOTE"
else
    git remote add origin "$REMOTE"
fi

echo ""
echo -e "  ${GREEN}Pushing to GitHub...${NC}"
git push -u origin main

echo ""
echo -e "  ${GREEN}✓ Successfully pushed to:${NC}"
echo "    https://github.com/${GH_USER}/${GH_REPO}"
echo ""
echo -e "  ${YELLOW}Next steps:${NC}"
echo "  1. Go to: https://github.com/${GH_USER}/${GH_REPO}/settings/secrets/actions"
echo "  2. Add secret: ANTHROPIC_API_KEY = your key"
echo "  3. Add secret: DOCKER_USERNAME   = your Docker Hub username"
echo "  4. Add secret: DOCKER_PASSWORD   = your Docker Hub password"
echo ""

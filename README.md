# ResumeIQ — AI-Powered Recruitment Platform

> Full-stack recruitment platform powered by **Gemini AI**. Parses resumes, semantically scores candidates, generates personalised improvement suggestions, provides an AI chat interface per candidate, sends email notifications, and ships with full Docker support.

---

## Features

| Feature | Description |
|---|---|
| 🤖 **AI Resume Parsing** | Gemini extracts name, skills, experience, education, strengths |
| 📊 **AI Candidate Scoring** | Semantic match score + ATS score per job role |
| 🔍 **Skill Gap Analysis** | Missing skills with priority and context |
| 💡 **AI Suggestions** | Personalised resume improvements, summary rewrite, interview tips |
| 💬 **Chat with Resume** | Ask Gemini anything about a candidate in real-time |
| 🔄 **Alternative Role Matching** | AI suggests other roles the candidate may suit |
| 🧠 **AI JD Parsing** | Paste any job description — Gemini extracts skills automatically |
| 📧 **Email Notifications** | Candidates receive styled HTML emails on status changes |
| 📤 **CSV Export** | One-click hiring report download |
| 🐳 **Docker Ready** | Full docker-compose with MySQL, backend, and frontend |
| 🔁 **GitHub Actions CI/CD** | Automated build, test, and Docker push pipeline |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Vanilla JS, HTML5, CSS3 |
| Backend | Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA |
| AI | Google Gemini API (`gemini-1.5-flash`) |
| Email | Spring Mail + Thymeleaf HTML templates |
| Chat | REST API with persistent history (Gemini AI) |
| Database | H2 (dev) / MySQL 8 (prod/Docker) |
| Resume Parsing | Apache PDFBox (PDF), Apache POI (DOCX) |
| Auth | JWT (jjwt 0.11.5) |
| Build | Maven 3.9 |
| Container | Docker + Docker Compose |
| CI/CD | GitHub Actions |

---

## Quick Start

### Option 1 — Local (Dev)

```bash
# 1. Clone the repo
git clone https://github.com/YOUR_USERNAME/resumeiq.git
cd resumeiq

# 2. Set your API key
export GEMINI_API_KEY=your-gemini-key-here    # Mac/Linux
set GEMINI_API_KEY=your-gemini-key-here        # Windows

# 3. Start backend
cd backend && mvn spring-boot:run

# 4. Start frontend (new terminal)
cd frontend && node server.js

# 5. Open
open http://localhost:5173
```

### Option 2 — Docker Compose (Recommended)

```bash
# 1. Copy env file and fill in values
cp .env.example .env
# Edit .env — set GEMINI_API_KEY at minimum

# 2. Build and start everything
docker compose up --build

# 3. Open
open http://localhost:5173
```

All three services start automatically:
- **MySQL** on port 3306
- **Backend** on port 8080
- **Frontend** on port 5173

---

## Default Credentials

| Role | Email | Password |
|---|---|---|
| Admin | admin@resumeiq.local | admin123 |
| Recruiter | recruiter@resumeiq.local | recruit123 |

---

## Email Notifications Setup

Email is **disabled by default**. To enable:

1. Create a Gmail App Password: Google Account → Security → 2FA → App Passwords
2. Set in `.env`:
```env
MAIL_ENABLED=true
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-16-char-app-password
MAIL_FROM=noreply@resumeiq.local
```
3. Candidates receive emails when:
   - Resume is uploaded and AI-scored
   - Status is changed (Selected, Rejected, etc.)

---

## Chat with Resume

Every candidate card has a **Chat** button. Click it to open an AI chat panel where you can ask Gemini questions like:

- *"Is this candidate a strong fit?"*
- *"What are their top 3 strengths?"*
- *"What interview questions should I ask?"*
- *"Can you summarise their background in 3 sentences?"*

Gemini has read the resume, match scores, and skill gaps in full context.

---

## GitHub Setup

```bash
# Initialise and push
git init
git add .
git commit -m "feat: initial ResumeIQ with Gemini AI"
git remote add origin https://github.com/YOUR_USERNAME/resumeiq.git
git branch -M main
git push -u origin main
```

### Required GitHub Secrets

Go to **Settings → Secrets and variables → Actions** and add:

| Secret | Value |
|---|---|
| `GEMINI_API_KEY` | Your Google Gemini API key |
| `DOCKER_USERNAME` | Your Docker Hub username |
| `DOCKER_PASSWORD` | Your Docker Hub password or access token |

---

## Project Structure

```
ResumeIQ/
├── .github/workflows/ci.yml        ← GitHub Actions CI/CD
├── docker-compose.yml               ← Full stack Docker setup
├── .env.example                     ← Environment variable template
├── .gitignore
│
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/resumeiq/
│       ├── config/
│       │   ├── SecurityConfig.java
│       │   ├── AppConfig.java
│       │   ├── AsyncConfig.java     ← Enables async email
│       │   └── WebSocketConfig.java ← WebSocket for chat
│       ├── controller/
│       │   ├── AuthController.java
│       │   ├── JobController.java       ← Includes /ai-parse
│       │   ├── ResumeController.java    ← Upload → AI pipeline
│       │   ├── CandidateController.java ← Rankings + email trigger
│       │   ├── ChatController.java      ← Chat endpoints
│       │   └── AnalyticsController.java
│       ├── model/
│       │   ├── User.java
│       │   ├── JobRole.java
│       │   ├── Resume.java
│       │   ├── CandidateScore.java
│       │   ├── CandidateStatus.java
│       │   └── ChatMessage.java         ← Chat history
│       ├── service/
│       │   ├── GeminiAIService.java         ← Raw Gemini HTTP client
│       │   ├── AIResumeAnalysisService.java ← All AI prompts
│       │   ├── MatchingService.java         ← Orchestrates AI scoring
│       │   ├── ResumeParserService.java     ← PDFBox / POI
│       │   ├── EmailService.java            ← Thymeleaf + SMTP
│       │   └── ChatService.java             ← Gemini chat sessions
│       ├── security/ JwtUtil.java, JwtFilter.java
│       └── resources/
│           ├── application.properties
│           ├── application-prod.properties
│           └── templates/email/
│               ├── status-update.html
│               └── resume-uploaded.html
│
└── frontend/
    ├── Dockerfile
    ├── package.json
    ├── server.js
    ├── index.html
    ├── css/style.css
    └── js/
        ├── api.js
        ├── auth.js
        ├── router.js
        ├── components.js
        ├── app.js
        └── pages/
            ├── login.js
            ├── dashboard.js
            ├── jobs.js
            ├── upload.js
            ├── candidates.js
            ├── analytics.js
            └── chat.js              ← AI Chat UI
```

---

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| POST | /api/auth/login | Login |
| POST | /api/auth/register | Register |
| GET | /api/jobs | All jobs |
| POST | /api/jobs | Create job |
| POST | /api/jobs/ai-parse | AI parse job description |
| POST | /api/resumes/upload | Upload + AI analyse resume |
| POST | /api/resumes/{id}/reanalyze | Re-run AI on existing resume |
| GET | /api/candidates/ranked/{jobId} | AI-ranked candidate list |
| GET | /api/candidates/score/{rid}/{jid} | Full AI score detail |
| PUT | /api/candidates/status/{rid}/{jid} | Update status + send email |
| GET | /api/candidates/alternatives/{rid}/{jid} | AI alternative roles |
| POST | /api/chat/{resumeId}/{jobId} | Send chat message |
| GET | /api/chat/{resumeId}/{jobId} | Get chat history |
| DELETE | /api/chat/{resumeId}/{jobId} | Clear chat |
| GET | /api/chat/suggestions/{rid}/{jid} | Suggested questions |
| GET | /api/analytics/dashboard | Dashboard data |
| GET | /api/analytics/export/csv | Download CSV report |

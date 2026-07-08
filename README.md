# Ledger — Weekly Report Generator & Team Dashboard

A full-stack app for submitting structured weekly work reports and giving managers
a consolidated, filterable dashboard across the whole team.

**Stack:** Spring Boot 3 (Java 17) · React 18 (Vite) · MySQL 8

```
weekly-report-app/
├── backend/    Spring Boot REST API
├── frontend/   React + Vite SPA
├── ER-DIAGRAM.md
└── docker-compose.yml   (MySQL only, for local dev)
```

## 1. Prerequisites

- Java 17+ and Maven 3.9+
- Node.js 18+ and npm
- MySQL 8 (or Docker, to run it in a container — see below)

## 2. Set up the database

**Option A — Docker (recommended):**

```bash
docker compose up -d
```

This starts MySQL 8 on `localhost:3306` with a database called `weekly_report_db`,
user `root`, password `root` (matches the backend's defaults).

**Option B — Local MySQL install:**

```sql
CREATE DATABASE weekly_report_db;
```

The backend auto-creates all tables on first run (`ddl-auto: update`), so no manual
schema/migration step is needed.

## 3. Run the backend

```bash
cd backend
# Optional: override DB credentials / JWT secret via env vars instead of the defaults
export DB_USERNAME=root
export DB_PASSWORD=root
export JWT_SECRET=change-this-to-a-long-random-string-in-production

mvn spring-boot:run
```

The API starts on **http://localhost:8080**.

### Optional: enable the AI Chat Assistant

The "Good to Have" AI assistant (manager-only) calls the Anthropic API. It's fully
optional — the app works without it, and the endpoint returns a friendly message if
unconfigured.

```bash
export ANTHROPIC_API_KEY=sk-ant-...
```

## 4. Run the frontend

```bash
cd frontend
npm install
cp .env.example .env   # defaults to http://localhost:8080, edit if needed
npm run dev
```

The app starts on **http://localhost:5173**.

## 5. First-time use

1. Go to `http://localhost:5173/register`.
2. Create one account with role **Manager** (for yourself) and one or more with
   role **Team Member** (or have teammates register themselves).
3. Sign in as the manager and add at least one **Project** under *Projects* — team
   members need a project to attach their first report to.
4. Sign in as a team member and submit a weekly report from *My Reports*.
5. Back in the manager account, check *Team Dashboard* and *Team Reports*.

## Architecture overview

- **Auth**: stateless JWT auth. `POST /api/auth/register` and `/login` issue a
  signed token (`app.jwt.secret`); every other request must send
  `Authorization: Bearer <token>`. Passwords are hashed with BCrypt.
- **Roles**: a single `role` enum on the `User` entity (`TEAM_MEMBER` / `MANAGER`).
  Spring Security enforces role checks at the URL level (`SecurityConfig`) — e.g.
  `/api/dashboard/**` and `/api/reports/team/**` require `ROLE_MANAGER` — plus
  ownership checks in `ReportService` so a team member can only edit their own
  reports.
- **Fixed report shape**: `WeeklyReport` is a plain relational entity with a fixed
  column set (not a flexible/JSON schema), so every user's report has identical
  fields in identical order — this is what makes reports comparable across the
  team dashboard.
- **Dashboard aggregation**: `DashboardService` computes summary metrics and chart
  data server-side per request (submission compliance, trend over the last 8
  weeks, workload by project, recent activity) rather than shipping raw report
  rows to the frontend for client-side aggregation.
- **AI Chat Assistant (bonus)**: `AiAssistantService` does lightweight
  retrieval — it pulls the last 8 weeks of report rows from MySQL, flattens them
  into plain text, and passes that as context to Claude alongside the manager's
  question. No vector DB is needed at this scale. Only report content and member/
  project names are sent — never credentials or emails. See inline comments in
  `AiAssistantService.java` for more on the data-privacy approach.

## API summary

| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| GET/POST | `/api/projects` | Any authenticated user (create/edit/delete = Manager) |
| POST/PUT/DELETE | `/api/reports`, `/api/reports/{id}` | Owner or Manager |
| GET | `/api/reports/mine` | Any authenticated user |
| GET | `/api/reports/team` | Manager (supports `userId`, `projectId`, `status`, `from`, `to` filters) |
| GET | `/api/dashboard/summary` | Manager |
| GET | `/api/users/team-members` | Manager |
| POST | `/api/ai/chat` | Manager |

## Database design

See [ER-DIAGRAM.md](./ER-DIAGRAM.md) for the full entity-relationship diagram and
design rationale.

## Possible future improvements

- Email/Slack reminders for reports still pending close to the weekly deadline.
- Manager-configurable per-project custom fields (currently intentionally fixed
  and uniform, per the assignment brief).
- Pagination and server-side sorting on `/api/reports/team` for larger teams.
- Refresh tokens / shorter-lived access tokens instead of a single 24h JWT.
- Assigning team members to specific projects (mentioned as optional in the brief).

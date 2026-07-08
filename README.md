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

The "Good to Have" AI assistant (manager-only) calls the Google Gemini API. It's
fully optional — the app works without it, and the endpoint returns a friendly
message if unconfigured. Gemini has a permanent free tier with no card required —
get a key at https://aistudio.google.com/apikey.

```bash
export GEMINI_API_KEY=your-key-here
```

## 4. Run the frontend

```bash
cd frontend
npm install
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
- **Project membership & visibility**: `Project` has a many-to-many `members`
  relationship to `User` (join table `project_members`). When creating or
  editing a project, a manager can optionally assign specific team members to
  it. A project with **no** members assigned is open to everyone (backward
  compatible default); a project **with** members assigned is only visible to
  — and reportable-against by — those members (managers always see every
  project). Enforced both when listing projects
  (`ProjectService.getVisibleForCurrentUser`) and when a report is
  created/updated (`ReportService` rejects submissions against a project the
  user isn't assigned to).
- **Dashboard aggregation**: `DashboardService` computes summary metrics and chart
  data server-side per request (submission compliance, trend over the last 8
  weeks, workload by project, recent activity) rather than shipping raw report
  rows to the frontend for client-side aggregation.
- **AI Chat Assistant (bonus)**: see the dedicated section below.

## AI Chat Assistant — approach, prompts, and data privacy

Manager-only feature (`/api/ai/chat` and `/api/ai/summary`, both gated by
`ROLE_MANAGER` in `SecurityConfig`), backed by the **Google Gemini API**
(`gemini-2.5-flash` by default — configurable via `app.ai.model`). Gemini was
chosen because it has a genuinely free, permanent tier with no billing/card
required, making it easy for evaluators to run this locally.

**Approach — lightweight RAG, no vector store.** At the scale of a single
team's weekly reports, the relevant history fits comfortably in one prompt, so
`AiAssistantService` queries MySQL directly for reports in a time window (8
weeks for Q&A, 4 weeks for the summary), flattens each into a compact
plain-text block (member, project, week, status, completed/planned/blockers),
and passes that as a `systemInstruction` to Gemini's `generateContent`
endpoint. No embeddings pipeline or extra infrastructure required, and every
answer is traceable back to real rows in the `weekly_reports` table.

**Two capabilities:**
- **Conversational Q&A** — `POST /api/ai/chat` with `{ "question": "..." }`.
  Free-form questions like *"What did the design team work on last week?"*
  (project names double as team names).
- **AI-generated team summary** — `GET /api/ai/summary`. A fixed three-section
  report: Completed work / Recurring blockers / Workload imbalances, built
  from the last 4 weeks of data. Triggered from the "Generate team summary"
  button in the chat widget.

**Prompt design.** Both prompts explicitly instruct the model to answer *only*
from the supplied report data and to say so plainly if the data doesn't cover
the question, rather than guessing — this keeps answers grounded and avoids
inventing project/people details. The summary prompt fixes the three section
headers so output is predictable and scannable regardless of what's in the
data that week.

**Data-privacy considerations:**
- Restricted to the `MANAGER` role only.
- Only report content for the requested window is sent — never the whole
  database, and never other users' account data.
- The only personal identifier sent is full name; emails, password hashes, and
  auth tokens are never part of the prompt.
- Read-only — nothing is written back to the database from this feature.
- Chat history lives only in the browser widget's React state; nothing is
  persisted server-side, and each request is stateless.
- **Free-tier caveat**: Gemini's free tier may use prompt/response data to
  improve Google's products (per the Gemini API terms). For report data that's
  genuinely sensitive, use a paid-tier key instead, which carries a different
  data-use contract — see https://ai.google.dev/gemini-api/terms.
- If `GEMINI_API_KEY` isn't set, the feature degrades gracefully (returns a
  message plus the raw matching context) instead of breaking the rest of the app.

## API summary

| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| GET | `/api/projects` | Any authenticated user — response scoped by role/membership |
| POST/PUT/DELETE | `/api/projects/{id}` | Manager (accepts `memberIds` to assign the project) |
| POST/PUT/DELETE | `/api/reports`, `/api/reports/{id}` | Owner or Manager (project must be accessible to the user) |
| GET | `/api/reports/mine` | Any authenticated user |
| GET | `/api/reports/team` | Manager (supports `userId`, `projectId`, `status`, `from`, `to` filters) |
| GET | `/api/dashboard/summary` | Manager |
| GET | `/api/users/team-members` | Manager |
| POST | `/api/ai/chat` | Manager |
| GET | `/api/ai/summary` | Manager |

## Database design

See [ER-DIAGRAM.md](./ER-DIAGRAM.md) for the full entity-relationship diagram and
design rationale.

## Possible future improvements

- Email/Slack reminders for reports still pending close to the weekly deadline.
- Manager-configurable per-project custom fields (currently intentionally fixed
  and uniform, per the assignment brief).
- Pagination and server-side sorting on `/api/reports/team` for larger teams.
- Refresh tokens / shorter-lived access tokens instead of a single 24h JWT.
- Function-calling/tool-use for the AI assistant (e.g. letting it query specific
  members or date ranges on demand) instead of a fixed context window.

# Vijaya Theatre — Ticketing and Operations Management System

Spring Boot REST API (`backend/`) + React SPA (`frontend/`). The scope and phasing
are defined in [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md).

## Running locally

```bash
docker compose up -d                  # Postgres on host port 5433
cd backend && ./gradlew bootRun       # API on :8080
cd frontend && npm ci && npm run dev  # SPA on :5173
```

Backend tests use Testcontainers, so Docker must be running: `cd backend && ./gradlew test`.

## Branching workflow

| Branch | Purpose |
|---|---|
| `main` | Released, demo-ready code only. Updated only by merging `dev` through a release PR. |
| `dev` | Integration branch. Every feature lands here first. |
| `feature/<name>` | One slice of work (one use case or FR), branched from `dev`. |
| `fix/<name>`, `chore/<name>` | Bug fixes and tooling/config changes, also branched from `dev`. |

1. Branch from an up-to-date `dev`: `git switch dev && git pull && git switch -c feature/<name>`.
2. Commit in small, descriptive steps that reference the FR/UC/BR ids they implement.
3. Run the tests locally, then push and open a PR into `dev`.
4. Merge only after CI (Backend CI / Frontend CI) is green. Delete the feature branch afterwards.
5. When `dev` reaches a stable milestone, open a release PR `dev` → `main` and merge it after CI passes.

`main` and `dev` are protected on GitHub. Nobody can push to them directly, and that includes admins. Every change goes
through a PR, and a PR can only merge once both the **Backend CI** and **Frontend CI** checks pass.

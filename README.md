# Project playbook

Reusable **GitHub template** for Ethan’s app-dev process. **Process + stubs only — not an application.**

Create a new product repo with GitHub **Use this template**. Then fill `PLAN.md` and start the current phase.

## AI entry

**Any AI reads [`PLAN.md`](./PLAN.md) first** → [`decisions/CURRENT.md`](./decisions/CURRENT.md) → newest [`HANDOFF.md`](./HANDOFF.md). Rules: [`AGENTS.md`](./AGENTS.md).

## What you get

| Path | Purpose |
| --- | --- |
| `PLAN.md` | Current phase, deployment kind, phase index, token rules |
| `decisions/` | Living snapshot + changelog + **A1…D4** process |
| `HANDOFF.md` | Cross-AI board (Cursor ↔ Claude ↔ Grok ↔ Cloud Agent) |
| `stack/` | Stack **by deployment kind** — not one flat list |
| `connectors/` | GitHub + Figma only (process-useful) |
| `docker-compose.yml` + `scripts/` + `.env.example` | Local Postgres 16 ≈ Azure names |
| `deploy/README.md` | Local + Azure checklist |

## Deployment kind

- **`app-web` (default):** web → maybe native. Use **100%** of the playbook ([`stack/APP-WEB.md`](./stack/APP-WEB.md) + phases A1–D4). Next.js / TypeScript / Tailwind / Prisma / Postgres 16. Azure: Container Apps, Flexible Server, ACS Email, Blob, Key Vault. Local ≈ Azure.
- **Anything else:** [`stack/OTHER-DEPLOYMENTS.md`](./stack/OTHER-DEPLOYMENTS.md) — short stubs, fill when needed. No fake A1–D4.

## Phase labels (app-web)

**A1** identity+score/logging+Postgres Prisma · **A2** local Docker≈Azure · **A3** soft-launch flags · **A4** theme/shell  
**B1** log UX · **B2** boards/friends · **B3** profile units/timezone/photos · **B4** edit/swipe-delete  
**C1** admin · **C2** ACS Email+OTP+tickets · **C3** IP soft-lock · **C4** anti-cheat+Report  
**D1** charts/achievements · **D2** admin KPIs · **D3** passkeys · **D4** payments then native last

## Token rules

Lock decisions before code. Core data plane first. Cloud Agent for repo code. Append decisions (newest first). No secret commits.

## GitHub template flag

This repo is meant to be a GitHub **template repository** so new products start with **Use this template**. `gh repo edit --template` from the filling agent returned **HTTP 403** (integration cannot change that setting). Ethan: **Settings → General → Template repository**.

## After “Use this template”

1. Set **Name** and **Deployment kind** in `PLAN.md` and `decisions/CURRENT.md`.
2. If `app-web`, begin **A1**. Do not copy Bragline (or any other product) application source.
3. Typical Windows DevWork: `DESKTOP-KAAGPFG`. Optional Aeternum/Tailscale for remote QA — [`stack/LOCAL-DEV.md`](./stack/LOCAL-DEV.md).

## Local stubs (once the product exists)

```bash
cp .env.example .env
./scripts/dev-up.sh
./scripts/db-migrate.sh
./scripts/db-seed.sh
```

Windows: `pwsh -File scripts/dev-up.ps1`. See [`deploy/README.md`](./deploy/README.md).

# PLAN — read this first

> **Any AI (Cursor, Claude, Grok, Cloud Agent, other):** read this file → [`decisions/CURRENT.md`](./decisions/CURRENT.md) → the newest entry in [`HANDOFF.md`](./HANDOFF.md). Then work. Do not scan the whole repo “for context.”

This repository is a **reusable process template**, not an application. Clone it (GitHub “Use this template”), fill the placeholders, then implement the product in the new repo.

---

## Project

| Field | Value |
| --- | --- |
| **Name** | _TBD — lock in `decisions/CURRENT.md`_ |
| **Owner** | Ethan Stidley (`estidley`) |
| **Deployment kind** | **`app-web`** (default) |
| **Current phase** | **A1** |
| **Phase file** | [`decisions/phases/A-foundation.md`](./decisions/phases/A-foundation.md) |

If **deployment kind** is not `app-web`, do **not** run the A1–D4 playbook. Use [`stack/OTHER-DEPLOYMENTS.md`](./stack/OTHER-DEPLOYMENTS.md) stubs and fill process only when the project actually needs it.

---

## Phase index (`app-web` only)

Full Bragline-style process. Labels are **A1, A2… B1, B2…** — use those IDs in commits, HANDOFF, and decisions.

### A — Foundation → [`decisions/phases/A-foundation.md`](./decisions/phases/A-foundation.md)

| ID | Slice |
| --- | --- |
| **A1** | Identity + core score/logging + Postgres-shaped Prisma |
| **A2** | Local Docker ≈ Railway |
| **A3** | Soft-launch flags |
| **A4** | Theme / shell |

### B — Product surface → [`decisions/phases/B-product-surface.md`](./decisions/phases/B-product-surface.md)

| ID | Slice |
| --- | --- |
| **B1** | Log UX |
| **B2** | Boards / friends |
| **B3** | Profile units / timezone / photos |
| **B4** | Edit / swipe-delete |

### C — Ops / admin → [`decisions/phases/C-ops-admin.md`](./decisions/phases/C-ops-admin.md)

| ID | Slice |
| --- | --- |
| **C1** | Admin (allowlist) |
| **C2** | Resend Email + OTP + tickets |
| **C3** | IP soft-lock |
| **C4** | Anti-cheat + Report |

### D — Polish / ship → [`decisions/phases/D-polish-ship.md`](./decisions/phases/D-polish-ship.md)

| ID | Slice |
| --- | --- |
| **D1** | Charts / achievements |
| **D2** | Admin KPIs |
| **D3** | Passkeys |
| **D4** | Payments, then native last |

---

## Where to look (do not invent)

| Need | Read |
| --- | --- |
| Locked product/tech choices | [`decisions/CURRENT.md`](./decisions/CURRENT.md) + newest [`decisions/CHANGELOG.md`](./decisions/CHANGELOG.md) |
| How to pick a stack | [`stack/README.md`](./stack/README.md) |
| **`app-web` full playbook** | [`stack/APP-WEB.md`](./stack/APP-WEB.md) |
| Local machine ≈ Railway | [`stack/LOCAL-DEV.md`](./stack/LOCAL-DEV.md) |
| Railway deploy checklist | [`stack/RAILWAY.md`](./stack/RAILWAY.md) + [`deploy/README.md`](./deploy/README.md) |
| Azure (superseded) | [`stack/AZURE.md`](./stack/AZURE.md) — not the default |
| Process-useful connectors | [`connectors/README.md`](./connectors/README.md) — GitHub + Figma only |
| Cross-AI board | [`HANDOFF.md`](./HANDOFF.md) |
| Agent rules | [`AGENTS.md`](./AGENTS.md) |

---

## Token rules (non-negotiable)

1. **Lock decisions before code.** If a choice is not in `decisions/`, write the decision first (context → decision → why → implications), then implement.
2. **Core data plane first.** Identity, primary log, score formula, Postgres-shaped Prisma. Do not skip to payments, native apps, or polish.
3. **Cloud Agent for repo code.** Source of truth is GitHub. Use Cloud Agents / PRs for implementation in the product repo — not paste-bin apps.
4. **Append decisions.** Newest changelog entry at the **top**. Do not rewrite history; supersede with a new entry.
5. **No secret commits.** `.env` is gitignored. Values live in local `.env` or Railway service variables.

---

## After cloning this template

1. Set **Name** and **Deployment kind** in this file and in `decisions/CURRENT.md`.
2. If `app-web`, start at **A1**. If not, stub only — see `stack/OTHER-DEPLOYMENTS.md`.
3. Keep **Current phase** accurate. Advance it when the phase’s **done-when** is true.
4. Seed `HANDOFF.md` when handing to another AI.

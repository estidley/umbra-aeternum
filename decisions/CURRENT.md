# Current snapshot

Fill this when you clone the template. Until then, values are placeholders. **Read this after `PLAN.md`.**

| Item | Status |
| --- | --- |
| Name | _TBD_ |
| Deployment kind | **`app-web`** (default). Other kinds: see `stack/OTHER-DEPLOYMENTS.md` — do not invent A–D phases. |
| Current phase | **A1** — identity + core score/logging + Postgres-shaped Prisma |
| V1 surface | Mobile-first **web**. Desktop / regular web shell at `lg`. Native iOS/Android **last** (D4). |
| Code | Original implementation only — do not clone third-party app repos |
| Token posture | Thin prompts. Lock decisions before code. Core data plane first. |
| GitHub | _this product’s repo URL_ |
| Theme | _lock in Figma before deep UI (A4)_ |
| Domain | _TBD_ |
| Cloud | **Railway** — one Railpack-built Next.js service + Railway PostgreSQL + volume at `/data` |
| Data plane | **Postgres 16**: Docker locally, Railway PostgreSQL in prod. Same env **names**. SQLite is fallback only, never Railway. |
| Score / core value | _lock formula in CHANGELOG before coding A1. Never mislabel in UI._ |
| Logging | Manual primary create in V1. FAB → `/log`. Kind toggle **on that page**. |
| Boards | Friends + Global (trusted vs noisier). Visibility honored. No prizes in soft launch. |
| Visibility | Private \| Friends only \| Public |
| Social | Emoji reactions only — **no messaging**. Support is tickets, not chat. |
| Admin | Ethan-only hard allowlist (`ADMIN_EMAILS`). Not a grantable user role. |
| Email / auth | **Resend HTTPS API** (`RESEND_API_KEY`, `EMAIL_FROM`). Optional Gmail SMTP fallback when Resend is unset. ACS Email is superseded / unused. Email OTP / magic-link. **No SMS.** Passkeys later (D3). |
| Uploads | Prod: Railway volume (`PHOTO_UPLOAD_DIR=/data/uploads`). Local: `uploads/` when unset. Azure Blob dormant / optional only. |
| Monetization | Soft launch: full entitlements, no charge. `LAUNCH_UNLOCK_PRO=true`, `BILLING_ENABLED=false`. Ends only via **manual kill switch**. No calendar end date. No BETA badge. |
| Payments | **D4**, after QA. Keep price constants from the start; do not wire Stripe early. |
| Native | **Last.** Web look locked first. |
| Local machine | Typical Windows DevWork: `DESKTOP-KAAGPFG`. Optional Aeternum / Tailscale for remote QA. |
| Handoff | `decisions/` + `HANDOFF.md` — not chat history |

## Stack (app-web)

Next.js App Router + TypeScript + Tailwind + Prisma + **Postgres 16** + email/password JWT cookies. Railway: Railpack app, Postgres, volume uploads, Resend HTTPS API. Details: [`stack/APP-WEB.md`](../stack/APP-WEB.md).

## Explicit non-goals (until a decision supersedes)

- Inventing a second stack or a connector we have not used as a process step
- Day-to-day assistants (Gmail, Calendar) as playbook steps
- Fake MCP for Railway or Azure
- Treating Azure Container Apps / Flexible Server / ACS / Key Vault as the default ship path
- GPS / wearable sync / extra fields that do not feed the core score (V1)
- Messaging / DMs / comment threads
- Selling user data
- SMS / phone OTP
- Grantable admin roles
- Third-party product-analytics SaaS (admin KPIs stay in-app / Railway)
- Per-user PII on admin KPI dashboards
- Native apps before D4
- Copying Bragline or any other product’s application source

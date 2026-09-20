# app-web — full playbook path

This is the **primary** stack and process. Use **100%** of `decisions/phases/` (A1–D4). Genericized from how Bragline **ships** (Railway + Resend) — **do not copy that app’s source**.

## Runtime

| Layer | Choice |
| --- | --- |
| App | **Next.js** (App Router) + **TypeScript** + **Tailwind CSS** |
| Data | **Prisma** + **Postgres 16** |
| Auth (V1) | Email/password, signed JWT cookie (`jose` + `bcryptjs` or equivalent) |
| Local DB | Docker Compose Postgres 16 — [`LOCAL-DEV.md`](./LOCAL-DEV.md) |
| Prod DB | **Railway PostgreSQL** — private `DATABASE_URL`, platform-managed storage |
| Host | **Railway** — one Railpack-built Next.js service |
| Mail | **Resend HTTPS API** (`RESEND_API_KEY`, `EMAIL_FROM`). Optional Gmail SMTP fallback when Resend is unset. ACS Email is superseded / unused. No SMS. |
| Files | Railway **volume** at `/data` with `PHOTO_UPLOAD_DIR=/data/uploads` in prod; local `uploads/` when unset. Azure Blob is dormant / optional only. |
| Secrets | Railway **service variables** — never git |

SQLite is a documented **fallback** only (`file:` URL + explicit allow flag + Prisma provider switch). Not Railway. Not the default.

## Local ≈ Railway

Same env **names** in `.env.example` and in Railway service variables:

`DATABASE_URL`, `AUTH_SECRET`, `ADMIN_EMAILS`, `LAUNCH_UNLOCK_PRO`, `BILLING_ENABLED`, `PHOTO_UPLOAD_DIR`, later `RESEND_API_KEY`, `EMAIL_FROM`. Optional: `GMAIL_USER`, `GMAIL_APP_PASSWORD`. Dormant: `AZURE_STORAGE_*`. Superseded: `ACS_EMAIL_*`.

Scripts: `scripts/dev-up.sh` / `dev-up.ps1`, `db-migrate.sh`, `db-seed.sh`. Deploy stub: `railway.json`. See [`deploy/README.md`](../deploy/README.md).

## Process (do not skip)

```
A1 identity + score/logging + Postgres Prisma
A2 local Docker ≈ Railway
A3 soft-launch flags
A4 theme/shell (Figma first)
B1 log UX
B2 boards/friends
B3 profile units/timezone/photos
B4 edit/swipe-delete
C1 admin allowlist
C2 Resend Email + OTP + tickets
C3 IP soft-lock
C4 anti-cheat + Report
D1 charts/achievements
D2 admin KPIs
D3 passkeys
D4 payments → native last
```

Token rules: lock decisions before code; core data plane first; Cloud Agent for repo code; append decisions; no secret commits. See [`PLAN.md`](../PLAN.md).

## Connectors used as process steps

- **GitHub** — source, PRs, Cloud Agents ([`connectors/github.md`](../connectors/github.md)).
- **Figma** — mocks / accents before locking UI ([`connectors/figma.md`](../connectors/figma.md)).

Railway is the deploy checklist in [`RAILWAY.md`](./RAILWAY.md), not an MCP connector. Gmail/Calendar are out of scope. Resend is the **runtime** mail provider, not a playbook connector page.

## Soft launch (A3) — keep this pattern

- `LAUNCH_UNLOCK_PRO=true`, `BILLING_ENABLED=false` → full entitlements, no charge.
- Copy: limited-time / early access. **No calendar end date. No BETA badge.**
- Ends only by **manual kill switch**.
- Price constants stay in code from the start.

## Operator

- Admin = `ADMIN_EMAILS` allowlist (Ethan). Not a grantable role.
- KPIs aggregate-only. No per-user PII on dashboards.
- Support = tickets + Resend replies, not chat.

## Out of scope for this kind’s V1 (unless CURRENT supersedes)

GPS, wearable sync, messaging, SMS, Mixpanel/Amplitude, native apps before D4, copying another product’s codebase, treating Azure/ACS as the default ship path.

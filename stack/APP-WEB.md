# app-web — full playbook path

This is the **primary** stack and process. Use **100%** of `decisions/phases/` (A1–D4). Genericized from the Bragline app-dev playbook — **do not copy that app’s source**.

## Runtime

| Layer | Choice |
| --- | --- |
| App | **Next.js** (App Router) + **TypeScript** + **Tailwind CSS** |
| Data | **Prisma** + **Postgres 16** |
| Auth (V1) | Email/password, signed JWT cookie (`jose` + `bcryptjs` or equivalent) |
| Local DB | Docker Compose Postgres 16 — [`LOCAL-DEV.md`](./LOCAL-DEV.md) |
| Prod DB | Azure Database for PostgreSQL **Flexible Server** — TLS + encryption at rest |
| Host | Azure **Container Apps** (or App Service Linux) |
| Mail | **Azure Communication Services Email** only (OTP + ticket replies). No SMS. |
| Files | **Azure Blob** in prod; local `uploads/` when Blob env is unset |
| Secrets | ACA secrets or **Key Vault** — never git |

SQLite is a documented **fallback** only (`file:` URL + explicit allow flag + Prisma provider switch). Not Azure. Not the default.

## Local ≈ Azure

Same env **names** in `.env.example` and in ACA/Key Vault:

`DATABASE_URL`, `AUTH_SECRET`, `ADMIN_EMAILS`, `LAUNCH_UNLOCK_PRO`, `BILLING_ENABLED`, later `ACS_EMAIL_*`, `AZURE_STORAGE_*`, `AZURE_KEY_VAULT_URL`.

Scripts: `scripts/dev-up.sh` / `dev-up.ps1`, `db-migrate.sh`, `db-seed.sh`. See [`deploy/README.md`](../deploy/README.md).

## Process (do not skip)

```
A1 identity + score/logging + Postgres Prisma
A2 local Docker ≈ Azure
A3 soft-launch flags
A4 theme/shell (Figma first)
B1 log UX
B2 boards/friends
B3 profile units/timezone/photos
B4 edit/swipe-delete
C1 admin allowlist
C2 ACS Email + OTP + tickets
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

Azure is the deploy checklist in [`AZURE.md`](./AZURE.md), not an MCP connector. Gmail/Calendar are out of scope.

## Soft launch (A3) — keep this pattern

- `LAUNCH_UNLOCK_PRO=true`, `BILLING_ENABLED=false` → full entitlements, no charge.
- Copy: limited-time / early access. **No calendar end date. No BETA badge.**
- Ends only by **manual kill switch**.
- Price constants stay in code from the start.

## Operator

- Admin = `ADMIN_EMAILS` allowlist (Ethan). Not a grantable role.
- KPIs aggregate-only. No per-user PII on dashboards.
- Support = tickets + ACS replies, not chat.

## Out of scope for this kind’s V1 (unless CURRENT supersedes)

GPS, wearable sync, messaging, SMS, Resend/SES, Mixpanel/Amplitude, native apps before D4, copying another product’s codebase.

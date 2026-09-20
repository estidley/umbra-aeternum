# Railway — deploy checklist (app-web)

Process we actually use when **deploying**. This is **not** an MCP connector and not a place to fake `railway` automation.

Do not provision Railway from a Cloud Agent unless Ethan asked. Local Docker (A2) comes first.

## Shape (bare bones)

| Concern | Service |
| --- | --- |
| App | One **Railpack**-built Next.js service (`railway.json`) |
| Database | **Railway PostgreSQL** (Postgres 16) on the project private network |
| In transit | Private `DATABASE_URL=${{Postgres.DATABASE_URL}}` — no public DB proxy unless Ethan explicitly accepts a time-bounded spike |
| Email | **Resend HTTPS API** (`RESEND_API_KEY`, `EMAIL_FROM`) on the product domain (OTP + CS replies). Optional Gmail SMTP only when Resend is unset. |
| Files | Persistent **volume** at `/data`; app var `PHOTO_UPLOAD_DIR=/data/uploads` |
| Secrets | Railway **service variables** — never git |
| Domain | Railway-generated domain for smoke, then custom hostname + TLS |
| Analytics | In-app / Railway only — no Mixpanel/Amplitude |

Keep **one app replica** while uploads live on a single attached filesystem. The volume is mounted at **runtime**, not during build or pre-deploy — migrations must not read or write upload files.

Azure Container Apps, Flexible Server, Blob, Key Vault, and ACS Email are **superseded**. See [`AZURE.md`](./AZURE.md) only if Ethan reopens that path. Leave `AZURE_STORAGE_*` and `ACS_EMAIL_*` unset.

## Same names as local

`DATABASE_URL`, `AUTH_SECRET`, `ADMIN_EMAILS`, `LAUNCH_UNLOCK_PRO`, `BILLING_ENABLED`, `PHOTO_UPLOAD_DIR`, `RESEND_API_KEY`, `EMAIL_FROM`. Optional local/soft-beta: `GMAIL_USER`, `GMAIL_APP_PASSWORD`. Dormant: `AZURE_STORAGE_*`. Superseded: `ACS_EMAIL_*`.

Never commit values.

## Prod checklist

1. Railway project + PostgreSQL service. App service from the GitHub repo / production branch.
2. Reference the private Postgres `DATABASE_URL` on the app service. Do not expose the database publicly for normal operation.
3. Attach a volume at `/data`. Set `PHOTO_UPLOAD_DIR=/data/uploads`.
4. Secrets in Railway service variables — including a **fresh** `AUTH_SECRET`, `ADMIN_EMAILS`, launch/billing flags, `RESEND_API_KEY`, and `EMAIL_FROM`.
5. `railway.json` runs `npx prisma migrate deploy` **before** traffic and health-checks `GET /api/health`.
6. Resend: verify the product domain; SPF / DKIM / DMARC **before** prod OTP. Sends go to `https://api.resend.com/emails`, not SMTP.
7. Soft-launch flags: `LAUNCH_UNLOCK_PRO=true`, `BILLING_ENABLED=false` until D4 kill switch.
8. Railway domain for smoke, then custom domain + TLS.
9. Confirm the app **refuses** SQLite `file:` URLs.
10. Do **not** seed production.

## Explicitly not this file

- A required Railway MCP connector (GitHub + Figma stay the only process connectors).
- Day-to-day mail/calendar connectors.
- Copying Bragline application source.

When a product repo grows a real `railway.json` / deploy script, link it from [`../deploy/README.md`](../deploy/README.md) and keep names aligned.

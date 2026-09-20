# Deploy & local data plane

Local should look like Railway: **Postgres 16**, same env var names, no secrets in git. This folder does **not** provision Railway.

This template is **process + stubs**. `dev-up` starts Postgres; migrate/seed expect a product `package.json` + Prisma (A1). `railway.json` is the prod deploy stub.

## Local Postgres (required for app-web)

Docker Compose runs Postgres 16 on `localhost:5432` (user/db `app`, password `app`, named volume).

```bash
cp .env.example .env
# set AUTH_SECRET
./scripts/dev-up.sh          # compose up + wait healthy; prints DATABASE_URL hint
./scripts/db-migrate.sh      # prisma generate + migrate deploy (needs the app)
./scripts/db-seed.sh
npm run dev
```

`DATABASE_URL` in `.env` must be the Postgres URL (see `.env.example`). In production the app **throws** on a `file:` SQLite URL. Locally it should warn unless you switch Prisma’s provider **and** set an explicit allow flag (fallback only — not Railway).

Files use `uploads/` when `PHOTO_UPLOAD_DIR` is unset. Azure Blob / Azurite is **not** required.

## Windows (DevWork / PowerShell)

Typical host: **`DESKTOP-KAAGPFG`**. Git Bash can run `scripts/*.sh`. Or Docker + Prisma directly:

```powershell
cp .env.example .env
# edit AUTH_SECRET
pwsh -File scripts/dev-up.ps1
npx prisma generate
npx prisma migrate deploy
npx prisma db seed
npm run dev
```

Optional remote QA: Aeternum over Tailscale — see [`../stack/LOCAL-DEV.md`](../stack/LOCAL-DEV.md).

## Railway (later — same names)

Checklist: [`../stack/RAILWAY.md`](../stack/RAILWAY.md). Do not create Railway resources from this repo unless Ethan asked.

| Local | Railway |
| --- | --- |
| `docker-compose.yml` Postgres 16 | **Railway PostgreSQL** (private `DATABASE_URL`) |
| app on :3000 | **Railpack** Next.js service (`railway.json`) |
| `.env` | Railway **service variables** |
| local `uploads/` | Volume at `/data` + `PHOTO_UPLOAD_DIR=/data/uploads` |
| (later) Resend placeholders | **Resend HTTPS API** on the product domain |

Use the **same** variable names: `DATABASE_URL`, `AUTH_SECRET`, `ADMIN_EMAILS`, `LAUNCH_UNLOCK_PRO`, `BILLING_ENABLED`, `PHOTO_UPLOAD_DIR`, plus later `RESEND_API_KEY`, `EMAIL_FROM`. Optional: `GMAIL_*`. Dormant: `AZURE_STORAGE_*`. Superseded: `ACS_EMAIL_*`. Never commit values.

Prod: `npx prisma migrate deploy` (pre-deploy in `railway.json`) against Railway Postgres before traffic. Do not seed production. SQLite is not prod.

Azure Container Apps / Flexible Server / ACS / Key Vault are **not** the default. Historical notes: [`../stack/AZURE.md`](../stack/AZURE.md).

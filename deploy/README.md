# Deploy & local data plane

Local should look like Azure: **Postgres 16**, same env var names, no secrets in git. This folder does **not** provision Azure.

This template is **process + stubs**. `dev-up` starts Postgres; migrate/seed expect a product `package.json` + Prisma (A1).

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

`DATABASE_URL` in `.env` must be the Postgres URL (see `.env.example`). In production the app **throws** on a `file:` SQLite URL. Locally it should warn unless you switch Prisma’s provider **and** set an explicit allow flag (fallback only — not Azure).

Azurite (Blob emulator) is **not** in compose. Files use `uploads/` when Blob env vars are unset.

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

## Azure (later — same names)

Checklist: [`../stack/AZURE.md`](../stack/AZURE.md). Do not create Azure resources from this repo unless Ethan asked.

| Local | Azure |
| --- | --- |
| `docker-compose.yml` Postgres 16 | **Azure Database for PostgreSQL Flexible Server** (TLS + encryption at rest) |
| app on :3000 | **Container Apps** (or App Service Linux) |
| `.env` | Container Apps secrets or **Key Vault** |
| local `uploads/` | **Azure Blob** |
| (later) ACS placeholders | **ACS Email** on the product domain |

Use the **same** variable names: `DATABASE_URL`, `AUTH_SECRET`, `ADMIN_EMAILS`, `LAUNCH_UNLOCK_PRO`, `BILLING_ENABLED`, plus later `ACS_EMAIL_*`, `AZURE_STORAGE_*`, `AZURE_KEY_VAULT_URL`. Never commit values.

Prod: `npx prisma migrate deploy` against Flexible Server before traffic. SQLite is not prod.

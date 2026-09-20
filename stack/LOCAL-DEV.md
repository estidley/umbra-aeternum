# Local dev (app-web)

Local is supposed to **look like Railway**: Postgres 16, same env names, no secrets in git.

## Typical machine

| Item | Value |
| --- | --- |
| Role | Windows **DevWork** |
| Host name | **`DESKTOP-KAAGPFG`** |
| Docker | Docker Desktop |
| Shell | PowerShell / `pwsh`, or Git Bash for `scripts/*.sh` |
| Remote QA (optional) | **Aeternum** over **Tailscale** — use when someone needs to hit this machine from elsewhere. Not required for day-to-day. Do not invent a VPN process if you are sitting at the box. |

If the hostname differs on a new machine, note it in `decisions/CURRENT.md`. Do not assume cloud agents can reach DevWork.

## First run

```bash
cp .env.example .env
# set AUTH_SECRET to a long random string
# npm install   # once the product repo has a package.json — this template does not
./scripts/dev-up.sh          # docker compose up, wait healthy, print DATABASE_URL hint
./scripts/db-migrate.sh      # prisma generate + migrate deploy
./scripts/db-seed.sh
npm run dev
```

Windows:

```powershell
cp .env.example .env
# edit AUTH_SECRET
pwsh -File scripts/dev-up.ps1
# then prisma generate / migrate deploy / db seed / npm run dev
```

Details: [`../deploy/README.md`](../deploy/README.md).

## Env names (do not rename)

Match Railway: `DATABASE_URL`, `AUTH_SECRET`, `ADMIN_EMAILS`, `LAUNCH_UNLOCK_PRO`, `BILLING_ENABLED`, `PHOTO_UPLOAD_DIR`. Leave `RESEND_API_KEY` / `EMAIL_FROM` commented until C2. Optional Gmail SMTP (`GMAIL_USER`, `GMAIL_APP_PASSWORD`) only when Resend is unset. Leave dormant Azure Blob and superseded ACS vars commented.

Default local URL (compose):

```
DATABASE_URL="postgresql://app:app@localhost:5432/app"
```

## SQLite

Fallback only. Requires Prisma `provider = "sqlite"` **and** an explicit allow flag. The app should **throw** on `file:` URLs in production. Never use SQLite on Railway.

## Photos / uploads

Leave `PHOTO_UPLOAD_DIR` unset → local `uploads/`. Prod uses a Railway volume at `/data/uploads`. Azure Blob (`AZURE_STORAGE_*`) is dormant / optional — do not add Azurite to compose on A2 unless you are blocked.

## What this file is not

Not a second Railway guide. Not a Tailscale install manual. Not a place to list every tool on the DevWork box.

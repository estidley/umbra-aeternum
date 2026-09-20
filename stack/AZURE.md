# Azure — superseded deploy checklist (app-web)

**Superseded.** New `app-web` products ship on **Railway + Resend**. See [`RAILWAY.md`](./RAILWAY.md) and [`APP-WEB.md`](./APP-WEB.md).

This file stays as a historical / reopen-only checklist. Do **not** provision Azure, ACS Email, Blob, or Key Vault unless Ethan locks that path again in `decisions/`.

Do not provision Azure from a Cloud Agent unless Ethan asked.

## Historical shape (do not assume for new products)

| Concern | Service (old default) |
| --- | --- |
| App | Container Apps (or App Service Linux) |
| Images | Azure Container Registry |
| Database | Azure Database for PostgreSQL Flexible Server (Postgres 16) |
| Email | Azure Communication Services Email — **superseded; unused** |
| Files | Azure Blob — **dormant / optional only**. Prod default is a Railway volume. |
| Secrets | ACA secrets or Key Vault — **superseded** by Railway service variables |

## If Ethan reopens Azure

Use the **same Railway/Resend-matching names** (`DATABASE_URL`, `AUTH_SECRET`, `ADMIN_EMAILS`, `LAUNCH_UNLOCK_PRO`, `BILLING_ENABLED`, `PHOTO_UPLOAD_DIR`, `RESEND_API_KEY`, `EMAIL_FROM`). Do not invent a second set of Azure-only names. Leave `ACS_EMAIL_*` unset unless a new decision revives ACS.

Never commit values.

## Explicitly not this file

- The default prod path.
- A made-up Azure MCP server.
- Bicep/Terraform unless the product repo later grows a real module (fill then).

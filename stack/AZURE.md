# Azure — deploy checklist (app-web)

Process we actually use when **deploying**. This is **not** an MCP connector and not a place to fake `az` automation.

Do not provision Azure from a Cloud Agent unless Ethan asked. Local Docker (A2) comes first.

## Shape (bare bones)

| Concern | Service |
| --- | --- |
| App | **Container Apps** (preferred) or App Service Linux |
| Images | Azure Container Registry |
| Database | **Azure Database for PostgreSQL Flexible Server** (Postgres 16) |
| Encrypt SQL | Encryption at rest (platform TDE minimum; CMK via Key Vault when practical) |
| In transit | **TLS** on `DATABASE_URL` — no cleartext SQL on the public network |
| Email | **Azure Communication Services Email** on the product domain (OTP + CS replies) |
| Files | **Azure Blob** (photos, ticket attachments) |
| Secrets | ACA secrets or **Key Vault** |
| Domain | Custom hostname + TLS |
| Analytics | In-app / Azure only — no Mixpanel/Amplitude |

## Same names as local

`DATABASE_URL`, `AUTH_SECRET`, `ADMIN_EMAILS`, `LAUNCH_UNLOCK_PRO`, `BILLING_ENABLED`, `ACS_EMAIL_CONNECTION_STRING`, `ACS_EMAIL_SENDER`, `AZURE_STORAGE_CONNECTION_STRING`, `AZURE_STORAGE_CONTAINER`, `AZURE_KEY_VAULT_URL`.

Never commit values.

## Prod checklist

1. Flexible Server up; TLS required; firewall allows the Container App (not `0.0.0.0/0` unless Ethan explicitly accepts that for a spike).
2. Secrets in ACA or Key Vault — including `AUTH_SECRET` and `ADMIN_EMAILS`.
3. `npx prisma migrate deploy` against Flexible Server **before** traffic.
4. ACS Email: SPF / DKIM / DMARC on the sending domain **before** prod OTP.
5. Blob container created; app identity or connection string in secrets.
6. Soft-launch flags: `LAUNCH_UNLOCK_PRO=true`, `BILLING_ENABLED=false` until D4 kill switch.
7. Custom domain + TLS on the Container App.
8. Confirm the app **refuses** SQLite `file:` URLs.

## Explicitly not this file

- A made-up Azure MCP server.
- Bicep/Terraform unless the product repo later grows a real module (fill then).
- Day-to-day mail/calendar connectors.

When a product repo grows real deploy scripts, link them from [`../deploy/README.md`](../deploy/README.md) and keep names aligned.

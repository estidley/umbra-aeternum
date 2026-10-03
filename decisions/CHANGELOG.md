# Changelog

Newest first. Do not rewrite history. If a decision changes, add a new entry and note what it supersedes.

Entry shape:

```md
### YYYY-MM-DD — short title

- **Context:** why this came up.
- **Decision(s):** what is locked.
- **Why:** one or two sentences.
- **Implications:** what implementers must do / not do. Phase ID if relevant.
```

---

### 2026-10-03 — Umbra is a native Android client

- **Context:** Ethan corrected the product away from a Hermes Phone chat app. Umbra is the 5e sheet plus an AI GM, on device, talking to a Hermes API Webb owns.
- **Decision(s):**
  1. Process deployment kind stays `app-web` and phase stays **A1** in PLAN / CURRENT so the playbook labels still apply. The implementation is native Kotlin, Material 3, minSdk 28. Not Expo. Not a web app. Not a Railway service.
  2. No backend in this repo. Hermes base URL defaults to the placeholder `https://hermes.example.invalid` and can be overridden on device. API keys never enter git.
  3. Character document shape is copied from Aeternum-VTT `characterDocument` plus the `vtt.character.v1` file wrapper. Book seed is SRD 5.1 only.
- **Why:** The phone is a client. The VTT repo stays the source of the sheet shape and must not be modified from this work.
- **Implications:** Do not add Expo, Figma, Railway, or a login API. AI writes to the sheet or book only after a yes/no confirm. Image generation and Google API stay disconnected.

### 2026-09-20 — app-web default is Railway + Resend (Bragline-derived)

- **Context:** New products were still templated on Azure Container Apps / Flexible Server / ACS Email / Blob / Key Vault. Bragline already ships on Railway + Resend; the playbook lagged that live path.
- **Decision(s):**
  1. **`app-web` prod host** is **Railway**: one Railpack-built Next.js service, Railway PostgreSQL on the private network, secrets in Railway service variables.
  2. **Email** is **Resend HTTPS API** (`RESEND_API_KEY`, `EMAIL_FROM`) as primary. Optional Gmail SMTP (`GMAIL_USER`, `GMAIL_APP_PASSWORD`) only when Resend is unset. **ACS Email is superseded / unused.**
  3. **Uploads** persist on a Railway volume (`PHOTO_UPLOAD_DIR=/data/uploads`). Azure Blob stays dormant / optional only — not required.
  4. Local Docker Postgres 16 remains the dress rehearsal. Env **names** match Railway/Resend, not Azure / Key Vault / ACS.
  5. What still fits is unchanged: Next.js App Router + TypeScript + Tailwind + Prisma + Postgres 16, JWT cookie auth, soft-launch flags (`LAUNCH_UNLOCK_PRO` / `BILLING_ENABLED`), A1–D4, no secret commits, Cloud Agent for product code.
- **Why:** Process should match how Ethan actually ships, not the retired Azure default. Names and checklists copied from Bragline’s `.env.example` / `deploy/README.md` patterns — **not** application source.
- **Implications:** Read PLAN → CURRENT → `stack/APP-WEB.md` → `stack/LOCAL-DEV.md` and pick Railway + Resend + volume uploads. `stack/AZURE.md` is historical. C2 is Resend + OTP + tickets. A2 is local Docker ≈ Railway. Do not provision Azure unless a later decision reopens it.
- **Supersedes:** Azure-as-default cloud/data/email/upload rows in the 2026-09-17 template seed (historical entry kept below).

### 2026-09-17 — Playbook template seeded

- **Context:** Ethan needed a reusable GitHub template: process + stubs, not an app. Stack is deployment-dependent. Connectors only if they are real process steps.
- **Decisions:**
  1. AI entry is root `PLAN.md` → `decisions/CURRENT.md` → newest `HANDOFF.md`.
  2. **`app-web`** runs the full A1–D4 playbook (genericized Bragline process). Other deployment kinds get stubs in `stack/OTHER-DEPLOYMENTS.md`.
  3. Connectors in this template: **GitHub** (source, PRs, Cloud Agents) and **Figma** (mocks / accents before locking UI). Azure is a deploy checklist, not a fake MCP. Gmail/Calendar are out of scope.
  4. Local default for app-web: Docker Postgres 16, Azure-matching env names. Typical DevWork host: `DESKTOP-KAAGPFG`.
- **Why:** Token-efficient replication. Only processes we have actually used.
- **Implications:** After “Use this template,” fill Name + Deployment kind, then start **A1** (or skip A–D if not app-web). Do not copy Bragline application source.

# HANDOFF

Cross-AI board (Cursor ↔ Claude ↔ Grok ↔ Cloud Agent ↔ human). **Newest entry at the top.**

Use this when a different model or machine continues the work. Do not treat chat history as the source of truth.

## How to write an entry

Copy the block. Fill every field. Prepend — do not edit older rows except to mark `status` done/superseded.

```md
## YYYY-MM-DD — short title

| Field | Value |
| --- | --- |
| Date | YYYY-MM-DD |
| From | cursor / claude / grok / cloud-agent / ethan |
| To | cursor / claude / grok / cloud-agent / ethan / any |
| Status | open / blocked / done / note |
| Phase | A1 (or —) |
| Message | What landed, what is next, what must not be re-litigated. Link decisions/CURRENT or a changelog heading. |
```

Keep messages short. Decisions live in `decisions/`, not here.

---

## 2026-10-03 — Umbra Android client

| Field | Value |
| --- | --- |
| Date | 2026-10-03 |
| From | Ched |
| To | Webb + Fred |
| Status | open |
| Phase | A1 |
| Message | Native Umbra client on `local/umbra-android` (not merged). Debug APK only. No Railway, no Hermes server, no Expo. Local display-name login (nothing sent). Hermes: GET /health and POST /v1/chat/completions with Bearer key, body model `hermes` plus session messages (plus one local system line that describes the proposal fence). Assistant text is choices[0].message.content. Proposals are a fenced json block with proposals[].kind sheet or book, summary, target inventory/species/subclass/monster/encounter/sheet, and data. Yes is required before a write. Sheet shape from Aeternum-VTT character.ts at f0eafec; book seed is content/srd-5.1 only. VTT repo was not modified. Image generation / Google API is a not-connected stub. |

## 2026-09-20 — app-web default shifted to Railway + Resend

| Field | Value |
| --- | --- |
| Date | 2026-09-20 |
| From | cloud-agent |
| To | any |
| Status | done |
| Phase | A1 (template; no product cloned) |
| Message | Playbook defaults now match how Bragline ships: Railway (app + Postgres + `/data` volume) and Resend HTTPS API (`RESEND_API_KEY`, `EMAIL_FROM`). Azure ACA / Flexible Server / ACS / Key Vault are superseded. Azure Blob is dormant/optional. Env names match Railway, not Azure. Do not copy Bragline app source. See `decisions/CHANGELOG.md` 2026-09-20 and `stack/APP-WEB.md`. |

## 2026-09-17 — Template filled (process + stubs)

| Field | Value |
| --- | --- |
| Date | 2026-09-17 |
| From | cursor |
| To | any |
| Status | done |
| Phase | A1 (placeholder until a product is cloned) |
| Message | `estidley/project-playbook` now has PLAN, decisions/A1–D4, stack-by-kind, GitHub+Figma connectors, Docker/env/scripts stubs. Not an app. GitHub **template flag could not be set** (403); Ethan: Settings → General → Template repository. Next: merge this PR, enable the flag, Use this template for a real product, start A1. |

## 2026-09-17 — How to use this board

| Field | Value |
| --- | --- |
| Date | 2026-09-17 |
| From | playbook |
| To | any |
| Status | note |
| Phase | — |
| Message | This file is the cross-AI board, not the product spec. After cloning the template: set Name + Deployment kind in PLAN.md and decisions/CURRENT.md. Incoming AI reads PLAN → CURRENT → **this newest entry**, then works the current phase. Prepend a real handoff row when you stop (what landed, what’s next, what is locked). Do not paste chat transcripts. |

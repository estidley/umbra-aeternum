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

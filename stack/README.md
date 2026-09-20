# Stack — pick by deployment kind

Do **not** treat this folder as one flat “Ethan stack” list. Stack is **deployment-dependent**.

| Kind | When | Process |
| --- | --- | --- |
| **`app-web`** | Web product that may later grow native | **Full playbook** — 100% of A1–D4. Read [`APP-WEB.md`](./APP-WEB.md). |
| **`app-web` local** | Day-to-day on the DevWork machine | [`LOCAL-DEV.md`](./LOCAL-DEV.md) — Docker Postgres, Railway/Resend-matching names. |
| **`app-web` prod** | Railway | [`RAILWAY.md`](./RAILWAY.md) — deploy **checklist**, not a fake MCP. |
| **`app-web` Azure (superseded)** | Only if Ethan reopens Azure | [`AZURE.md`](./AZURE.md) — historical checklist. Not the default. |
| **Anything else** | Godot, Android-only, local dashboards, scripts, VTT, etc. | [`OTHER-DEPLOYMENTS.md`](./OTHER-DEPLOYMENTS.md) — **stubs only**. Fill when needed. Do not invent A1–D4. |

Set **Deployment kind** in [`PLAN.md`](../PLAN.md) and [`decisions/CURRENT.md`](../decisions/CURRENT.md) on day one.

## Rule

If the project is **app development** (web → maybe native), use the Bragline-style process in this template (Railway + Resend as the ship path). Other kinds get a short “fill when needed” section — not a fake process.

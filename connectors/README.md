# Connectors

Only integrations that are **actually useful as process steps** we have used for real. Not a catalog.

| Connector | Process role | File |
| --- | --- | --- |
| **GitHub** | Source of truth, PRs, Cloud Agents | [`github.md`](./github.md) |
| **Figma** | UI mocks / accents **before** locking UI | [`figma.md`](./figma.md) |

## Out of scope (this template)

- **Gmail / Calendar / other day-to-day assistants** — Ethan can add later; they are not app-dev playbook steps.
- **Azure as an MCP** — deploy is a checklist: [`stack/AZURE.md`](../stack/AZURE.md). Do not invent a connector page.
- Random SaaS (Resend, Mixpanel, Slack, Notion, Linear, …) unless a future decision locks them as a **process step**.

If you are tempted to add a connector: it must have a repeatable step (when to open it, what to lock, what not to do). Otherwise leave it out.

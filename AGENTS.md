# AGENTS

Shared rules for every assistant that touches a repo cloned from this template (Cursor, Claude, Grok, Cloud Agents, others).

## Read order

1. [`PLAN.md`](./PLAN.md) — current phase, deployment kind, token rules.
2. [`decisions/CURRENT.md`](./decisions/CURRENT.md) — locked snapshot.
3. Newest entry in [`HANDOFF.md`](./HANDOFF.md).
4. Only then the phase file for the current ID (`A1`…`D4`).

Do not dump the whole tree into context. Do not invent connectors, stacks, or phases.

## Deployment kind

- **`app-web`** (default): run **100%** of the playbook in `decisions/phases/` and [`stack/APP-WEB.md`](./stack/APP-WEB.md).
- **Anything else:** [`stack/OTHER-DEPLOYMENTS.md`](./stack/OTHER-DEPLOYMENTS.md). Short stubs. Fill when the project actually needs a process. Do not fake an A1–D4 sequence.

## Connectors in scope

Process-useful only — see [`connectors/README.md`](./connectors/README.md):

- **GitHub** — source, PRs, Cloud Agents.
- **Figma** — UI mocks / accents **before** locking UI.

Railway is a **deploy checklist** ([`stack/RAILWAY.md`](./stack/RAILWAY.md)), not an MCP connector. Azure is superseded ([`stack/AZURE.md`](./stack/AZURE.md)). Gmail, Calendar, and other day-to-day assistants are **out of scope** unless Ethan adds them. Resend is the runtime mail provider, not a connector page.

## Working rules

- Lock the decision in `decisions/` the same day (append changelog; update CURRENT if the snapshot row changed).
- Implement in the product repo via GitHub (branch + PR). Cloud Agent for repo code.
- Same env **names** locally and on Railway (Resend-matching mail vars). Never commit secrets.
- Original implementation. Do not clone third-party app source.
- When handing off: prepend a row to `HANDOFF.md` (date, from, to, status, message).

## What not to do

- Do not start payments, passkeys, or native apps before the phase index says so.
- Do not add “helpful” connectors or a flat stack list.
- Do not copy Bragline (or any other product) application source into a new repo.
- Do not rewrite `decisions/CHANGELOG.md` history.

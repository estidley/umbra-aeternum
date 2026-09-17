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

### 2026-09-17 — Playbook template seeded

- **Context:** Ethan needed a reusable GitHub template: process + stubs, not an app. Stack is deployment-dependent. Connectors only if they are real process steps.
- **Decisions:**
  1. AI entry is root `PLAN.md` → `decisions/CURRENT.md` → newest `HANDOFF.md`.
  2. **`app-web`** runs the full A1–D4 playbook (genericized Bragline process). Other deployment kinds get stubs in `stack/OTHER-DEPLOYMENTS.md`.
  3. Connectors in this template: **GitHub** (source, PRs, Cloud Agents) and **Figma** (mocks / accents before locking UI). Azure is a deploy checklist, not a fake MCP. Gmail/Calendar are out of scope.
  4. Local default for app-web: Docker Postgres 16, Azure-matching env names. Typical DevWork host: `DESKTOP-KAAGPFG`.
- **Why:** Token-efficient replication. Only processes we have actually used.
- **Implications:** After “Use this template,” fill Name + Deployment kind, then start **A1** (or skip A–D if not app-web). Do not copy Bragline application source.

# GitHub — source, PRs, Cloud Agents

The only code home. Chat is not the repo.

## Process steps (app-web)

1. **Create the product repo** from this template (GitHub “Use this template”). Private unless Ethan says public.
2. Fill `PLAN.md` + `decisions/CURRENT.md` (name, deployment kind, current phase).
3. **Implementation happens on GitHub** — branch, commit, PR into the product default branch.
4. **Cloud Agents** work in the GitHub repo. Point them at `PLAN.md` first (`AGENTS.md` restates this).
5. After a locked decision: append `decisions/CHANGELOG.md` in the **same** PR (or immediately after). Same day.
6. Cross-model handoff: prepend `HANDOFF.md`. The next Cloud Agent / Cursor / Claude / Grok reads that — not your transcript.

## What GitHub is for

- Source, history, PRs, reviews.
- Issues if Ethan wants them; they do not replace `decisions/`.
- Cloud Agent runs that edit this repo.

## What GitHub is not

- A second decisions log (don’t paste changelog into issue comments as the source of truth).
- A secrets store.
- Permission to clone other products’ application source “for reference.”

## Token habit

Give agents the phase ID (`A1`…) and the three-file read order. Do not attach the entire tree.

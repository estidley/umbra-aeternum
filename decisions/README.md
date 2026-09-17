# decisions/

Living log for humans and AIs. **This directory is the planning source of truth** after `PLAN.md`.

| File | Role |
| --- | --- |
| [`CURRENT.md`](./CURRENT.md) | Snapshot of locked choices. Read after PLAN. |
| [`CHANGELOG.md`](./CHANGELOG.md) | Dated entries, **newest first**. Append; do not rewrite. |
| [`phases/`](./phases/) | Replicable **app-web** process (A1…D4). Not a product history. |

## Rule

Any meaningful product or tech choice made in chat or implementation lands here **the same day**, with *context → decision → why → implications*.

1. Read **CURRENT** + the latest changelog entries.
2. After a locked decision: add `### YYYY-MM-DD — title` at the **top** of CHANGELOG.
3. If it changes a snapshot row, edit CURRENT and note supersession in the new entry.
4. Keep entries short. Decisions live here, not in chat.

## Phases

`A1`, `A2`… `B1`, `B2`… are the labels. They apply only when **deployment kind = `app-web`**. Other kinds do not get a fake phase tree.

## What this is not

- Not Bragline app source.
- Not a second README.
- Not a place to dump connector catalogs or unused stacks.

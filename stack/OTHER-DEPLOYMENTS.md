# Other deployment kinds — stubs only

**Not the primary playbook.** If `PLAN.md` says **deployment kind = `app-web`**, ignore this file.

These are **fill-when-needed** stubs. Do **not** invent an A1–D4 sequence, a fake Railway/Azure MCP, or a connector catalog. Token-efficient: only write a process here after Ethan has actually used it twice (or explicitly locks it).

---

## How to use a stub

1. Set **Deployment kind** in `PLAN.md` + `decisions/CURRENT.md` to one of the ids below (or add a row).
2. Fill the stub in the **cloned product repo**, not by bloating this template with guesswork.
3. Keep GitHub as source + PRs if there is code. Figma only if there is UI to lock.
4. `HANDOFF.md` still applies. Phase labels A1–D4 do **not**, unless you later promote the kind to a real playbook.

---

## `game-godot`

- **When:** Godot / GDScript shipped as a game (Steam, itch, export templates).
- **Fill later:** engine version, export targets, save-data location, store checklist.
- **Do not:** run Next.js/Prisma/Railway Postgres “for consistency.”
- **Process so far:** none locked in this template.

## `native-android`

- **When:** Android-first (Kotlin, Play). Not “web wrapped later.”
- **Fill later:** min SDK, signing, Play track, privacy.
- **Do not:** copy the app-web email/OTP/Resend path unless you actually use it.
- **Process so far:** none locked in this template.

## `native-ios`

- **When:** iOS-first. For app-web, native is **D4 last** — that is not this kind.
- **Fill later:** Xcode / TestFlight / signing.
- **Process so far:** none locked in this template.

## `local-tool`

- **When:** PowerShell/Node scripts, local dashboards, one-machine utilities.
- **Fill later:** how to run, what must never hit a public host.
- **Do not:** Docker-Postgres-Railway theatre.
- **Process so far:** none locked in this template.

## `other`

- **When:** VTT, firmware, static site, anything that is not app-web.
- **Fill later:** one short CURRENT snapshot: language, how it runs, where it ships.
- **Process so far:** none locked in this template.

---

## Promotion rule

If a non-app-web kind grows a **replicable** process (used for real, more than once), add a real markdown file beside this one and point `stack/README.md` at it. Until then, leave the stub empty of fake steps.

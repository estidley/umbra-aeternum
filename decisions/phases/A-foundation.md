# A — Foundation

Spine first. Do not start B until A1–A4 **done-when** is true. Labels: **A1 A2 A3 A4**.

---

## A1 — Identity + core score/logging + Postgres-shaped Prisma

The load-bearing data plane. Lock the **score formula** (or equivalent core computed value) in `decisions/` **before** writing schema or UI.

### Checklist

- [ ] Product name + core value name locked in `CURRENT.md` (never mislabel in UI — e.g. do not call a score “calories” if it is not).
- [ ] Auth: email/password sessions (signed JWT cookie). Register / sign in persist a user.
- [ ] Prisma schema targets **Postgres** from day one (`provider = "postgresql"`). Models: User, primary log/event, score inputs.
- [ ] One successful **create log** path writes a row and computes the score server-side.
- [ ] Only fields that **feed the score** (or required identity). No “nice” columns yet.
- [ ] Legal consent checkboxes at signup if the domain is health-adjacent or you collect PII (Terms + Privacy).
- [ ] Username uniqueness + server-side reserved/blocklist (generic “isn’t available”).

### Done when

A human can register, sign in, persist **one** primary log, and see the computed score. Schema is Postgres-shaped. Formula is in CHANGELOG, not only in the model’s head.

### Pitfalls

- Designing the whole social graph before a single log saves.
- SQLite-first schema that later breaks on Flexible Server (`DateTime`, enums, constraints).
- Putting the formula only in the client.
- Accepting every chat “what if” field that does not change the score.

### What NOT to do early

Payments, Stripe, passkeys, native apps, GPS, boards polish, admin KPIs, ACS wiring, chart redesigns, extra MET/catalog rows you do not need to score V1.

---

## A2 — Local Docker ≈ Azure

Local should be a dress rehearsal for Azure. Same engine major, same env **names**. Do **not** provision Azure this slice.

### Checklist

- [ ] `docker-compose.yml` runs **Postgres 16** (volume, `5432`, healthcheck).
- [ ] `.env.example` lists Azure-matching names: `DATABASE_URL`, `AUTH_SECRET`, `ADMIN_EMAILS`, `LAUNCH_UNLOCK_PRO`, `BILLING_ENABLED`, plus commented ACS / Blob / Key Vault.
- [ ] `scripts/dev-up.sh` + `scripts/dev-up.ps1` bring compose up and wait healthy.
- [ ] `scripts/db-migrate.sh` → `prisma generate` + `migrate deploy`.
- [ ] `scripts/db-seed.sh` → `prisma db seed` (demo user documented in README).
- [ ] App refuses SQLite `file:` URLs unless an explicit allow flag **and** Prisma provider is switched. Documented fallback only.
- [ ] Windows path works on typical DevWork (`DESKTOP-KAAGPFG`): Docker Desktop + `pwsh -File scripts/dev-up.ps1`. See [`stack/LOCAL-DEV.md`](../../stack/LOCAL-DEV.md).

### Done when

Fresh clone: copy `.env.example` → compose → migrate → seed → `npm run dev` against Docker Postgres. No Azure resources created.

### Pitfalls

- Leftover `DATABASE_URL=file:./dev.db` in the shell winning over `.env`.
- Different variable names locally vs ACA/Key Vault.
- Skipping healthcheck; migrate races the engine.

### What NOT to do early

`az` provisioning, Azurite-unless-needed, Key Vault in local compose, committing `.env`.

---

## A3 — Soft-launch flags

Ship to real users without charging. Paywall **code** exists; paywalls stay **off**.

### Checklist

- [ ] `LAUNCH_UNLOCK_PRO=true` and `BILLING_ENABLED=false` by default.
- [ ] Every signed-in user gets full paid entitlements while those flags say unlock / billing-off.
- [ ] Price constants live in one module (monthly / yearly / lifetime). Do not delete gates — wrap them.
- [ ] Copy: **limited-time free / early access**. **No calendar end date.** **No BETA badge.**
- [ ] Offer ends only via **manual kill switch**: `LAUNCH_UNLOCK_PRO=false` **and** `BILLING_ENABLED=true`. No cron.

### Done when

Flags are the only way to turn paywalls on. Banner/profile copy is dateless. Constants remain for D4.

### Pitfalls

- Publishing “ends October 1” and then slipping.
- Deleting gate functions because “everyone is Pro anyway.”
- Wiring Stripe “just to have it.”

### What NOT to do early

Checkout, webhooks, native-app store IAP, dated countdowns.

---

## A4 — Theme / shell

Lock look from **Figma** before pouring hours into UI. Mobile-first stays; wide screens get a real shell.

### Checklist

- [ ] Accent + background tokens locked from a Figma pick (see [`connectors/figma.md`](../../connectors/figma.md)). One theme.
- [ ] Mobile: bottom nav + centered primary action (FAB / equivalent).
- [ ] From **`lg`**: sidebar + wider content. Do not remove the phone FAB.
- [ ] Empty states point at the same create route as the FAB.

### Done when

Phones keep the thumb bar; desktop is not a skinny phone column. Tokens match the Figma accent. No second theme “to try.”

### Pitfalls

- Redesigning charts (D1) during shell work.
- FAB inside a `transform`ed nav (eats taps).
- Treating desktop as “scale the mobile column.”

### What NOT to do early

Native navigation patterns, chart visual rewrite, photo pipeline (B3), chooser sheets that block the log page (B1).

# D — Polish / ship

Depth, operator metrics, stronger auth, then money — then native. Labels: **D1 D2 D3 D4**.

---

## D1 — Charts / achievements

Paid **insight** and a badge grind. Boards stay social and (typically) Free.

### Checklist

- [ ] Pro charts: **pie**, **bar/column**, and **multi-series line** (you + selectable friends). All three — not one-of.
- [ ] Visual bar: theme accent, gradients, soft shadows. **No** gimmicky full-3D WebGL as the default.
- [ ] Free: **locked teasers** (shape of Pro), not a missing page.
- [ ] Friend overlay / deep peek key off the **viewer’s** entitlements, not the subject’s plan. Visibility + friendship still apply.
- [ ] Achievements: catalog with earn rules. Soft launch: everyone can earn. After billing: Free earns the starter only; Pro earns the rest; **earned persist** on downgrade.
- [ ] Do not deep-rewrite charts until this slice (do not steal A4/B1 time for a redesign).

### Done when

Entitled users get pie + bar + friend overlay. Free sees teasers. At least the starter achievement can award on log.

### Pitfalls

- Paywalling boards instead of graphs / peeks / cheers / earning.
- Mixpanel “just for funnels.”
- Calorie (or other false) labels on charts.

### What NOT to do early

Native app-open badges awarded from web visits, Stripe, chart engines that need a GPU.

---

## D2 — Admin KPIs

Product health next to the actions that move it. **Aggregate only.** In-app / Railway only.

### Checklist

- [ ] `/admin/kpi`: DAU/WAU/MAU, session proxies, signups / bans / grants, mix of primary types, top free-text “Other” labels (aggregated, nobody’s name).
- [ ] **Never** per-user weight or other PII on the dashboard. User lookup stays on ops screens (ban/grant).
- [ ] Plan sorter: filter/sort by plan (Free / Pro / lifetime / internal influencer label).
- [ ] Instant plan change + **audit log** (actor, target, from → to, reason, time). No silent edits.
- [ ] Pretty charts — same visual bar as D1. No third-party analytics SaaS.

### Done when

Ethan can see aggregates and change a plan with an audit row. No per-user body table.

### Pitfalls

- Drill-down that becomes a PII browser.
- Shipping KPIs before abuse/ban (C1/C4) exists.

### What NOT to do early

Amplitude/Mixpanel, per-user health widgets, public status page.

---

## D3 — Passkeys

WebAuthn **after** email OTP works. Ethan first. **No SMS.**

### Checklist

- [ ] Email OTP / magic-link already sending from Resend (C2).
- [ ] Passkeys as the primary 2FA path. Ethan’s account first.
- [ ] No phone numbers, no SMS fallback.
- [ ] Recovery still email-based.

### Done when

Allowlisted operator can sign in with a passkey; users can add one without losing email OTP.

### Pitfalls

- Shipping passkeys before SPF/DKIM/OTP (no recovery).
- “Optional SMS just in case.”

### What NOT to do early

SMS, authenticator-app-only as a substitute for the locked WebAuthn path (unless CURRENT is updated).

---

## D4 — Payments, then native last

Money after the web product has been lived in. Native **after** the web look is locked.

### Checklist

- [ ] QA the web loop (log → score → boards → admin) with launch unlock still on.
- [ ] Flip is mechanical: `LAUNCH_UNLOCK_PRO=false` and `BILLING_ENABLED=true`. No dated banner required.
- [ ] Wire checkout (Stripe). Keep the constants you stored in A3.
- [ ] Cancel on web: **never wipe history**. Free loses convenient Pro **views**, not the underlying logs. Resubscribe restores views.
- [ ] Comp / influencer grants stay **admin-only** (not a public coupon SKU).
- [ ] **Native iOS/Android last**, same UX as the locked web. GPS / health integrations after that if CURRENT says so.
- [ ] Native may require Pro once billing is on (`NATIVE_APPS_REQUIRE_PRO`). Website stays Free-accessible unless CURRENT changes.

### Done when

A real card can subscribe; kill switch is documented; native has not started until Ethan opens that slice.

### Pitfalls

- Starting Expo/Capacitor while A1 schema is still moving.
- Dated “sale ends” copy.
- Family SKU before CURRENT locks seats.

### What NOT to do early

Native before D4. IAP before web billing. Building a Plus tier that CURRENT already deleted.

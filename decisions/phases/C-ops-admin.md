# C — Ops / admin

Operator tools and abuse brakes. Ethan-only. Labels: **C1 C2 C3 C4**.

---

## C1 — Admin (allowlist)

Private operator UI. **Not** a user-facing role anyone can grant.

### Checklist

- [ ] `ADMIN_EMAILS` hard allowlist (default Ethan). Server-gated `/admin`.
- [ ] Everyone else: **Forbidden** (not a hidden 404 that you might “fix” into a role).
- [ ] Stub the planned ops list: Customer Support, Abuse, bans, lockout viewer, influencer/comp grants, plan sorter, instant plan change + audit, KPI dashboard.
- [ ] Do **not** ship every stub as live UI this slice — shell + allowlist is enough to start.

### Done when

Only allowlisted emails can open `/admin`. The panel is documented as operator-side.

### Pitfalls

- `role = admin` on the User model as a grantable flag.
- Building the KPI dashboard here (that is D2).

### What NOT to do early

Live CS queue, ACS send, Stripe customer portal, Mixpanel.

---

## C2 — ACS Email + OTP + tickets

Transactional mail stays on Azure. Support is **async tickets**, not chat.

### Checklist

- [ ] **Azure Communication Services Email** is the only provider. Custom domain. SPF/DKIM/DMARC before prod OTP.
- [ ] **No** Resend, Amazon SES, Gmail SMTP, or SMS.
- [ ] Email OTP / magic-link for verification and step-up. Passkeys wait for D3.
- [ ] Signed-in ticket form: subject, category (Bug / Account / Billing/Plans / Abuse report / Feature idea / Other), body, Blob attachments.
- [ ] Tied to user id + email. Notice: reply-by-email, watch inbox and spam. Public `support@` on the domain.
- [ ] Admin Customer Support: open/closed queue, ACS replies, stored thread. Not live chat. Not a public ticket board.
- [ ] Attachments: Azure Blob (separate prefix from photos); local disk when Blob unset.

### Done when

OTP can send from ACS on the domain, and a signed-in user can file a ticket Ethan can answer by email from `/admin`.

### Pitfalls

- Adding “just a Resend fallback.”
- In-app chat “until email works.”
- Sending OTP before SPF/DKIM (land in spam, lock people out).

### What NOT to do early

SMS, passkeys (D3), public coupon codes, shipping the form before ACS credentials exist in Key Vault / ACA secrets.

---

## C3 — IP soft-lock

Cheap stuffing brake on password (and later OTP) attempts. No Redis required for V1.

### Checklist

- [ ] Named constants: `LOGIN_FAIL_LIMIT = 10`, `LOGIN_SOFT_LOCK_SECONDS = 30`.
- [ ] Unknown emails and wrong passwords both count. Success clears the IP bucket.
- [ ] User-facing error does **not** mention the numbers or remaining attempts.
- [ ] In-memory per-process map is acceptable until a shared store is justified.
- [ ] Admin lockout viewer can wait; constants must not be magic numbers.

### Done when

Ten failures from one IP get a 30s soft-lock. Copy is generic. Flags for soft launch are unchanged.

### Pitfalls

- Leaking “3 attempts left” (helps stuffing).
- A long lockout that support cannot explain.
- Counting only “known emails” (user-oracle).

### What NOT to do early

WAF theatre, Redis “because later,” SMS lockout (there is no SMS).

---

## C4 — Anti-cheat + Report

V1 manual logs can be faked. Sanity + human review — **not** a lie detector. **Verified** waits for GPS/health.

### Checklist

- [ ] Server caps on create/edit: reject cartoon-impossible values and cap a day’s volume. Tune in one module.
- [ ] User-facing errors are **generic**. Never leak the formula or the thresholds.
- [ ] **Report** on boards / profiles → admin **Abuse** queue (reporter email, target id, optional note, context).
- [ ] Duplicate open reports from the same reporter are ignored.
- [ ] **Ban** from a report: block login, hide from boards. Logs stay until a later delete path.
- [ ] Soft launch: **no prizes**. Friends = trusted; Global = noisier.

### Done when

Impossible logs bounce. A report appears in `/admin/abuse` and can become a ban.

### Pitfalls

- Showing “max pace is X” in the UI.
- Auto-ban without a human queue.
- Shipping prizes that require a verified badge you do not have.

### What NOT to do early

GPS verification, wearable attestations, public shaming, prize pools.

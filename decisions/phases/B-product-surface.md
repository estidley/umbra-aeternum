# B — Product surface

User-facing loop after the spine exists. Labels: **B1 B2 B3 B4**.

---

## B1 — Log UX

The primary create action is **one tap**. Kind (run vs workout, or this product’s equivalent) is a **field on the log page**, not a menu in front of it.

### Checklist

- [ ] Center **+** / FAB and empty-state CTAs go **straight to `/log`** (or the product’s one create route).
- [ ] Kind toggle lives **on** that page. No chooser sheet.
- [ ] Intensity / type controls are native radios (or equivalent) with real label association — chips must not swallow taps.
- [ ] **Live score preview** uses the **same helper** as the server. Missing required inputs → explicit message, never a stale number.
- [ ] Save disabled until required score inputs exist (e.g. body weight).
- [ ] Recent is a link from `/log` and home — not a FAB popout.

### Done when

One tap from the chrome opens a form that can submit a scored log. Preview matches the server. Kind is not a separate route maze.

### Pitfalls

- `"use server"` files exporting strings/constants (poisons every action in the module graph). Export **only async functions**.
- Overlay chips inside `<label>` stealing clicks.
- Preview reimplementing the formula with different rounding.

### What NOT to do early

GPS, incline / non-scoring fluff, edit/delete polish (B4), friend peeks, achievements.

---

## B2 — Boards / friends

Social ranking. Friends feel like **trusted competition**; Global is **noisier**. Soft launch: **no prizes**.

### Checklist

- [ ] Surfaces: Friends + Global × the windows you locked (typical: 30d + all-time). Visibility still applies.
- [ ] Visibility: private / friends-only / public. Private stays off boards.
- [ ] Mixed-tier people rank on the **same** board (do not split Free vs Pro leaderboards).
- [ ] Friend graph: request / accept. **No friend caps** unless CURRENT says otherwise.
- [ ] Social: **emoji reactions only**. No DMs, no comment threads.
- [ ] Ranks 1–3 can get a gold/accent treatment. That’s chrome, not prizes.

### Done when

A public or friends-visible log appears on the correct boards. Private users do not. No chat product.

### Pitfalls

- Paywalling “who appears on a board” instead of insight tools.
- Building city boards or GPS-verified badges (later).
- In-app messaging “for support” (support is C2 tickets).

### What NOT to do early

Prizes, verified badges, anti-cheat (C4), report queue (C4), chart overlays (D1).

---

## B3 — Profile units / timezone / photos

People must enter units they understand; the data plane stays canonical. Photos are allowed with a ban lever.

### Checklist

- [ ] Units: enter in local unit (e.g. lb|kg); **store canonical** (e.g. kg). Default unit from locale / timezone (US → lb).
- [ ] Valid ranges that actually accept the values humans type (do not HTML-`min` the stored unit while they type the display unit).
- [ ] Timezone asked at **signup** with browser default; **Skip** still **persists** the detected zone. Editable on profile.
- [ ] Streaks / day boundaries / later KPIs use profile timezone.
- [ ] Photos: JPEG/PNG/WebP, size cap, magic-byte sniff. Prod **Railway volume** (`PHOTO_UPLOAD_DIR=/data/uploads`); local `uploads/` when unset. Azure Blob is dormant / optional only.
- [ ] Upload disabled until ToS is checked. Abuse → **ban** path. NSFW auto-scan is later/optional.
- [ ] Photo form is **not** nested inside Save profile in a way that HTML5 `required` fights the other fields.

### Done when

A US user can save a lb weight and score correctly. Timezone exists even if they skipped the picker. Avatar survives reload (cache-bust query).

### Pitfalls

- Converting on every keystroke and validating the other unit.
- Caching photo URLs without `?v=`.
- Re-exporting ToS strings from a `"use server"` file.

### What NOT to do early

NSFW vendor, Azurite-in-compose unless you need it, public CDN, per-user weight on admin KPIs. Do not require Azure Blob for V1 photos.

---

## B4 — Edit / swipe-delete

Logs are not create-only. Phone habit is swipe; desktop/keyboard still need a visible Delete.

### Checklist

- [ ] Edit route loads the owned row, recalculates score with the **same formula**, keeps the snapshot inputs the product locked (e.g. weight at log time).
- [ ] Owned list rows: **swipe left or right** → trash well → **confirm** (“Delete this session?”). One row open at a time.
- [ ] Explicit **Delete** on each row + on the edit screen (mouse, trackpad, keyboard).
- [ ] Delete cascades reactions / derived rows and revalidates home + boards.
- [ ] Friend peeks are **not** deletable.

### Done when

An owned log can be edited and deleted without losing the rest of the account. Accidental swipe does not wipe data.

### Pitfalls

- Swipe-only delete (fails a11y and desktop).
- No confirm.
- Recalculating with *current* profile weight when CURRENT says snapshot-at-log.

### What NOT to do early

Hard-delete accounts, GDPR export (unless required now), anti-cheat caps (C4) unless already blocking cartoon values.

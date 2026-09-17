# Figma — mocks / accents before locking UI

Used when the product has a **UI to lock** (app-web A4). Open Figma **before** pouring implementation into chrome.

## Process steps

1. **Accent / theme first.** Pick tokens (accent, background, rank gold/mint, etc.) in Figma. Write the hex/tokens into `decisions/CURRENT.md` and A4.
2. **Mocks for the primary loop** (log, boards, profile) if the layout is not obvious. One flow, not a 40-frame kit.
3. **Then** implement shell + tokens in the app (A4). Mobile-first; desktop shell at `lg`.
4. If Figma and code drift, **Figma wins on look** until Ethan supersedes in CHANGELOG.

## What Figma is for

- Visual lock: color, type scale, key screens.
- Accents before the CSS tokens harden.

## What Figma is not

- A substitute for `decisions/` (no product rules that exist only in a comment on a frame).
- A license to redesign Pro charts during A4 (that is D1).
- Required for `other` deployment kinds with no UI.

## Out of scope

Generating a full design system for a local script. Day-to-day FigJam workshops. Auto-sync plugins unless Ethan adds them as a process step.

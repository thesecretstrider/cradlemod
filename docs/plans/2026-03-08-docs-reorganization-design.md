# Docs Folder Reorganization

> Design doc for cleaning up and deduplicating the docs/ directory

---

## Problem

Three files overlap significantly:
- `IDEAS.md` — brainstorm dump
- `ROADMAP.md` — what's left to build
- `LORE-ACCURACY-PLAN.md` — tiered lore accuracy checklist

Many items appear in two or all three files. No single source of truth for "what to build next."

## Decision

Merge all three into a single `ROADMAP.md` with clear sections. Keep `DESIGN.md`, `LORE.md`, and `PROGRESS.md` unchanged.

## Final Structure

```
docs/
├── DESIGN.md              (unchanged — visual/mechanical design philosophy)
├── LORE.md                (unchanged — Cradle book lore reference)
├── PROGRESS.md            (unchanged — completed features tracker)
├── ROADMAP.md             (merged — single source of truth for all future work)
└── plans/                 (new — for future design docs from brainstorming)
```

## New ROADMAP.md Sections

1. **Ready to Build (Code Only)** — features that can be coded now
2. **Needs Textures First** — designed but blocked on art
3. **Lore Accuracy Rework** — tiered plan (Tier 1/2/3) from LORE-ACCURACY-PLAN.md
4. **Ideas / Maybe Later** — unvetted brainstorms not already in other sections
5. **Known Issues** — bugs and disabled features

## Merge Rules

- Duplicate items get ONE entry in the most appropriate section
- Completed items keep their "Done in Save X" notes
- Unique IDEAS.md items go to "Ideas / Maybe Later"
- CLAUDE.md docs references updated to match

## Files Deleted

- `IDEAS.md`
- `LORE-ACCURACY-PLAN.md`

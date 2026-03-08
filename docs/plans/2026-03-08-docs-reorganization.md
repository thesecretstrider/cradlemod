# Docs Reorganization Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Merge three overlapping docs files (IDEAS.md, ROADMAP.md, LORE-ACCURACY-PLAN.md) into a single ROADMAP.md, clean up CLAUDE.md references, and create the docs/plans/ directory.

**Architecture:** Pure file editing — no code changes. Merge all "what to build" content into one file with 5 sections, delete the redundant files, fix stale CLAUDE.md references.

**Tech Stack:** Markdown files, git

---

### Task 1: Write Merged ROADMAP.md

**Files:**
- Modify: `docs/ROADMAP.md`

**Step 1: Replace ROADMAP.md with merged content**

Write the following content to `docs/ROADMAP.md`:

```markdown
# Cradle Mod — Roadmap

> Single source of truth for everything left to build

---

## Ready to Build (Code Only)

These can be implemented right now with no new art needed.

- [ ] **Soulsmith Forging** — Anvil + hammer crafting system for sacred materials (Steelborn crystal as byproduct)
- [ ] **More Authority Commands** — Open, Break, etc. (extends existing Sage Authority)
- [ ] **Archlord Lore Hints** — Teaser messages about Icons and spirit-body merge
- [ ] **Herald Advancement Lore** — Hint messages at Archlord about merging spirit with body
- [ ] **Dreadgod Boss Logic** — Massive boss mob AI/behavior (prototype without final textures)

---

## Needs Textures First

Designed but blocked on custom art/models.

- [ ] **Dreadbeast Sheep** — 3 texture layers needed (wool/undercoat). Mixin disabled in Save 43
- [ ] **More Dreadbeast Animals** — Pig (3 files), Chicken (3), Spider (2), Goat (1), Fox (4)
- [ ] **Sacred Beast Mobs** — Custom entity with path-specific behavior, three tiers, path drops
- [ ] **New Ores** — All sacred materials from Cradle (ore block + item textures)
- [ ] **Half-Silver Weapons** — Craftable via Soulsmith system (weapon models + textures)
- [ ] **Badges** — Craftable, appear on character model + nametag, show path/rank
- [x] **Goldsigns** — ~~Visual cosmetics per path at Lowgold~~ (Done in Save 45)
- [ ] **Custom Duel Arenas** — 15 path-matchup designs (structure blocks)

---

## Lore Accuracy Rework

Making the mod more faithful to the books, organized by priority.

### Tier 1 — "It's not Cradle without these"

- [x] **Remnants** — ~~Sacred artists/mobs drop Remnants on death. Absorb compatible ones for Lowgold. Remnant entities with path-specific appearance~~ (Done in Save 45)
- [x] **Goldsigns** — ~~Visible cosmetic changes when reaching Gold via Remnant. Path-specific~~ (Done in Save 45)
- [ ] **Soulfire** — Lord realm resource bar. Enhances abilities, reforges body at each Lord breakthrough. Visible secondary resource
- [ ] **Foundation Cycling Fix** — Foundation can only cycle pure madra internally (no aura). Aura cycling unlocks at Copper

### Tier 2 — "Fans will really notice if these are missing"

- [ ] **Jade Senses** — Spiritual radar. See player madra levels, sense sacred artists/mobs through walls
- [ ] **Sacred Beasts** — Properly advancing animals (not just Dreadbeasts). Tiered mobs that scale with their own stage. *Needs textures*
- [ ] **Contracts** — Bond with a sacred beast at Gold. Share cores, eyes change color, beast companion
- [ ] **Cycling Techniques** — Discoverable techniques that improve cycling speed/efficiency. Technique scrolls found in world
- [ ] **Half-Silver** — Weapons that disrupt/cut through madra. Ignore madra-based defenses. *Needs textures*
- [ ] **Copper Sight Rework** — Better aura visualization at Copper
- [ ] **Iron Body Rework** — Cycling-based instead of crystal pickup (Basic / Perfect / Advanced tiers)
- [ ] **Jade Spiral Core** — Cycling mini-game to create spiral pattern (better technique = stronger foundation)
- [ ] **Highgold Remnant Digestion** — Mini-progression within Gold

### Tier 3 — "Would make it incredible"

- [ ] **Soulsmith Crafting** — Full crafting system for sacred materials. Special workbench, Remnant parts as ingredients
- [ ] **Split Core** — Hollow King signature. Two smaller madra pools, switch between pure and path madra
- [ ] **Hunger Madra / Dreadgods** — World bosses, hunger madra as dangerous aura type, Monarchs generate it passively
- [ ] **Suppression Field** — Zone effect that caps power level. Great for balanced PvP
- [ ] **Scripts & Constructs** — Madra-powered items: proximity alarms, light constructs, cloud-ships
- [ ] **Soulspace Inventory** — Spiritual ender chest at Underlord
- [ ] **Revelation System Rework** — Player types answers, quality affects power
- [ ] **Icon Training** — Earn Sage through gameplay actions instead of button press
- [x] **Herald Remnant Fight** — ~~Boss fight against your own Remnant mirror~~ (Done, uses RemnantEntity)
- [x] **Three Paths to Lowgold** — ~~Remnant absorption or Natural accumulation~~ (Done in Save 45, Sacred beast contract still todo)
- [ ] **Monarch Hunger Aura** — Passive environmental effects, attracts Dreadbeasts
- [ ] **Ascension Portal** — Endgame portal to new dimension or server event

---

## Ideas / Maybe Later

Brainstorms not committed to — captured for future reference.

- [ ] **Rare Aura Types** — Death, Shadow, Dream auras that could enable entirely new Paths
- [ ] **Aura Density Zones** — Special high-aura areas for faster cycling (power-leveling spots)
- [ ] **Truegold Mastery Trial** — Prove mastery before entering Lord realm
- [ ] **Soulfire Body Reforging Visuals** — Each Lord advancement triggers visible upgrade (particles, glow, stat boosts)

---

## Known Issues

- [ ] Dreadbeast Sheep disabled (incomplete textures, crash on load)
- [ ] Sheep hostile check commented out in `isHostile()`
```

**Step 2: Verify the file reads correctly**

Run: `head -5 docs/ROADMAP.md`
Expected: The new header and subtitle

---

### Task 2: Delete Redundant Files

**Files:**
- Delete: `docs/IDEAS.md`
- Delete: `docs/LORE-ACCURACY-PLAN.md`

**Step 1: Delete the files**

```bash
rm docs/IDEAS.md docs/LORE-ACCURACY-PLAN.md
```

**Step 2: Verify they're gone**

```bash
ls docs/
```

Expected: `DESIGN.md  LORE.md  PROGRESS.md  ROADMAP.md  plans/`

---

### Task 3: Update CLAUDE.md Documentation Section

**Files:**
- Modify: `CLAUDE.md` (lines 116-122)

**Step 1: Replace the Documentation section**

Find:
```markdown
## Documentation

- `PLAN.md` — Current development phases and implementation steps
- `docs/DESIGN.md` — Visual and mechanical design philosophy
- `docs/LORE.md` — Cradle universe lore accuracy notes
- `docs/ROADMAP.md` — Future feature plans
- `docs/PROGRESS.md` — Current progress tracker
```

Replace with:
```markdown
## Documentation

- `docs/DESIGN.md` — Visual and mechanical design philosophy
- `docs/LORE.md` — Cradle universe lore reference
- `docs/ROADMAP.md` — Everything left to build (single source of truth)
- `docs/PROGRESS.md` — Completed features tracker
- `docs/plans/` — Design documents from brainstorming sessions
```

---

### Task 4: Commit

**Step 1: Stage and commit**

```bash
git add docs/ROADMAP.md docs/plans/2026-03-08-docs-reorganization-design.md docs/plans/2026-03-08-docs-reorganization.md CLAUDE.md
git rm docs/IDEAS.md docs/LORE-ACCURACY-PLAN.md
git commit -m "Reorganize docs: merge IDEAS + LORE-ACCURACY-PLAN into ROADMAP, add docs/plans/"
```

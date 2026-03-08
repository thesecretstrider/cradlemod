# Cradle Mod — Completed Features

> Everything that's been built so far (through Save Game 44)

---

## Core Systems

- **Path Selection & Advancement** — 5 paths, 13 stages (Foundation to Monarch)
- **Madra System** — Custom resource bar (replaces XP bar), grows with advancement
- **Cycling** — Passive + active cycling (G key), environmental biome bonuses
- **Willpower System** — Secondary resource for Sage Authority (Archlord+)
- **Data Persistence** — Auto-save, world save/load hooks
- **Networking & GUI** — Madra bar, Info screen (J key), cycling keybind (G key)

## Ability System

- **Skill Tree (Phase 15)** — Data-driven radial tree UI (K key), 6 slots, stage-gated unlocks, upgrade points, branching at level 10, mastery memory (swap paths without losing levels), 33 total abilities (1 starter + 15 base + 15 branches + 2 universal)
- **All 15 Path Abilities (Phase 11)** — Enforcer (Z), Striker (X), Ruler (C) for all 5 paths
- **Charge-Up System** — Hold Striker keys for up to 3x damage at higher madra cost
- **Striker Cooldown Scaling** — Per-ability cooldowns
- **6 Ability Slots** — Z, X, C, R, F, T keybinds
- **Unified AbilityExecutor** — Replaced legacy individual handlers (cleaned up Save 44)

## Advanced Progression

- **Iron Body System (Phase 10)** — Bloodforged/Steelborn/Raindrop crystals, P key toggle
- **Revelation Trial (Phase 12)** — Lord breakthroughs with personal questions, Sage/Herald branching
- **Sage Authority** — V key: tap = Stop, hold = Kill. Path-specific flavor words
- **Icon Selection System** — Choose your Icon at Sage
- **Herald Spirit Shift** — B key form shifting
- **Monarch World Event** — Endgame event system
- **Flight** — Cloud Hammer at Low Gold, all others at Archlord

## Combat & Multiplayer

- **Duel System (Phase 14)** — Player dueling with win/loss tracking
- **Combat XP** — XP from fighting
- **Sage vs Sage** — Authority can be countered by other Sages spending willpower

## World Features

- **Vital Aura** — 7 types (Fire, Water, Earth, Wind, Life, Force, Blood) mapped to biomes
- **Copper Sight** — H key to see colored aura particles in the world
- **Spirit Fruit Bushes** — Natural worldgen
- **Spirit Stones** — Mineable, fuel advancement
- **Dreadbeast Wolves** — Hostile retextured wolves
- **Dreadbeast Cows** — Hostile retextured cows
- **Blackflame Cooking** — Special cooking mechanic

## Visuals & HUD

- **Madra Bar** — Path-colored, smooth fill animation
- **Willpower Bar** — Secondary bar for Archlord+
- **Ability Slot Bar** — HUD with cooldown overlays and charge indicators
- **Cycling Particles** — Path-specific colors, inward motion (Hollow King circulates outward/inward)
- **Breakthrough Visuals** — Stage-up effects scale with advancement
- **Info Screen (J key)** — Scrollable status panel
- **Story/Lore Messages** — Welcome screen, environmental cycling bonuses
- **Action Bar Messages** — Clean, non-intrusive notifications

## Recent Changes (Save 43-44)

- **Save 44** — Removed ~140 lines of dead legacy ability code (old Enforcer/Striker/Ruler handlers replaced by unified system)
- **Save 43** — Updated spirit stone texture, disabled Dreadbeast Sheep (incomplete textures causing crash)

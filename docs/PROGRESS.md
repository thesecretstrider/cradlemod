# Cradle Mod — Completed Features

> Everything that's been built so far (through Save Game 47)

---

## Core Systems

- **Path Selection & Advancement** — 5 paths (Black Flame, Endless Sword, Stellar Spear, Cloud Hammer, Hollow King), 13 stages (Foundation through Monarch)
- **Madra System** — Custom resource bar (replaces XP bar), path-colored, grows with advancement
- **Cycling** — Passive + active cycling (G key), environmental biome bonuses, Hollow King internal circulation
- **Willpower System** — Secondary resource for Sage Authority (Archlord+)
- **Data Persistence** — NBT serialization to `cradlemod_playerdata.dat`, auto-save, world save/load hooks
- **Networking** — 25 custom payload classes (C2S + S2C), CradleSyncPayload every tick with ~17 fields via bitmask flags

## Ability System

- **22 Abilities Total** — 1 starter + 15 path-specific + 15 branch abilities + 2 universal
  - **Basic Enforcement** — Foundation starter, +armor/+speed
  - **15 Path Abilities** — Enforcer (Z), Striker (X), Ruler (C) for all 5 paths, unlocked at Copper
  - **15 Branch Abilities** — Evolved specializations unlocked at upgrade level 10 (Underlord+)
  - **2 Universal Abilities** — Madra Shield and Spirit Pulse (Underlord+, any path)
- **Skill Tree (K key)** — Radial tree UI, 6 slots, stage-gated unlocks, upgrade points (1-20), branching at level 10, mastery memory (swap paths without losing levels)
- **6 Ability Slots** — Z, X, C, R, F, T keybinds
- **Charge-Up System** — Hold Striker keys for up to 3x damage at higher madra cost (60-tick max charge)
- **Striker Cooldown Scaling** — Per-ability cooldowns
- **Unified AbilityExecutor** — Server-side validation, madra cost/cooldown checks, handler delegation

## Advanced Progression

- **Iron Body System** — Bloodforged/Steelborn/Raindrop crystals, P key toggle, 3 types with distinct effects
- **Revelation Trial** — Lord breakthroughs: Underlord (3 Vex), Overlord (5 Phantom), Archlord (10 Zombie), 1000-block distance limit
- **Sage Authority** — V key: tap = Stop, hold = Kill. Path-specific flavor words, willpower cost
- **Icon Selection** — 12 icons (path-specific + universal Heart/Shield) chosen at Sage
- **Herald Spirit Shift** — B key form shifting via SpiritShiftMixin
- **Herald Remnant Fight** — Boss fight against personal Remnant mirror for Herald advancement
- **Sage vs Sage** — Authority can be countered by other Sages spending willpower
- **Monarch World Event** — Endgame event system
- **Flight** — Cloud Hammer at Low Gold, all others at Archlord

## Remnant System (Save 45-47)

- **RemnantEntity** — Ghostly hostile mob with path-colored tint, scaled stats (HP 20-85, attack 3-22.5), fire immune
- **Spawn on Player Death** — Copper+ sacred artists leave a Remnant at death location (power = stage ordinal)
- **Mob Remnants** — 15% chance on non-sacred-beast mob kills (8+ HP), path based on biome vital aura
- **Two Lifecycle Phases** — Camp phase (5 min at death spot) then roaming phase (wanders killing hostile mobs)
- **Absorption Channeling** — 60-tick (3s) channel within 3 blocks, Jade+ only
- **Gold Advancement Rework** — Remnant absorption (primary path to Gold) vs natural accumulation (harder)
- **Goldsign System** — 5 path-specific visual marks granted on Remnant absorption at Gold:
  - Black Flame Eyes, Sword Arms, Spear Light, Crackling Skin, Pale Aura
- **Client Renderer** — Translucent humanoid with path-colored tint
- **Debug Logging** — Remnant spawn tracking for troubleshooting (Save 47)

## Combat & Multiplayer

- **Duel System** — `/duel` command with invite/accept/decline/forfeit, friendly + competitive modes
- **Duel Arena** — 50x50 block arena construction with terrain restoration
- **Win/Loss Tracking** — Persistent duel record per player
- **Forfeit System** — Available after 2-minute minimum
- **Combat XP** — XP from fighting

## World Features

- **Vital Aura** — 7 types (Fire, Water, Earth, Wind, Life, Force, Blood) mapped to biomes
- **Copper Sight** — H key to see colored aura particles in the world (Copper+)
- **Spirit Fruit Bushes** — Natural worldgen via Fabric BiomeModifications (VEGETAL_DECORATION)
- **Vital Fruit Bushes** — Additional worldgen bush type
- **Spirit Stones** — Mineable, fuel advancement, added to chest loot tables
- **Revelation Items** — Underlord/Overlord/Archlord Revelation items (unsackable)
- **Iron Body Crystals** — Bloodforged, Steelborn, Raindrop (block + item forms)
- **Dreadbeast Wolves** — Hostile retextured wolves via mixin
- **Dreadbeast Cows** — Hostile retextured cows via mixin
- **Dreadbeast Sheep** — Mixin exists but disabled (incomplete textures, crash on load)

## Entities

- **StrikerProjectile** — Path-specific projectile for all Striker abilities (0.5x0.5, fire immune)
- **RemnantEntity** — Ghostly spirit mob (0.6x1.8, 8-block tracking, monster category)

## Visuals & HUD

- **Madra Bar** — Path-colored, smooth fill animation, replaces XP bar via MadraBarMixin
- **Willpower Bar** — Secondary bar for Archlord+
- **Ability Slot Bar** — Bottom-left HUD: 6 slots (22x22), type-colored (green/red/blue), cooldown overlays, charge indicators, key labels, active glow with pulsing
- **HUD Render Order** — HUD renders behind chat overlay for clean layering (Save 47)
- **Cycling Particles** — Path-specific colors, inward motion (Hollow King circulates outward/inward)
- **Breakthrough Visuals** — Stage-up effects scale with advancement
- **Custom Glow Colors** — GlowColorMixin for path-specific glow effects
- **Entity Renderers** — StrikerProjectileRenderer, RemnantRenderer (translucent + path tint)

## Screens & GUI

- **Info Screen (J key)** — Scrollable status panel: stage, path, madra, level, XP, iron body, Goldsign, advance/Sage/Herald buttons, skill tree link
- **Skill Tree Screen (K key)** — Radial layout: center hub (54px), inner ring (48px), outer ring (44px, Underlord+), branch indicators (36px), bottom panel for selection/upgrade/branch
- **Path Selection Screen** — 5 path options with lore descriptions, shown on first join
- **Icon Selection Screen** — 12 icons with lore descriptions and path-specific colors
- **Ability Picker Screen** — Stage-gate ability selection at Copper/Iron/Low Gold
- **Welcome Screen** — Onboarding overlay with word-wrapped text

## Commands

- **`/cycle`** — `start`, `stop`, `info`, `setlevel`, `setstage`, `setpath`, `setmadra`
- **`/duel`** — `invite [friendly|competitive]`, `confirm`, `cancel`, `accept`, `decline`, `forfeit`, `stats [player]`

## Keybinds (11 total)

| Key | Action |
|-----|--------|
| G | Toggle Cycling |
| J | Open Info Screen |
| K | Open Skill Tree |
| H | Toggle Copper Sight (Copper+) |
| P | Toggle Iron Body |
| Z/X/C/R/F/T | Ability Slots 1-6 |
| V | Sage Authority (tap=Stop, hold=Kill) |
| B | Herald Spirit Shift |

## Infrastructure

- **Split Source Sets** — `src/main/` (server/shared) + `src/client/` (client-only)
- **Mixin System** — 7 mixins: CradleMixin, MadraBarMixin, SpiritShiftMixin, GlowColorMixin, DreadbeastWolfMixin, DreadbeastCowMixin, DreadbeastSheepMixin
- **Client State Cache** — ClientCradleData + ClientLoadoutData for synced player state
- **Debug Command** — `/cycle` for setting player state during development

---

## Recent Changes

| Save | Changes |
|------|---------|
| **47** | HUD renders behind chat overlay, Remnant spawn debug logging |
| **46** | Added .claude helper scripts and backup assets |
| **45** | Full Remnant system: entity, spawning, absorption, Gold advancement rework, Goldsign system, roaming AI, client renderer, documentation |
| **44** | Removed ~140 lines of dead legacy ability code (old Enforcer/Striker/Ruler handlers) |
| **43** | Updated spirit stone texture, disabled Dreadbeast Sheep (incomplete textures) |

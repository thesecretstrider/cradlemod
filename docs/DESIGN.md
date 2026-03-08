# Cradle Mod — Design Notes

> How the core systems are designed to look and feel

---

## Madra System

Madra is a separate resource from health and hunger. Players generate it by cycling and spend it on abilities.

**Storage:** `CradlePlayerData` holds `currentMadra`, `maxMadra`, `level`, `cyclingXP`, `advancementStage`, `chosenPath`

**Flow:** Player cycles -> gains Madra -> spends Madra -> activates Path-specific abilities -> powers scale with level and stage

### Madra Bar

- Displayed above the hotbar (replaces XP bar visually)
- Smooth fill animation, not instant jumps
- Color changes by Path:
  - **Black Flame** — dark flame/orange glow
  - **Endless Sword** — white/silver
  - **Stellar Spear** — gold/light
  - **Cloud Hammer** — pale blue/grey
  - **Hollow King** — pale/faint white
- Slight glow when near full

### Generating Madra

- **Passive cycling** — small gain over time
- **Active cycling (G key)** — faster gain, also feeds cycling XP
- **Environmental bonuses** — cycling in biomes matching your Path gives bonus madra
- Hollow King: reduced ability costs instead of environmental bonuses

### Spending Madra

- Each ability has a `madraCost` value
- Costs scale with ability power and advancement stage
- "Not enough Madra!" feedback when insufficient

---

## Cycling Visual Design

**Core idea:** Cycling should feel like drawing energy from the world, not generating it internally. Calm, controlled, focused — not explosive.

### Standard Paths (Environmental Absorption)

- Particles spawn in 2-5 block radius around player
- Particles slowly move **inward** toward the player
- Disappear when reaching the player
- Motion: Environment -> Player center

**Path-specific visuals:**
- **Black Flame** — Dark embers/smoke, heat distortion, strong pull
- **Endless Sword** — Thin straight streaks, clean and precise
- **Stellar Spear** — Small bright light particles, sharp motion
- **Cloud Hammer** — Soft mist/cloud, heavier and slower

### Hollow King Exception (Internal Circulation)

Pure madra can't be absorbed from the environment, so:
- Particles originate **from** the player
- Move outward briefly, then curve back inward
- Motion: Player -> outward -> returning to player
- Very subtle, pale, minimal visual noise

### Stage Scaling

| Stage | Visual Strength |
|-------|----------------|
| Foundation | Minimal, small local effect |
| Copper | Small glow, visible cycling nearby |
| Iron | Visible aura, energy moving from surroundings |
| Jade | Strong aura and particles |
| Gold+ | Distinct visual identity, large area |

---

## Ability Visual Design

Each Path has its own visual identity:

- **Black Flame** — Dark fire particles, flame trails, explosions
- **Endless Sword** — Quick slash effects, sharp particle lines
- **Stellar Spear** — Light streaks, piercing beams
- **Cloud Hammer** — Shockwaves, impact particles
- **Hollow King** — Minimal, distortion or subtle flashes

### Breakthrough Visuals

Stage-up effects scale with significance:
- Foundation -> Copper: small flash
- Copper -> Iron: stronger pulse
- Iron -> Jade: visible aura for several seconds
- Jade -> Gold: large burst with sound and particles

---

## Implementation Notes

- Particle spawning is client-side for performance
- Server only syncs: cycling state, chosen Path, advancement stage
- Particle movement interpolates toward/away from player based on Path
- HUD overlay rendered every frame, reads synced madra values
- Abilities triggered via hotkeys, check madra >= cost before activating

---

## Controls Reference

| Key | Action |
|-----|--------|
| G | Toggle Cycling |
| J | Open Info Screen |
| K | Open Skill Tree |
| H | Toggle Copper Sight (Copper+) |
| P | Toggle Iron Body |
| Z | Ability Slot 1 (Enforcer) |
| X | Ability Slot 2 (Striker) — hold to charge |
| C | Ability Slot 3 (Ruler) |
| R / F / T | Ability Slots 4-6 |
| V | Sage Authority (tap = Stop, hold = Kill) |
| B | Herald Spirit Shift |

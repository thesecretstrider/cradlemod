# Remnant System Overhaul Design

**Date:** 2026-03-08
**Status:** Approved

## Summary

Overhaul remnants from generic translucent humanoids into mob-shaped spirits with glowing eyes, stage-relative damage scaling, ability-using AI, and a reworked Herald trial. Introduces the Sacred Beast concept for passive mobs (deferred to separate implementation).

## 1. Mob-Shaped Remnant Visuals

### Entity Data Changes (RemnantEntity)

- New synced field: `SOURCE_MOB_TYPE` (String) — registry name of source mob (e.g., `"minecraft:spider"`, `"cradlemod:player"`)
- New synced field: `RENDER_SCALE` (Float) — defaults to 1.0, set to 2.0 for Herald remnants
- Hitbox adjusts to match source mob dimensions (spider is wide/low, enderman is narrow/tall)

### Renderer (RemnantRenderer rewrite)

- Resolve source mob type to its vanilla EntityRenderer model
- Apply translucent path-colored tint (existing 50% alpha system)
- Render eye overlay texture as emissive layer (full-bright, no lighting darkening)
- Scale entire model by `RENDER_SCALE`

### Eye Overlay Textures

- One transparent PNG per supported mob: `textures/entity/remnant_eyes/<mob_name>.png`
- Same UV layout as mob's base texture, fully transparent except eye pixels
- Eyes rendered bright white, tinted to path color at render time
- Unsupported mobs get generic glowing dot pair at head position

#### Supported Mobs (~20)

zombie, skeleton, spider, creeper, enderman, blaze, witch, guardian, phantom, pillager, warden, iron_golem, cow, pig, sheep, chicken, wolf, horse, villager, ravager

### Ambient Particles

- Existing path-colored dust particles continue spawning around remnant
- No changes to particle system

## 2. Stage-Relative Damage Scaling

All remnant damage is relative to the gap between remnant power level and victim stage.

### Formula

```
stageGap = remnantPowerLevel - victimStageOrdinal

if stageGap >= 0:
    damage = 2.0 + (stageGap * 4.5)
else:
    damage = max(1.0, 2.0 + (stageGap * 0.5))
```

### Damage Table (Monarch remnant, power=13)

| Victim Stage     | Gap | Damage | Hearts |
|------------------|-----|--------|--------|
| Monarch (13)     | 0   | 2.0    | 1.0    |
| Herald (12)      | +1  | 6.5    | 3.25   |
| Sage (11)        | +2  | 11.0   | 5.5    |
| Archlord (10)    | +3  | 15.5   | 7.75   |
| Overlord (9)     | +4  | 20.0   | 10.0   |
| Underlord (8)    | +5  | 24.5   | 12.25  |
| Truegold (7)     | +6  | 29.0   | 14.5   |
| Highgold (6)     | +7  | 33.5   | 16.75  |
| Lowgold (5)      | +8  | 38.0   | 19.0   |
| Jade (4)         | +9  | 42.5   | 21.25  |
| Iron (3)         | +10 | 47.0   | 23.5   |
| Copper (2)       | +11 | 51.5   | 25.75  |
| Foundation (1)   | +12 | 56.0   | 28.0   |

### Health Scaling (updated)

```
health = 20.0 + (10.0 * powerLevel)
```

Monarch remnant: 150 HP (75 hearts).

## 3. Remnant Ability AI

### Ability Count by Stage

| Stage Range           | Abilities |
|-----------------------|-----------|
| Copper – Iron         | 0 (melee) |
| Jade – Lowgold        | 1         |
| Highgold – Truegold   | 2         |
| Underlord – Overlord  | 3         |
| Archlord – Sage       | 4         |
| Herald                | 5         |
| Monarch               | 6 (full)  |

### Data Storage

- On player death, remnant copies `PlayerLoadout` (6 slots + upgrade levels) into NBT
- Remnant gets `madraPool` equal to dead player's max madra
- Per-ability cooldowns tracked on remnant, using same values as player abilities

### Range-Based Tactical AI

- **Close (< 4 blocks):** Prefer Enforcer abilities (melee buffs, body enhancement)
- **Mid (4–10 blocks):** Prefer Striker abilities (projectile attacks)
- **Long (10+ blocks):** Prefer Ruler abilities (area effects, pulls/pushes)
- Fallback: use whatever is off cooldown and has madra cost available
- If nothing available: melee attack
- Decision tick rate: every 20 ticks (1 second), re-evaluate based on target distance

### Ability Execution

- Adapts `AbilityExecutor` logic for entity context (not player)
- Remnant faces target when using directional abilities
- Path-colored particle effects render normally

## 4. Herald Trial Rework

Replace Iron Golem boss with proper RemnantEntity:

- `sourceType = "cradlemod:player"` (humanoid model)
- `renderScale = 2.0` (twice player size)
- Copies trial player's full 6-slot loadout, path, madra pool (1.5x multiplier)
- Health: 200 HP (100 hearts)
- Custom name: `"<PlayerName>'s Remnant"`
- Glowing eyes + translucent path-color tint on player model
- Does NOT use stage-relative damage scaling (fixed boss difficulty)
- On death: triggers Monarch advancement (existing logic)

### Death Count Scaling

`remnantDeathCount` (already tracked but unused) now affects Herald trial:

- Each prior death: +25 HP and +10% madra pool to Herald remnant
- Example: 3 prior deaths = 275 HP, 1.8x madra pool

## 5. Sacred Beasts (Deferred)

**Concept:** Passive mobs that have lived long enough in high-aura chunks absorb enough aura to become Sacred Beasts. Sacred Beasts can leave remnants on death (same 15% chance as hostiles). Their remnants use the passive mob's model with standard ghost treatment.

This is a separate feature requiring its own design for:
- Aura absorption tracking per entity
- Sacred Beast visual indicators
- Sacred Beast behavior changes (if any)
- Interaction with the cycling/aura system

## Architecture: Polymorphic RemnantEntity

Single `RemnantEntity` class stores source mob type. Renderer dynamically loads the corresponding mob model and applies ghost treatment. No per-mob entity subclasses.

### Key Files to Modify

| File | Changes |
|------|---------|
| `RemnantEntity.java` | Add source mob type, render scale, loadout storage, ability AI, damage formula |
| `RemnantRenderer.java` | Full rewrite: dynamic model resolution, eye overlay, scale support |
| `RemnantRenderState.java` | Add source mob type, render scale fields |
| `RemnantManager.java` | Pass source mob type on spawn, copy player loadout |
| `RevelationTrialManager.java` | Replace Iron Golem with RemnantEntity, apply death count scaling |
| `AbilityExecutor.java` | Adapt for entity context (new overloads or interface) |
| `CradleEntities.java` | Update entity dimensions handling for dynamic sizing |

### New Files

| File | Purpose |
|------|---------|
| `RemnantAbilityAI.java` | Range-based ability decision logic |
| `RemnantModelResolver.java` | Maps mob types to model/renderer references |
| `textures/entity/remnant_eyes/*.png` | ~20 eye overlay textures |

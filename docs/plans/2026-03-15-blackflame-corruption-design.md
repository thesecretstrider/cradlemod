# Blackflame Corruption — Design Document

> Lore-accurate mechanic: Blackflame madra consumes its user if not managed

---

## Overview

Gold+ Blackflame players accumulate a "Corruption" effect from using abilities and cycling. Corruption makes abilities stronger but eventually kills the player if not purged. Represents the canonical danger of the Blackflame path — the entire Blackflame family was destroyed by their own madra.

## When It Activates

- **Stage gate:** Gold and above only (when the Blackflame Remnant is absorbed)
- **Path:** Blackflame only
- Pre-Gold Blackflame players are unaffected

## Corruption Mechanic

- **10 tiers** displayed as a potion-style effect (Corruption I through X)
- **Pacing:** ~40 minutes of normal play from tier 0 to tier X (death)
- **Sources:**
  - Using Blackflame abilities (adds corruption per use)
  - Active cycling (adds corruption while cycling)
- **No natural decay** — corruption never decreases on its own

## Power Boost

- Linear damage scaling: **+15% ability damage per corruption tier**
- Corruption I = +15%, Corruption V = +75%, Corruption X = +150%
- Cap of +150% at max corruption (tunable — start here and adjust down if too strong)
- Only affects Blackflame ability damage, not melee or other sources

## Visual Effect — Screen Darkening

Progressive vignette overlay rendered client-side:

| Tier | Visual |
|------|--------|
| I–III | Subtle dark vignette at screen edges |
| IV–VI | Vignette deepens, encroaches further inward |
| VII–VIII | Only a shrinking rectangle of clear vision in center |
| IX | Small square of visibility remaining |
| X | Screen goes fully black → blackout death triggers |

## Blackout Death

This is NOT a standard Minecraft death:

1. Screen fades to full black
2. Player is teleported to their spawn point
3. **No death screen** shown
4. **Items are kept** — no drops
5. **No Remnant spawns** — the corruption consumed the spirit entirely
6. Corruption resets to 0
7. A **15–20 block radius spherical crater** appears at the blackout location
   - Terrain destroyed as if the madra vented explosively
   - Damages entities caught in the blast radius
   - Should respect server explosion protection rules

## Purging Corruption

### 1. Vital Fruit Juice (Solo Method)

- **Recipe:** Water Bottle + Vital Fruit (shapeless crafting)
- **Effect:** Clears **2 corruption tiers** per use
- Player at Corruption VIII needs 4 juices to fully cleanse
- Encourages carrying a supply rather than one-shot full reset

### 2. Hollow King Cleansing Ability (Multiplayer Method)

- A specific Hollow King ability that targets a Blackflame player
- Reduces their corruption (amount TBD, likely more effective than juice)
- Creates cross-path social dependency (mirrors Lindon + Little Blue dynamic)
- Hollow King players become valuable allies for Blackflame players

### 3. Little Blue Sacred Beast (Future)

- Pure madra companion that passively cleanses corruption
- Tied to Sacred Beast + Contracts systems (not yet implemented)

## Fruit Farming (Sub-Feature)

To support the Vital Fruit Juice supply chain:

- Spirit fruits and vital fruits can now be **planted** (place fruit on ground → grows into bush)
- **Farmed bushes** drop 1–3 fruits on harvest
- **Wild-spawned bushes** still drop only 1 fruit
- Gives Blackflame players a sustainable supply without trivializing early gathering

## Implementation Notes

### Server-Side
- New `corruptionLevel` field in `CradlePlayerData` (int, 0–10, persisted in NBT)
- Corruption ticking in `CyclingManager` (increment during cycling)
- Corruption increment in `AbilityExecutor` (on Blackflame ability use)
- Blackout death handler: teleport, crater generation, no Remnant spawn
- Damage multiplier: `1.0 + (corruptionLevel * 0.15)` applied in ability handlers

### Client-Side
- Corruption level synced via `CradleSyncPayload` (new field or flag)
- Vignette overlay renderer (scales opacity/size with corruption level)
- Potion-style effect icon in HUD

### Networking
- Corruption level added to `CradleSyncPayload` composite codec
- No new C2S payloads needed (corruption is server-driven)

## Lore Justification

In the Cradle series, Blackflame madra is uniquely destructive — it burns everything, including the user. The Blackflame family went extinct because they couldn't control it. Lindon survives by using his pure madra core (later Little Blue) to flush the destructive madra from his channels. This mechanic captures that tension: immense power at the cost of self-destruction.

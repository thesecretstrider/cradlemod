# Cradle Mod — Master Plan

## Phase 10: Iron Body System ✅ COMPLETE
- Bloodforged (Instant Health II on P, 30s cooldown), Steelborn (Resistance I toggle), Raindrop (Speed I toggle)
- Event-driven crystal spawning (rain/thunder/mob kill)
- Crystal blocks (cross-shaped, light-emitting), breakthrough integration
- P keybind, /cycle setironbody command

## Phase 11: Path Abilities (Enforcer / Striker / Ruler)

Each path has 3 abilities that consume Madra. All abilities are unlocked based on advancement stage.

### Ability Types
- **Enforcer** — Self-buff that enhances the body. Toggle on/off, drains Madra over time while active.
- **Striker** — Ranged projectile/attack. One-shot, costs Madra per use, has cooldown.
- **Ruler** — Area-of-effect zone around the player. Costs Madra to activate, lasts for a duration.

### Keybind Layout
- **R** — Enforcer technique (toggle)
- **T** — Striker technique (fire)
- **Y** — Ruler technique (activate)

### Path of Black Flame
1. **Enforcer — Burning Body**: +attack damage, +sprint speed, attacks ignite enemies. Madra drains quickly, small self-damage over time. High-risk power boost.
2. **Striker — Blackflame Burst**: Short-range explosive projectile. High damage in small area, applies fire DoT. High madra cost.
3. **Ruler — Domain of Ash**: Area around player damages enemies over time with fire. Enemies inside burn slowly. Area remains briefly after activation.

### Path of the Endless Sword
1. **Enforcer — Flowing Edge**: +attack speed, reduced attack cooldown. Moving while attacking increases damage. Missing attacks briefly reduces effectiveness.
2. **Striker — Endless Slash**: Fast horizontal slash projectile. Medium range. Can hit multiple enemies in a line.
3. **Ruler — Field of Blades**: Enemies moving near the player take small repeated damage. Zone control through motion.

### Path of the Stellar Spear
1. **Enforcer — Stellar Alignment**: +forward speed, increased reach/forward lunge. Bonus damage while sprinting forward. Reduced knockback taken.
2. **Striker — Piercing Star**: Long-range piercing projectile. Passes through enemies. Damage slightly reduced per target hit.
3. **Ruler — Spear Domain**: Enemies moving directly toward the player take damage. Rewards positioning and facing enemies.

### Path of the Cloud Hammer
1. **Enforcer — Thunderous Weight**: +armor, +knockback dealt. Reduced movement speed. Charged attacks deal bonus damage.
2. **Striker — Falling Hammer**: Delayed area strike from above. High knockback. Strong single impact.
3. **Ruler — Gravity Field**: Enemies inside area move slower, jump height reduced. Crowd control.

### Path of the Hollow King (Pure Madra)
1. **Enforcer — Hollow Circulation**: Reduced madra cost for other abilities, passive madra regen, slight damage reduction. No offensive bonus.
2. **Striker — Empty Palm**: Short-range shockwave. Moderate damage. Temporarily weakens enemy attacks or armor.
3. **Ruler — Hollow Domain**: Reduces incoming damage in area, slows enemy effects. Improves allied madra efficiency in multiplayer.

### Stage Unlock Requirements
- **Foundation** — No abilities available
- **Copper** — Enforcer technique unlocked
- **Iron** — Striker technique unlocked
- **Jade** — Ruler technique unlocked
- **Gold** — All techniques enhanced (stronger effects, lower cost)

### Implementation Steps

#### Step 1: Ability Data Model
- Add ability cooldown tracking to `CradlePlayerData`
- Add enforcer active state (boolean)
- Define Madra costs, cooldowns, and durations per ability per path

#### Step 2: Keybinds + Payloads
- Register R, T, Y keybinds in `CradleModClient`
- Create C2S payloads: `UseEnforcerPayload`, `UseStrikerPayload`, `UseRulerPayload`
- Server handlers validate stage requirements, check Madra, apply effects

#### Step 3: Enforcer Techniques (R key — Toggle)
- Server-side: toggle enforcer state, apply/remove attribute modifiers and effects
- Madra drain per tick while active
- Path-specific effects via attribute modifiers and potion effects
- Burning Body: +damage, +speed, fire aspect on attacks, self-damage
- Flowing Edge: +attack speed, movement-damage bonus
- Stellar Alignment: +speed, +reach, sprint damage bonus
- Thunderous Weight: +armor, +knockback, -speed
- Hollow Circulation: -madra cost, +madra regen, +damage reduction

#### Step 4: Striker Techniques (T key — Projectile)
- Custom projectile entity (or use Snowball-like approach)
- Path-specific projectile behavior:
  - Blackflame Burst: short range, explodes on impact, sets fire
  - Endless Slash: medium range, pierces mobs in a line
  - Piercing Star: long range, passes through all enemies
  - Falling Hammer: targeted area, delayed impact from above
  - Empty Palm: short-range cone/shockwave, debuffs target

#### Step 5: Ruler Techniques (Y key — AoE Zone)
- Area effect applied around the player for a duration
- Path-specific zone effects:
  - Domain of Ash: damage + fire in radius
  - Field of Blades: damage to moving enemies in radius
  - Spear Domain: damage to enemies approaching player
  - Gravity Field: slowness + reduced jump in radius
  - Hollow Domain: damage reduction aura + enemy debuff

#### Step 6: Networking + GUI
- Sync ability cooldowns and enforcer state to client
- Show active abilities on J info screen
- Cooldown indicators (chat messages or HUD element)

#### Step 7: Commands
- `/cycle useability <enforcer|striker|ruler>` — for testing

## Future Phases
- **Phase 12**: Body Moves — Iron Body-specific combat abilities (no Madra cost)
- **Crystal Beacon Beam** (deferred cosmetic from Phase 10 Step 4)

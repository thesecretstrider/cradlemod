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
- [ ] **Blackflame Corruption** — Gold+ Blackflame players accumulate Corruption (10 tiers) from abilities and cycling. +15% damage per tier (up to +150%). Screen darkens from edges inward. At tier X: blackout, teleport to spawn (no death screen, keep items, no Remnant), 15-20 block crater at location. Purged by Vital Fruit Juice (-2 tiers) or Hollow King cleansing ability. See `docs/plans/2026-03-15-blackflame-corruption-design.md`
- [ ] **Vital Fruit Juice** — New item: water bottle + vital fruit. Clears 2 corruption tiers. Primary solo purge method for Blackflame players
- [ ] **Fruit Farming** — Spirit fruits and vital fruits can be planted (place fruit → grows into bush). Farmed bushes drop 1-3 fruits; wild bushes still drop 1
- [ ] **Sound Effects** — Ability cast sounds, cycling ambient loop, breakthrough fanfare, duel start/end. Repurpose vanilla SoundEvents. Silent combat feels broken for public release
- [ ] **Server Config File** — Ability damage multiplier, cycling speed, enable/disable duels, enable/disable PvP ability damage, Remnant spawn chance, corruption rate. Server operators expect configurability
- [ ] **Dead Code Cleanup** — Remove orphaned payloads (UseEnforcerPayload, UseStrikerPayload, UseRulerPayload), unused `remnantDeathCount` field, empty duel payload handlers
- [ ] **Mod Compatibility** — Graceful failure if mixins conflict (XP bar, combat events). Document known incompatibilities. Fabric mods are expected to coexist
- [ ] **Advancement Integration** — Tie Cradle milestones to vanilla advancement system (choose path, reach Copper, absorb Remnant, reach Gold, win a duel). Free discoverability via pause menu
- [ ] **Death Screen Remnant Info** — Custom death message: "Your Remnant lingers at [coordinates]" when dying at Copper+. Players need to know to go back within 5 minutes
- [ ] **Tooltip Polish** — All mod items (spirit stones, iron body crystals, revelation items, spirit/vital fruits) need proper tooltips explaining what they do

---

## Needs Textures First

Designed but blocked on custom art/models.

- [ ] **Sword Slash Particle** — Custom particle for Endless Sword striker techniques. PNG in `textures/particle/sword_slash.png`. Needs: register particle type, particle JSON, client factory, spawn on Endless Sword striker fire
- [ ] **Dreadbeast Sheep** — 3 texture layers needed (wool/undercoat). Mixin disabled in Save 43
- [ ] **More Dreadbeast Animals** — Pig (3 files), Chicken (3), Spider (2), Goat (1), Fox (4)
- [ ] **Sacred Beast Mobs** — Custom entity with path-specific behavior, three tiers, path drops
- [ ] **New Ores** — All sacred materials from Cradle (ore block + item textures)
- [ ] **Half-Silver Weapons** — Craftable via Soulsmith system (weapon models + textures)
- [ ] **Badges** — Craftable, appear on character model + nametag, show path/rank
- [x] **Goldsigns** — ~~Visual cosmetics per path at Lowgold~~ (Done in Save 45)
- [ ] **Custom Duel Arenas** — 15 path-matchup designs (structure blocks)
- [ ] **Ancestral Trees** — Ancient trees that come alive and attack. Chopping one with an axe while it's dormant summons Remnants that attack. Custom tree model/texture + hostile entity form. *Needs tree model, animated hostile form, and bark textures*

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
- [ ] **Goldsign Growth Animation** — When advancing to Gold, goldsign visually grows/emerges over a few seconds (e.g., metallic hair sprouting from the player's head)
- [ ] **Cycling While Using Techniques** — Lore-accurate: must be cycling to use abilities (you cycle madra to power techniques), but can't *gain* madra while using moves or sprinting. Cycling only stops entirely when sprinting. Simplifies the many cycling techniques from the books into one unified mechanic
- [ ] **In-Game Guidebook** — Craftable book item explaining paths, cycling, advancement, abilities. Alternative to JEI/REI integration for player onboarding
- [ ] **Data Pack Support** — Ability definitions, cycling rates, Remnant spawn chances overridable via data pack for modpack creators
- [ ] **Duel Spectator Mode** — Allow non-participants to watch duels. Spectator camera within arena bounds
- [ ] **Accessibility — Color-Blind Support** — Shape/icon indicators alongside color for path differentiation (madra bar, ability slots, particles)
- [ ] **Little Blue Sacred Beast** — Pure madra companion that passively cleanses Blackflame corruption. Tied to Sacred Beast + Contracts systems

---

## Known Issues

- [ ] Dreadbeast Sheep disabled (incomplete textures, crash on load)
- [ ] Sheep hostile check commented out in `isHostile()`
- [ ] **Skill tree path validation broken** — Says "move is not from your path" when selecting abilities that should be available
- [ ] **Blackflame self-burn at Underlord+** — Burning Body still damages the user at Lord realm; should stop or heavily reduce at Underlord+
- [ ] **Remnant striker damage too high** — Weakest striker does ~5 hearts + burn; needs damage scaling tuned down for remnants
- [ ] **Slow Falling replacement** — Remove slow falling effect, replace with no-fall-damage only (consider cloud particle at feet instead)

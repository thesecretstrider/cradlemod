# Cradle Mod — Roadmap

> What's left to build, organized by what's blocking it

---

## Ready to Build (Code Only)

These can be implemented right now with no new art needed.

- [ ] **Soulsmith Forging** — Anvil + hammer crafting system (Steelborn crystal as byproduct)
- [ ] **More Authority Commands** — Open, Break, etc. (extends existing Sage Authority)
- [ ] **Archlord Lore Hints** — Teaser messages about Icons and spirit-body merge
- [ ] **Herald Advancement Lore** — Hint messages at Archlord about merging spirit with body
- [ ] **Dreadgod Boss Logic** — Massive boss mob AI/behavior (prototype without final textures)

---

## Needs Textures First

These features are designed but blocked on custom art/models.

- [ ] **Dreadbeast Sheep** — 3 texture layers needed (wool/undercoat). Mixin disabled in Save 43
- [ ] **More Dreadbeast Animals** — Pig (3 files), Chicken (3), Spider (2), Goat (1), Fox (4)
- [ ] **Sacred Beast Mobs** — Custom RemnantEntity with path-specific behavior, three tiers, path drops
- [ ] **New Ores** — All sacred materials from Cradle (ore block + item textures)
- [ ] **Half-Silver Weapons** — Craftable via Soulsmith system (weapon models + textures)
- [ ] **Badges** — Craftable, appear on character model + nametag
- [x] **Goldsigns** — ~~Visual cosmetics per path at Lowgold (flame eyes, blade arms, etc.)~~ (Done in Save 45)
- [ ] **Custom Duel Arenas** — 15 path-matchup designs (structure blocks)

---

## Canon Lore Accuracy Rework

Bigger changes to make the mod more faithful to the books.

- [ ] **Foundation Cycling Rework** — Only cycle pure madra internally at Foundation (no aura)
- [ ] **Copper Sight Rework** — Better aura visualization at Copper
- [ ] **Iron Body Rework** — Cycling-based instead of crystal pickup (Basic / Perfect / Advanced tiers)
- [ ] **Jade Senses** — Spiritual awareness: see other players' madra, sense through walls
- [ ] **Jade Spiral Core** — Cycling mini-game to create spiral pattern (better technique = stronger foundation)
- [x] **Remnant System** — ~~Sacred artists leave Remnants on death, absorb compatible ones for Lowgold~~ (Done in Save 45)
- [x] **Three Paths to Lowgold** — ~~Remnant absorption or Natural accumulation~~ (Done in Save 45, Sacred beast contract still todo)
- [ ] **Highgold Remnant Digestion** — Mini-progression within Gold
- [ ] **Soulspace Inventory** — Spiritual ender chest at Underlord
- [ ] **Soulfire Resource Bar** — Secondary resource for Lord-realm abilities
- [ ] **Revelation System Rework** — Player types answers, quality affects power
- [ ] **Icon Training** — Earn Sage through gameplay actions instead of button press
- [x] **Herald Remnant Fight** — ~~Boss fight against your own Remnant mirror~~ (Done, uses RemnantEntity)
- [ ] **Monarch Hunger Aura** — Passive environmental effects, attracts Dreadbeasts
- [ ] **Ascension Portal** — Endgame portal to new dimension or server event

---

## Known Issues

- [ ] Dreadbeast Sheep disabled (incomplete textures, crash on load)
- [ ] Sheep hostile check commented out in `isHostile()`

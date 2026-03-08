# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Cradle Mod is a Minecraft Fabric mod (Java 21) inspired by Will Wight's Cradle book series. It adds an RPG progression system with madra cycling, 5 Paths, 13 advancement stages, abilities, duels, and more. Built for Minecraft 1.21.11 with Fabric Loader 0.18.2+ and Fabric API 0.139.4+1.21.11. Uses official Mojang mappings.

## Build Commands

```bash
./gradlew build          # Compile and assemble JAR
./gradlew runClient      # Launch Minecraft client with mod loaded
./gradlew runServer      # Launch dedicated server with mod
./gradlew clean build    # Clean rebuild
```

No test framework is configured beyond Gradle defaults. No linter or CI pipeline.

## Architecture

### Split Source Sets (Fabric Loom)

The build uses `splitEnvironmentSourceSets()` — server and client code live in separate source roots:

- `src/main/java/com/cradle/mod/` — Server-side (and shared) code
- `src/client/java/com/cradle/mod/` — Client-only code (screens, renderers, keybinds)

Client code can import server code but not vice versa. Mixin configs are also split: `cradlemod.mixins.json` (server) and `cradlemod.client.mixins.json` (client).

### Entry Points

- **Server:** `CradleMod.java` implements `ModInitializer` — registers blocks, items, entities, network payloads, commands, event callbacks, and world gen
- **Client:** `CradleModClient.java` implements `ClientModInitializer` — registers keybinds, entity renderers, HUD elements, and client network handlers

### Core Server-Side Managers

| Class | Responsibility |
|-------|---------------|
| `CradlePlayerData` | Central player state (stage, path, madra, iron body, etc.). Stored in NBT, synced to client every tick |
| `CyclingManager` | Server tick loop: madra regen, cycling XP, environmental bonuses, flight physics |
| `BreakthroughManager` | Stage advancement requirements and validation |
| `AbilityExecutor` | Server-side ability execution, madra cost/cooldown validation |
| `AbilityRegistry` / `AbilityDefinitions` | Central registry of all ~25 abilities defined via builder pattern |
| `PlayerLoadout` | 6-slot equipped ability loadout with upgrade levels and branching |
| `DuelManager` | Duel lifecycle, win/loss tracking |
| `RemnantManager` | Remnant entity spawning and absorption mechanics |
| `RevelationTrialManager` | Lord-realm breakthrough trial validation |
| `CrystalSpawnManager` | Event-driven iron body crystal generation |

### Networking

All client-server communication uses Fabric's custom payload system (26+ payload classes in `network/`):

- **C2S payloads:** Player actions (UseAbilityPayload, ChoosePathPayload, ToggleCyclingPayload, etc.)
- **S2C payloads:** State sync (CradleSyncPayload synced every tick, AbilityLoadoutSyncPayload, screen-open payloads)
- **CradleSyncPayload** uses a flags bitmask to pack ~17 boolean states into a single int, with a 12-field StreamCodec composite

When adding new synced state: add a flag bit to `CradleSyncPayload` for booleans, or add a field to the composite codec. Both sides (send in `CradleMod`, receive in `CradleModClient`) must be updated.

### Client-Side

| Class | Purpose |
|-------|---------|
| `ClientCradleData` / `ClientLoadoutData` | Client-side cache of synced player state |
| `CradleInfoScreen` | Status panel (J key) |
| `SkillTreeScreen` | Radial skill tree UI (K key) |
| `PathSelectionScreen` / `WelcomeScreen` | Player onboarding flow |
| `AbilitySlotBarRenderer` | HUD overlay for 6 ability slots with cooldowns |
| `MadraBarMixin` | Replaces vanilla XP bar with madra bar |
| `CyclingParticleRenderer` / `AuraParticleRenderer` | Path-specific particle effects |

### Ability System Pattern

Abilities use a builder pattern (`AbilityDefinition.builder(...)`) defined in `AbilityDefinitions.java`. Each ability has:
- Path, type (ENFORCER/STRIKER/RULER), stage requirement
- Madra cost, cooldown, duration
- Functional handlers (lambda-based: `EnforcerTickHandler`, `StrikerFireHandler`, `RulerAreaHandler`)
- Optional branch ability (unlocked at specific upgrade level)

The flow: client keybind -> C2S payload -> `AbilityExecutor` validates requirements -> executes handler -> state synced back via `CradleSyncPayload`.

### Player Data Persistence

`CradlePlayerData` uses NBT serialization (`readNbt`/`writeNbt`). Data is stored in a separate file (`cradlemod_playerdata.dat`) in the world save directory, loaded on server start and saved periodically. The `CradleMod` class manages the `HashMap<UUID, CradlePlayerData>` map.

### Key Enums (all in CradlePlayerData)

- `Path`: BLACK_FLAME, ENDLESS_SWORD, STELLAR_SPEAR, CLOUD_HAMMER, HOLLOW_KING
- `AdvancementStage`: FOUNDATION through MONARCH (13 stages)
- `IronBody`: NONE, BLOODFORGED, STEELBORN, RAINDROP

### Mixin Strategy

Mixins are used sparingly — only for things that require hooking vanilla internals:
- `CradleMixin` — hooks server lifecycle for initialization
- `MadraBarMixin` — replaces XP bar rendering
- `SpiritShiftMixin` — Herald visual transformation
- `GlowColorMixin` — Custom glow colors
- `Dreadbeast*Mixin` — Hostile mob retexturing/behavior

### Commands

`/cycle` — Debug command for setting player state (stage, path, madra, level, iron body, etc.)
`/duel` — Duel management (invite, accept, decline, stats)

## Key Conventions

- All network payloads are record classes implementing `CustomPacketPayload`
- Payload IDs follow pattern: `CustomPacketPayload.Type<T>` with `Identifier.fromNamespaceAndPath("cradlemod", "...")`
- Server-side validation is required for all player actions — never trust client payloads
- Action bar messages (`player.displayClientMessage(Component.literal(...), true)`) are preferred over chat messages for notifications
- Particle spawning is client-side for performance; server only syncs state
- The mod uses Fabric API events (not Forge) — `ServerTickEvents`, `ServerEntityCombatEvents`, `UseBlockCallback`, etc.

## Documentation

- `docs/DESIGN.md` — Visual and mechanical design philosophy
- `docs/LORE.md` — Cradle universe lore reference
- `docs/ROADMAP.md` — Everything left to build (single source of truth)
- `docs/PROGRESS.md` — Completed features tracker
- `docs/plans/` — Design documents from brainstorming sessions

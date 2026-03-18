# Cradle World & Story Mode — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a Story Mode with Sacred Valley world generation, NPCs, dialogue, companion, cutscenes, and a faithful book-1 story adaptation alongside the existing Free Mode.

**Architecture:** Story Mode layers on top of existing systems. A `GameModeManager` tracks whether a world is Free or Cradle mode. A custom `ChunkGenerator` replaces overworld terrain. `StoryNpcEntity` provides the NPC framework. `StoryManager` tracks quest state. `CompanionEntity` extends NPC with follow/combat AI. `CutsceneManager` handles scripted sequences. All story content is data-driven via JSON.

**Tech Stack:** Fabric 1.21.11, Java 21, Mojang mappings. Custom ChunkGenerator, entity types, network payloads, screens, HUD elements. JSON data files for dialogue/quests.

**Note:** This project has no test framework. Verification is done via `./gradlew build` (compile check) and `./gradlew runClient` (manual testing).

---

## Phase 1: Game Mode Foundation ✅

### Task 1: GameModeManager — Core Mode Tracking ✅

**Files:**
- Create: `src/main/java/com/cradle/mod/story/GameModeManager.java`
- Modify: `src/main/java/com/cradle/mod/CradleMod.java` — load/save mode flag
- Modify: `src/main/java/com/cradle/mod/CradlePlayerData.java` — add character choice field

**Step 1: Create GameModeManager**

```java
package com.cradle.mod.story;

import net.minecraft.nbt.CompoundTag;

public class GameModeManager {
    public enum CradleGameMode { FREE, CRADLE }
    public enum PlayerCharacter { NONE, LINDON, YERIN }

    private static CradleGameMode currentMode = CradleGameMode.FREE;

    public static CradleGameMode getMode() { return currentMode; }
    public static boolean isCradleMode() { return currentMode == CradleGameMode.CRADLE; }
    public static boolean isFreeMode() { return currentMode == CradleGameMode.FREE; }
    public static void setMode(CradleGameMode mode) { currentMode = mode; }

    public static CompoundTag writeNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("gameMode", currentMode.name());
        return tag;
    }

    public static void readNbt(CompoundTag tag) {
        if (tag.contains("gameMode")) {
            try {
                currentMode = CradleGameMode.valueOf(tag.getString("gameMode"));
            } catch (IllegalArgumentException e) {
                currentMode = CradleGameMode.FREE;
            }
        }
    }
}
```

**Step 2: Add character field to CradlePlayerData**

Add a `PlayerCharacter chosenCharacter` field alongside the existing path/stage fields. Add getter/setter, include in `readNbt`/`writeNbt`. Default to `NONE`.

**Step 3: Hook into CradleMod save/load**

In `CradleMod.java`, add `GameModeManager.writeNbt()` / `readNbt()` alongside the existing playerdata persistence in the `cradlemod_playerdata.dat` file.

**Step 4: Verify**

Run: `./gradlew build`
Expected: Compiles without errors.

**Step 5: Commit**

```bash
git add src/main/java/com/cradle/mod/story/GameModeManager.java
git add src/main/java/com/cradle/mod/CradleMod.java
git add src/main/java/com/cradle/mod/CradlePlayerData.java
git commit -m "feat: add GameModeManager and character choice field"
```

---

### Task 2: Character Selection Screen (Client) ✅

**Files:**
- Create: `src/client/java/com/cradle/mod/screen/CharacterSelectionScreen.java`
- Create: `src/main/java/com/cradle/mod/network/ChooseCharacterPayload.java`
- Create: `src/main/java/com/cradle/mod/network/OpenCharacterSelectionPayload.java`
- Modify: `src/main/java/com/cradle/mod/CradleMod.java` — register payloads, handle character choice
- Modify: `src/client/java/com/cradle/mod/CradleModClient.java` — register client handler

**Step 1: Create OpenCharacterSelectionPayload (S2C)**

Follow existing `OpenInfoScreenPayload` pattern — stateless record with `StreamCodec.unit()`.

```java
public record OpenCharacterSelectionPayload() implements CustomPacketPayload {
    public static final Type<OpenCharacterSelectionPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "open_character_selection"));
    public static final StreamCodec<ByteBuf, OpenCharacterSelectionPayload> STREAM_CODEC =
        StreamCodec.unit(new OpenCharacterSelectionPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

**Step 2: Create ChooseCharacterPayload (C2S)**

Follow existing `ChoosePathPayload` pattern — record with a string field for character name.

```java
public record ChooseCharacterPayload(String characterName) implements CustomPacketPayload {
    public static final Type<ChooseCharacterPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "choose_character"));
    public static final StreamCodec<ByteBuf, ChooseCharacterPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ChooseCharacterPayload::characterName,
            ChooseCharacterPayload::new
        );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

**Step 3: Create CharacterSelectionScreen**

Follow existing `PathSelectionScreen` pattern. Two large buttons: "Wei Shi Lindon" and "Yerin". Each shows a brief description of the character's story and starting state. On click, send `ChooseCharacterPayload`.

**Step 4: Register payloads in CradleMod.onInitialize()**

Register `OpenCharacterSelectionPayload` as S2C, `ChooseCharacterPayload` as C2S. Add server handler for `ChooseCharacterPayload` that:
- Sets `CradlePlayerData.chosenCharacter`
- Sets path based on character (Lindon → PURE at FOUNDATION, Yerin → ENDLESS_SWORD at COPPER)
- Sets `hasChosenPath = true` (bypasses PathSelectionScreen)
- Syncs data

**Step 5: Register client handler in CradleModClient.onInitializeClient()**

Handle `OpenCharacterSelectionPayload` — open `CharacterSelectionScreen`.

**Step 6: Modify player join logic**

In `CradleMod.java` `ServerPlayConnectionEvents.JOIN` handler:
- If Cradle mode AND player hasn't chosen character → send `OpenCharacterSelectionPayload`
- If Free mode → send `OpenPathSelectionPayload` (existing behavior)

**Step 7: Verify**

Run: `./gradlew build`
Expected: Compiles. Manual test: create world, verify character selection screen appears.

**Step 8: Commit**

```bash
git add src/main/java/com/cradle/mod/network/ChooseCharacterPayload.java
git add src/main/java/com/cradle/mod/network/OpenCharacterSelectionPayload.java
git add src/client/java/com/cradle/mod/screen/CharacterSelectionScreen.java
git add src/main/java/com/cradle/mod/CradleMod.java
git add src/client/java/com/cradle/mod/CradleModClient.java
git commit -m "feat: character selection screen for Cradle Mode (Lindon/Yerin)"
```

---

### Task 3: World Type Registration — Cradle Mode World ✅

**Files:**
- Create: `src/main/java/com/cradle/mod/worldgen/SacredValleyChunkGenerator.java`
- Modify: `src/main/java/com/cradle/mod/CradleMod.java` — register chunk generator

**Step 1: Create minimal SacredValleyChunkGenerator**

Extend Minecraft's `ChunkGenerator`. For now, generate a flat world at Y=64 with grass blocks — just enough to prove the custom generator works. The actual terrain shaping comes in Phase 2.

```java
package com.cradle.mod.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.chunk.ChunkGenerator;
// ... other imports

public class SacredValleyChunkGenerator extends ChunkGenerator {
    public static final MapCodec<SacredValleyChunkGenerator> CODEC =
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            // biome source codec
        ).apply(instance, SacredValleyChunkGenerator::new));

    // Constructor, buildSurface, fillFromNoise, getBaseHeight, etc.
    // Start with flat grass at Y=64, stone below, air above
}
```

**Step 2: Register the chunk generator**

Register `SacredValleyChunkGenerator.CODEC` with `Registry.register()` against `BuiltInRegistries.CHUNK_GENERATOR` using identifier `cradlemod:sacred_valley`.

**Step 3: Create a world preset JSON or programmatic preset**

Create a world preset that uses `SacredValleyChunkGenerator` for the overworld dimension. This is what the world creation screen will use when "Cradle Mode" is selected. Store the mode choice (FREE/CRADLE) in the world's level data.

**Step 4: Verify**

Run: `./gradlew build`
Expected: Compiles. Manual test: create a world with the custom world type, verify flat terrain generates.

**Step 5: Commit**

```bash
git add src/main/java/com/cradle/mod/worldgen/SacredValleyChunkGenerator.java
git add src/main/java/com/cradle/mod/CradleMod.java
git commit -m "feat: register SacredValleyChunkGenerator (flat placeholder)"
```

---

## Phase 2: Sacred Valley Terrain ✅

### Task 4: Valley Heightmap — Bowl Shape with Mountain Ring ✅

**Files:**
- Modify: `src/main/java/com/cradle/mod/worldgen/SacredValleyChunkGenerator.java`
- Create: `src/main/java/com/cradle/mod/worldgen/ValleyHeightmap.java`

**Step 1: Create ValleyHeightmap utility**

A pure function that takes `(blockX, blockZ)` and returns a height value. The valley is centered at `(0, 0)` with radius ~750 blocks.

```java
public class ValleyHeightmap {
    public static final int VALLEY_CENTER_X = 0;
    public static final int VALLEY_CENTER_Z = 0;
    public static final int VALLEY_RADIUS = 750;
    public static final int MOUNTAIN_RING_WIDTH = 200;
    public static final int VALLEY_FLOOR_Y = 68;
    public static final int MOUNTAIN_PEAK_Y = 180;
    public static final int MOUNT_SAMARA_RADIUS = 80;
    public static final int MOUNT_SAMARA_PEAK_Y = 180;

    public static int getHeight(int blockX, int blockZ) {
        double distFromCenter = Math.sqrt(blockX * blockX + blockZ * blockZ);

        // Mount Samara at center
        if (distFromCenter < MOUNT_SAMARA_RADIUS) {
            double samaraFactor = 1.0 - (distFromCenter / MOUNT_SAMARA_RADIUS);
            return VALLEY_FLOOR_Y + (int)(samaraFactor * (MOUNT_SAMARA_PEAK_Y - VALLEY_FLOOR_Y));
        }

        // Valley floor with gentle rolling hills
        if (distFromCenter < VALLEY_RADIUS - MOUNTAIN_RING_WIDTH) {
            // Use simplex noise for gentle hills, base at VALLEY_FLOOR_Y
            return VALLEY_FLOOR_Y + gentleHills(blockX, blockZ);
        }

        // Mountain ring transition
        double ringProgress = (distFromCenter - (VALLEY_RADIUS - MOUNTAIN_RING_WIDTH)) / MOUNTAIN_RING_WIDTH;
        ringProgress = Math.min(1.0, ringProgress);
        return VALLEY_FLOOR_Y + (int)(ringProgress * (MOUNTAIN_PEAK_Y - VALLEY_FLOOR_Y));
    }

    private static int gentleHills(int x, int z) {
        // Perlin/simplex noise for 0-12 block variation
        // Use seed-based noise for determinism
    }
}
```

**Step 2: Wire heightmap into SacredValleyChunkGenerator**

Replace the flat generation with `ValleyHeightmap.getHeight()`. Place stone below the surface, grass/dirt on top, air above. Bedrock at Y=-64.

**Step 3: Add barrier blocks at mountain peaks**

At the very top of the mountain ring (the perimeter), place barrier blocks to prevent climbing over. Only at the very edge — the mountains themselves are natural stone.

**Step 4: Verify**

Run: `./gradlew runClient`
Expected: Spawn in a valley with rolling hills, Mount Samara visible in the center, mountains around the rim.

**Step 5: Commit**

```bash
git add src/main/java/com/cradle/mod/worldgen/ValleyHeightmap.java
git add src/main/java/com/cradle/mod/worldgen/SacredValleyChunkGenerator.java
git commit -m "feat: Sacred Valley terrain — bowl shape, Mount Samara, mountain ring"
```

---

### Task 5: Biome Painting & Surface Decoration ✅

**Files:**
- Create: `src/main/java/com/cradle/mod/worldgen/ValleyBiomePainter.java`
- Modify: `src/main/java/com/cradle/mod/worldgen/SacredValleyChunkGenerator.java`

**Step 1: Create ValleyBiomePainter**

Assign biome types based on position within the valley:
- **Valley floor** → Plains biome (grass, flowers)
- **Forested areas** (specific zones) → Forest biome (oak/birch trees)
- **River corridors** → River biome (water channels)
- **Mountain slopes** → Mountain biome (stone, gravel, snow at peaks)
- **Mount Samara** → Old Growth Forest biome (large trees, the Samara ring)

**Step 2: Add surface decoration**

- Trees in forested zones (standard Minecraft tree placement)
- Grass and flowers on valley floor
- Water in river channels (carve rivers at Y=63 through specific paths)
- Vital fruit bushes in forested areas (reuse existing CradleWorldGen feature)
- Snow on mountain peaks above Y=150

**Step 3: Verify**

Run: `./gradlew runClient`
Expected: Valley has varied terrain — forests, rivers, grassy areas, snowy mountain peaks.

**Step 4: Commit**

```bash
git add src/main/java/com/cradle/mod/worldgen/ValleyBiomePainter.java
git add src/main/java/com/cradle/mod/worldgen/SacredValleyChunkGenerator.java
git commit -m "feat: biome painting and surface decoration for Sacred Valley"
```

---

### Task 6: Spawn Point & Day/Night Cycle ✅

**Files:**
- Modify: `src/main/java/com/cradle/mod/worldgen/SacredValleyChunkGenerator.java`
- Modify: `src/main/java/com/cradle/mod/CradleMod.java`

**Step 1: Set spawn point**

In Cradle mode, override the spawn point to the Wei clan territory (northwest quadrant of the valley, approximately `-300, 68, -300`). Use `ServerPlayConnectionEvents.JOIN` to teleport new Cradle mode players to the Wei clan area.

**Step 2: Lock weather (optional)**

Sacred Valley has a mild climate. Consider locking weather to clear in Cradle mode (no rain/thunder) for atmosphere. Can use `ServerTickEvents` to clear weather each tick, or a game rule.

**Step 3: Verify**

Run: `./gradlew runClient`
Expected: Spawn in the Wei clan area of the valley, not at world origin.

**Step 4: Commit**

```bash
git add src/main/java/com/cradle/mod/worldgen/SacredValleyChunkGenerator.java
git add src/main/java/com/cradle/mod/CradleMod.java
git commit -m "feat: set Sacred Valley spawn point at Wei clan territory"
```

---

## Phase 3: Sacred Valley Structures ⏸️ (structures disabled — user building manually in-game)

### Task 7: Structure Generator Framework ✅ *(code exists but disabled — user will build structures in-game and import later)*

**Files:**
- Create: `src/main/java/com/cradle/mod/worldgen/structure/StructureGenerator.java`
- Create: `src/main/java/com/cradle/mod/worldgen/structure/BuildingPlacer.java`

**Step 1: Create StructureGenerator base**

A utility class that places blocks in the world at specific coordinates. Provides helper methods for common building patterns:

```java
public class BuildingPlacer {
    // Place a rectangular room
    public static void placeRoom(ServerLevel level, BlockPos origin,
                                  int width, int depth, int height,
                                  Block wallBlock, Block floorBlock, Block roofBlock);

    // Place a path/road between two points
    public static void placePath(ServerLevel level, BlockPos from, BlockPos to,
                                  int width, Block pathBlock);

    // Place a fenced area
    public static void placeFence(ServerLevel level, BlockPos origin,
                                   int width, int depth, Block fenceBlock);

    // Place a tower/column
    public static void placeTower(ServerLevel level, BlockPos base,
                                   int radius, int height, Block block);
}
```

**Step 2: Create StructureGenerator**

Manages when and how structures get placed. Structures are generated on first world load (not during chunk gen) to avoid chunk-loading complexity. Uses a flag in the world data to track whether structures have been placed.

```java
public class StructureGenerator {
    private static boolean structuresPlaced = false;

    public static void generateIfNeeded(ServerLevel level) {
        if (!GameModeManager.isCradleMode() || structuresPlaced) return;
        // Place all structures
        WeiClanStructure.generate(level);
        HeavensGloryStructure.generate(level);
        MountSamaraStructure.generate(level);
        // ... exterior-only structures
        structuresPlaced = true;
    }
}
```

**Step 3: Verify**

Run: `./gradlew build`
Expected: Compiles.

**Step 4: Commit**

```bash
git add src/main/java/com/cradle/mod/worldgen/structure/
git commit -m "feat: structure generator framework with BuildingPlacer utilities"
```

---

### Task 8: Wei Clan Village (Fully Detailed) ✅ *(code exists but disabled — user will rebuild in-game)*

**Files:**
- Create: `src/main/java/com/cradle/mod/worldgen/structure/WeiClanStructure.java`

**Step 1: Design the Wei clan layout**

The Wei clan area at approximately `(-300, 68, -300)` consists of:
- **Main hall** — largest building, where Elder Whisper resides. 15x20 block wooden structure with a raised platform.
- **Training grounds** — open area with fence posts, targets (hay bales), flat stone floor. 20x20 blocks.
- **Transcription hall** — medium building (10x12) with bookshelves and lecterns.
- **Residential houses** — 6-8 small houses (7x7 each) scattered around the area. Oak planks, cobblestone foundation.
- **Village paths** — gravel/path blocks connecting all buildings.
- **Wei family home** — slightly larger house (10x10) for Lindon's family. Identifiable by banners or decorations.

**Step 2: Implement WeiClanStructure.generate()**

Use `BuildingPlacer` to construct each building. Place furniture (crafting tables, furnaces, beds, bookshelves, item frames). Add torches/lanterns for lighting.

**Step 3: Verify**

Run: `./gradlew runClient`
Expected: Wei clan village visible at spawn. Buildings have interiors with furniture.

**Step 4: Commit**

```bash
git add src/main/java/com/cradle/mod/worldgen/structure/WeiClanStructure.java
git commit -m "feat: Wei clan village — main hall, training grounds, houses"
```

---

### Task 9: Heaven's Glory School (Fully Detailed) ✅ *(code exists but disabled — user will rebuild in-game)*

**Files:**
- Create: `src/main/java/com/cradle/mod/worldgen/structure/HeavensGloryStructure.java`

**Step 1: Design Heaven's Glory layout**

Located in the southwest, built into a mountainside at approximately `(-400, 80, 300)`:
- **Main temple** — large stone brick structure (25x30) with pillars, high ceiling, grand entrance stairs. Built against the mountain slope.
- **Treasure vault** — underground room accessed from the temple. Iron doors, chests, display cases (armor stands with items).
- **Trial of Glorious Ascension** — parkour course built into the mountain. Jumping platforms, ladders, narrow ledges rising upward. Timed challenge.
- **Elder quarters** — stone rooms at the back of the temple.
- **Training courtyard** — stone-floored open area in front of the temple.
- **Entrance gate** — decorated archway with stairs leading up from the valley floor.

**Step 2: Implement HeavensGloryStructure.generate()**

Use stone bricks, chiseled stone, polished andesite for a temple aesthetic. Place redstone torches and soul lanterns for moody lighting. The parkour course uses stone slabs, walls, and chains for jumping platforms.

**Step 3: Verify**

Run: `./gradlew runClient`
Expected: Temple visible in the southwest, built into a hillside. Parkour course accessible.

**Step 4: Commit**

```bash
git add src/main/java/com/cradle/mod/worldgen/structure/HeavensGloryStructure.java
git commit -m "feat: Heaven's Glory school — temple, vault, parkour trial"
```

---

### Task 10: Mount Samara & Key Landmarks ✅ *(code exists but disabled — user will rebuild in-game)*

**Files:**
- Create: `src/main/java/com/cradle/mod/worldgen/structure/MountSamaraStructure.java`
- Create: `src/main/java/com/cradle/mod/worldgen/structure/LandmarkStructures.java`
- Create: `src/main/java/com/cradle/mod/worldgen/structure/HolyWindStructure.java`
- Create: `src/main/java/com/cradle/mod/worldgen/structure/FallenLeafStructure.java`
- Create: `src/main/java/com/cradle/mod/worldgen/structure/GoldenSwordStructure.java`
- Create: `src/main/java/com/cradle/mod/worldgen/structure/KazanClanStructure.java`
- Create: `src/main/java/com/cradle/mod/worldgen/structure/LiClanStructure.java`

**Step 1: Mount Samara details**

The mountain is already generated by the heightmap (Task 4). Add:
- **Samara ring** — A golden ring made of glowstone and light source blocks (sea lanterns, shroomlights) that enters the mountain and emerges back out, forming a circle around the top of the mountain. Partially embedded in the terrain so it looks like it passes through the rock. The ring only appears at night and disappears at dawn — use server tick logic to place/remove the blocks based on day/night cycle. When visible, it glows intensely against the dark sky.
- **Ancestor's Tomb** — Small stone structure at the summit (Y~180). Stone brick room with chests, a central altar (enchanting table), and Adama's sword item inside.
- **Path to summit** — Winding gravel path spiraling up the mountain.

**Step 2: Yerin's cave**

A cave carved into a hillside between the Wei clan and Mount Samara. ~10 blocks deep, 5 wide, 4 tall. Partially hidden by leaves/vines. Contains a campfire, crafting table, and a bed.

**Step 3: Fully detailed faction structures**

All 5 remaining factions have full interiors and unique layouts. Each has a main hall, training area, and residential buildings with furniture/lighting:

- **Holy Wind school (north, ~(0, 68, -500))** — white concrete/quartz. Known for movement techniques. Open-air training platforms at varying heights (parkour-style). Main dojo with polished quartz floors, paper lantern lighting. Wind chime decorations (note blocks). Meditation garden with flowers and water features.

- **Fallen Leaf school (east, ~(400, 68, 0))** — dark oak/spruce. Known for stealth and subtlety. Low-profile buildings hidden among dense trees. Underground training hall accessed via trapdoor. Library with bookshelves and map wall (item frames). Dormitories with beds tucked into tree hollows.

- **Golden Sword school (south, ~(0, 68, 400))** — gold/yellow terracotta. Known for aggressive combat. Large arena with tiered seating (stairs). Weapon racks (armor stands with swords). Grand entrance with gold block accents. Forge room with furnaces, anvils, and lava channels.

- **Kazan clan (northeast, ~(350, 68, -350))** — deepslate/blackstone. Known for fire and destruction arts. Dark, fortress-like compound with thick walls. Training pit (sunken arena in the center). Lava-lit corridors. War room with large table (dark oak) and banner decorations. Blacksmith quarter with blast furnaces.

- **Li clan (southeast, ~(350, 68, 350))** — stripped birch/sandstone. Known for scholarly pursuits. Elegant buildings with arched doorways. Large library spanning two floors with enchanting tables and lecterns. Courtyard with a central fountain. Scholar quarters with desks (crafting tables) and bookshelves. Observatory tower (highest point in the compound).

**Step 4: Verify**

Run: `./gradlew runClient`
Expected: All 7 faction areas visible. Mount Samara has glowing ring around the top (at night) and tomb at peak. Cave exists between Wei clan and Samara.

**Step 5: Commit**

```bash
git add src/main/java/com/cradle/mod/worldgen/structure/MountSamaraStructure.java
git add src/main/java/com/cradle/mod/worldgen/structure/LandmarkStructures.java
git add src/main/java/com/cradle/mod/worldgen/structure/HolyWindStructure.java
git add src/main/java/com/cradle/mod/worldgen/structure/FallenLeafStructure.java
git add src/main/java/com/cradle/mod/worldgen/structure/GoldenSwordStructure.java
git add src/main/java/com/cradle/mod/worldgen/structure/KazanClanStructure.java
git add src/main/java/com/cradle/mod/worldgen/structure/LiClanStructure.java
git commit -m "feat: Mount Samara, Yerin's cave, all faction structures with full interiors"
```

---

## Phase 4: NPC Framework ✅

### Task 11: StoryNpcEntity — Base Class ✅

**Files:**
- Create: `src/main/java/com/cradle/mod/entity/StoryNpcEntity.java`
- Modify: `src/main/java/com/cradle/mod/entity/CradleEntities.java` — register entity type
- Modify: `src/main/java/com/cradle/mod/CradleMod.java` — register attributes

**Step 1: Create StoryNpcEntity**

Extends `PathfinderMob` (peaceful mob with pathfinding). Key features:
- Synched data: `NPC_ID` (string), `NPC_NAME` (string), `NPC_PATH` (string), `NPC_STAGE` (string), `FACTION` (string)
- Invulnerable — override `hurt()` to return false, override `isInvulnerable()` to return true
- No despawn — override `removeWhenFarAway()` to return false
- Right-click interaction — override `mobInteract()` to trigger dialogue
- No knockback — override `knockback()` to do nothing
- Persist — override `shouldBeSaved()` to return true

```java
public class StoryNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> NPC_ID =
        SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> NPC_DISPLAY_NAME =
        SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.STRING);
    // ... more synced data

    public StoryNpcEntity(EntityType<? extends StoryNpcEntity> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            // Send dialogue open payload to client
            if (player instanceof ServerPlayer serverPlayer) {
                ServerPlayNetworking.send(serverPlayer,
                    new OpenDialoguePayload(this.getNpcId(), this.getId()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public void knockback(double strength, double x, double z) { /* no-op */ }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }
}
```

**Step 2: Register in CradleEntities**

Follow `REMNANT` registration pattern. Use `MobCategory.CREATURE`, sized `0.6f, 1.8f` (player-sized), `clientTrackingRange(8)`.

**Step 3: Register attributes in CradleMod.onInitialize()**

```java
FabricDefaultAttributeRegistry.register(CradleEntities.STORY_NPC, StoryNpcEntity.createMobAttributes());
```

**Step 4: Verify**

Run: `./gradlew build`
Expected: Compiles. Entity type registered.

**Step 5: Commit**

```bash
git add src/main/java/com/cradle/mod/entity/StoryNpcEntity.java
git add src/main/java/com/cradle/mod/entity/CradleEntities.java
git add src/main/java/com/cradle/mod/CradleMod.java
git commit -m "feat: StoryNpcEntity — invulnerable, persistent, right-click dialogue"
```

---

### Task 12: NPC Renderer (Client) ✅

**Files:**
- Create: `src/client/java/com/cradle/mod/entity/StoryNpcRenderer.java`
- Create: `src/client/java/com/cradle/mod/entity/StoryNpcRenderState.java`
- Modify: `src/client/java/com/cradle/mod/CradleModClient.java` — register renderer

**Step 1: Create StoryNpcRenderer**

Use the humanoid player model (`PlayerModel`). Render NPCs as player-shaped with a nametag above their head showing `NPC_DISPLAY_NAME`. The renderer reads the NPC's path from synced data to apply path-colored effects (reuse existing color mapping from `ClientCradleData`).

**Step 2: Create StoryNpcRenderState**

Follow `RemnantRenderState` pattern. Hold the NPC's display name, path, stage for rendering.

**Step 3: Register renderer**

In `CradleModClient.onInitializeClient()`:
```java
EntityRendererRegistry.register(CradleEntities.STORY_NPC, StoryNpcRenderer::new);
```

**Step 4: Verify**

Run: `./gradlew runClient`
Expected: NPC entity renders as a humanoid with a nametag. (Use `/summon cradlemod:story_npc` to test.)

**Step 5: Commit**

```bash
git add src/client/java/com/cradle/mod/entity/StoryNpcRenderer.java
git add src/client/java/com/cradle/mod/entity/StoryNpcRenderState.java
git add src/client/java/com/cradle/mod/CradleModClient.java
git commit -m "feat: StoryNpcRenderer — humanoid model with nametag"
```

---

### Task 13: NPC Spawning & Waypoint AI ✅

**Files:**
- Create: `src/main/java/com/cradle/mod/story/NpcSpawnManager.java`
- Create: `src/main/java/com/cradle/mod/entity/ai/WaypointWanderGoal.java`
- Modify: `src/main/java/com/cradle/mod/entity/StoryNpcEntity.java` — add AI goals

**Step 1: Create WaypointWanderGoal**

An AI goal that moves the NPC between a list of waypoints. Each waypoint has a position and a dwell time (how long to stand there). The NPC walks to the next waypoint, stands for the dwell time, then moves on.

```java
public class WaypointWanderGoal extends Goal {
    private final PathfinderMob mob;
    private final List<WaypointEntry> waypoints;
    private int currentIndex = 0;
    private int dwellTicksRemaining = 0;

    public record WaypointEntry(BlockPos pos, int dwellTicks) {}

    // In tick: if at waypoint, dwell. If dwell done, move to next.
    // If not at waypoint, pathfind toward it.
}
```

**Step 2: Create NpcSpawnManager**

Spawns all story NPCs at their starting positions when a Cradle mode world first loads. Tracks which NPCs have been spawned via world data. Each NPC definition includes:
- NPC ID (string key)
- Display name
- Spawn position
- Path and stage
- Faction
- Waypoint list

```java
public class NpcSpawnManager {
    public static void spawnIfNeeded(ServerLevel level) {
        if (!GameModeManager.isCradleMode()) return;
        // Check world data flag
        // Spawn each NPC at their defined position
        // Store entity UUIDs for later reference
    }
}
```

**Step 3: Wire up**

Add `WaypointWanderGoal` to `StoryNpcEntity.registerGoals()`. Add `LookAtPlayerGoal` so NPCs face the player when nearby. Call `NpcSpawnManager.spawnIfNeeded()` on world load in `CradleMod`.

**Step 4: Verify**

Run: `./gradlew runClient`
Expected: NPCs spawn at their positions and wander between waypoints. They face the player when approached.

**Step 5: Commit**

```bash
git add src/main/java/com/cradle/mod/story/NpcSpawnManager.java
git add src/main/java/com/cradle/mod/entity/ai/WaypointWanderGoal.java
git add src/main/java/com/cradle/mod/entity/StoryNpcEntity.java
git commit -m "feat: NPC spawning and waypoint wandering AI"
```

---

### Task 14: Dialogue System — Data & Server ✅

**Files:**
- Create: `src/main/java/com/cradle/mod/story/dialogue/DialogueTree.java`
- Create: `src/main/java/com/cradle/mod/story/dialogue/DialogueNode.java`
- Create: `src/main/java/com/cradle/mod/story/dialogue/DialogueLoader.java`
- Create: `src/main/java/com/cradle/mod/network/OpenDialoguePayload.java`
- Create: `src/main/java/com/cradle/mod/network/DialogueResponsePayload.java`
- Create: `src/main/java/com/cradle/mod/network/DialogueNodePayload.java`
- Create: `data/cradlemod/dialogue/elder_whisper.json` (example)

**Step 1: Define dialogue data model**

```java
public record DialogueNode(
    String id,               // Unique node ID within the tree
    String speakerName,       // Who's talking
    String text,              // What they say
    List<DialogueOption> options  // Player response choices
) {}

public record DialogueOption(
    String label,             // Button text
    String nextNodeId,        // Which node to go to (null = end conversation)
    String requiredFlag,      // Story flag that must be set (null = always available)
    String setFlag,           // Story flag to set when chosen (null = none)
    String giveItem,          // Item to give player (null = none)
    String advanceObjective   // Objective ID to complete (null = none)
) {}

public record DialogueTree(
    String npcId,             // Which NPC this belongs to
    String startNodeId,       // Entry point
    Map<String, DialogueNode> nodes  // All nodes by ID
) {}
```

**Step 2: Create DialogueLoader**

Loads JSON files from `data/cradlemod/dialogue/`. Each file is one NPC's dialogue tree. Parse using Gson.

Example JSON (`elder_whisper.json`):
```json
{
    "npcId": "elder_whisper",
    "startNodeId": "greeting",
    "nodes": {
        "greeting": {
            "speakerName": "Elder Whisper",
            "text": "Young one. I sense you have questions about the sacred arts.",
            "options": [
                { "label": "Tell me about cycling.", "nextNodeId": "about_cycling" },
                { "label": "What is this valley?", "nextNodeId": "about_valley" },
                { "label": "Goodbye.", "nextNodeId": null }
            ]
        },
        "about_cycling": {
            "speakerName": "Elder Whisper",
            "text": "Cycling is the foundation of all sacred arts. Through cycling, you draw vital aura into your body and refine it into madra.",
            "options": [
                { "label": "How do I cycle?", "nextNodeId": "how_to_cycle" },
                { "label": "Thank you.", "nextNodeId": null }
            ]
        }
    }
}
```

**Step 3: Create network payloads**

- `OpenDialoguePayload` (S2C) — sends NPC ID and entity ID to client to open dialogue
- `DialogueNodePayload` (S2C) — sends the current node's data (speaker, text, options) to client
- `DialogueResponsePayload` (C2S) — player clicked option index, server processes effects and sends next node

**Step 4: Register payloads and handlers**

Server handler for `DialogueResponsePayload`:
- Look up current dialogue tree for the NPC
- Process the chosen option's effects (set flags, give items, advance objectives)
- Send the next `DialogueNodePayload` (or close if nextNodeId is null)

**Step 5: Verify**

Run: `./gradlew build`
Expected: Compiles. Dialogue data loads from JSON.

**Step 6: Commit**

```bash
git add src/main/java/com/cradle/mod/story/dialogue/
git add src/main/java/com/cradle/mod/network/OpenDialoguePayload.java
git add src/main/java/com/cradle/mod/network/DialogueResponsePayload.java
git add src/main/java/com/cradle/mod/network/DialogueNodePayload.java
git add src/main/resources/data/cradlemod/dialogue/elder_whisper.json
git commit -m "feat: dialogue system — JSON data model, loader, network payloads"
```

---

### Task 15: DialogueScreen (Client) ✅

**Files:**
- Create: `src/client/java/com/cradle/mod/screen/DialogueScreen.java`
- Modify: `src/client/java/com/cradle/mod/CradleModClient.java` — register payload handlers

**Step 1: Create DialogueScreen**

Follow existing screen patterns. Layout:
- **Top section** — NPC name in gold text, centered
- **Middle section** — Dialogue text, left-aligned, word-wrapped. Dark semi-transparent panel behind it.
- **Bottom section** — Response buttons, stacked vertically. Each is a clickable text row that highlights on hover.
- Non-pausing (`isPauseScreen() = false`)

```java
public class DialogueScreen extends Screen {
    private String speakerName;
    private String dialogueText;
    private List<String> optionLabels;
    private int npcEntityId;
    private int hoveredOption = -1;

    public DialogueScreen(String speakerName, String text, List<String> options, int npcEntityId) {
        super(Component.literal("Dialogue"));
        this.speakerName = speakerName;
        this.dialogueText = text;
        this.optionLabels = options;
        this.npcEntityId = npcEntityId;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Semi-transparent background (lower third of screen only)
        int panelTop = this.height - 140;
        graphics.fill(20, panelTop, this.width - 20, this.height - 10, 0xCC1A1A2E);

        // Speaker name
        graphics.drawString(this.font, speakerName, 30, panelTop + 8, 0xFFFFD700);

        // Dialogue text (word-wrapped)
        int textY = panelTop + 24;
        // ... word wrap and render dialogueText

        // Options
        int optionY = panelTop + 80;
        for (int i = 0; i < optionLabels.size(); i++) {
            int color = (i == hoveredOption) ? 0xFFFFAA00 : 0xFFCCCCCC;
            graphics.drawString(this.font, "> " + optionLabels.get(i), 40, optionY, color);
            optionY += 14;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredOption >= 0) {
            ClientPlayNetworking.send(new DialogueResponsePayload(npcEntityId, hoveredOption));
            return true;
        }
        return false;
    }
}
```

**Step 2: Register client payload handlers**

Handle `OpenDialoguePayload` — request initial dialogue node from server.
Handle `DialogueNodePayload` — open/update `DialogueScreen` with new node data. If `nextNodeId` is null, close the screen.

**Step 3: Verify**

Run: `./gradlew runClient`
Expected: Right-click an NPC → dialogue screen appears with text and clickable options.

**Step 4: Commit**

```bash
git add src/client/java/com/cradle/mod/screen/DialogueScreen.java
git add src/client/java/com/cradle/mod/CradleModClient.java
git commit -m "feat: DialogueScreen — NPC dialogue UI with response options"
```

---

## Phase 5: Quest & Story System

### Task 16: StoryManager — Server-Side Quest Tracking

**Files:**
- Create: `src/main/java/com/cradle/mod/story/StoryManager.java`
- Create: `src/main/java/com/cradle/mod/story/StoryChapter.java`
- Create: `src/main/java/com/cradle/mod/story/StoryObjective.java`
- Create: `src/main/java/com/cradle/mod/story/StoryDataLoader.java`
- Create: `data/cradlemod/story/lindon_chapter1.json` (example)
- Modify: `src/main/java/com/cradle/mod/CradlePlayerData.java` — add story state fields

**Step 1: Define story data model**

```java
public record StoryChapter(
    String id,
    String title,
    String description,
    String character,           // "LINDON", "YERIN", or "BOTH"
    List<StoryObjective> objectives,
    String nextChapterId
) {}

public record StoryObjective(
    String id,
    String description,         // Shown in HUD and journal
    String triggerType,          // "TALK_TO_NPC", "REACH_LOCATION", "DEFEAT_ENEMY", "COLLECT_ITEM", "CUTSCENE"
    String triggerTarget,        // NPC ID, location name, enemy type, item ID, cutscene ID
    String setFlag,             // Story flag to set on completion
    boolean mandatory           // If true, must complete to progress
) {}
```

**Step 2: Create StoryManager**

Per-player quest state tracking:
```java
public class StoryManager {
    // Per-player state
    private static final Map<UUID, PlayerStoryState> storyStates = new HashMap<>();

    public static class PlayerStoryState {
        private String currentChapterId;
        private int currentObjectiveIndex;
        private Set<String> completedObjectives = new HashSet<>();
        private Set<String> storyFlags = new HashSet<>();

        // NBT save/load
    }

    public static void completeObjective(UUID playerId, String objectiveId) {
        // Mark objective complete
        // Check if all mandatory objectives in chapter are done
        // If so, advance to next chapter
        // Send sync to client
    }

    public static StoryObjective getCurrentObjective(UUID playerId) { ... }

    // Check triggers
    public static void onNpcInteraction(UUID playerId, String npcId) { ... }
    public static void onLocationReached(UUID playerId, BlockPos pos) { ... }
    public static void onEnemyDefeated(UUID playerId, String enemyType) { ... }
}
```

**Step 3: Add story state to CradlePlayerData**

Add fields for `currentChapterId`, `currentObjectiveIndex`, `completedObjectives` (Set<String>), `storyFlags` (Set<String>). Include in NBT read/write.

**Step 4: Create StoryDataLoader**

Load chapter JSON files from `data/cradlemod/story/`. Parse with Gson.

**Step 5: Verify**

Run: `./gradlew build`
Expected: Compiles. Story data loads.

**Step 6: Commit**

```bash
git add src/main/java/com/cradle/mod/story/StoryManager.java
git add src/main/java/com/cradle/mod/story/StoryChapter.java
git add src/main/java/com/cradle/mod/story/StoryObjective.java
git add src/main/java/com/cradle/mod/story/StoryDataLoader.java
git add src/main/resources/data/cradlemod/story/
git add src/main/java/com/cradle/mod/CradlePlayerData.java
git commit -m "feat: StoryManager — quest tracking, chapters, objectives, data loading"
```

---

### Task 17: Objective HUD (Client)

**Files:**
- Create: `src/main/java/com/cradle/mod/network/StorySyncPayload.java`
- Create: `src/client/java/com/cradle/mod/hud/ObjectiveHudRenderer.java`
- Modify: `src/client/java/com/cradle/mod/CradleModClient.java` — register HUD element and payload handler
- Modify: `src/main/java/com/cradle/mod/CradleMod.java` — register payload, sync on objective change

**Step 1: Create StorySyncPayload (S2C)**

Sends current chapter title and objective description to the client.

```java
public record StorySyncPayload(
    String chapterTitle,
    String objectiveText,
    boolean hasMandatoryObjective
) implements CustomPacketPayload { ... }
```

**Step 2: Create ObjectiveHudRenderer**

Renders current objective text in the top-right corner of the screen:
- Small semi-transparent panel
- Chapter title in gold
- Objective text in white below it
- Only renders in Cradle mode when there's an active objective

```java
public class ObjectiveHudRenderer {
    private static String chapterTitle = "";
    private static String objectiveText = "";

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (objectiveText.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        int x = Minecraft.getInstance().getWindow().getGuiScaledWidth() - 160;
        int y = 10;
        // Draw panel background
        graphics.fill(x - 5, y - 3, x + 155, y + 28, 0x88000000);
        // Draw chapter title
        graphics.drawString(font, chapterTitle, x, y, 0xFFFFD700, false);
        // Draw objective
        graphics.drawString(font, objectiveText, x, y + 14, 0xFFFFFFFF, false);
    }

    public static void update(String chapter, String objective) {
        chapterTitle = chapter;
        objectiveText = objective;
    }
}
```

**Step 3: Register HUD element**

```java
HudElementRegistry.attachElementBefore(
    VanillaHudElements.CHAT,
    Identifier.fromNamespaceAndPath("cradlemod", "objective_hud"),
    ObjectiveHudRenderer::render
);
```

**Step 4: Register payload and handler**

Server sends `StorySyncPayload` whenever the objective changes. Client updates `ObjectiveHudRenderer`.

**Step 5: Verify**

Run: `./gradlew runClient`
Expected: Objective text visible in top-right corner in Cradle mode.

**Step 6: Commit**

```bash
git add src/main/java/com/cradle/mod/network/StorySyncPayload.java
git add src/client/java/com/cradle/mod/hud/ObjectiveHudRenderer.java
git add src/client/java/com/cradle/mod/CradleModClient.java
git add src/main/java/com/cradle/mod/CradleMod.java
git commit -m "feat: objective HUD — top-right display of current quest objective"
```

---

### Task 18: Journal Screen (Client)

**Files:**
- Create: `src/client/java/com/cradle/mod/screen/JournalScreen.java`
- Create: `src/main/java/com/cradle/mod/network/JournalDataPayload.java`
- Modify: `src/client/java/com/cradle/mod/CradleModClient.java` — register keybind (L key) and payload handler

**Step 1: Create JournalDataPayload (S2C)**

Sends list of chapters with their completion status and objective text. Player requests this when opening the journal.

**Step 2: Create JournalScreen**

Follow `CradleInfoScreen` pattern with scrollable panel:
- **Left column** — list of chapters (completed ones grayed out, current one highlighted)
- **Right area** — selected chapter's objectives with checkmarks for completed ones
- **Scrollable** — reuse the scissor/scroll pattern from CradleInfoScreen

**Step 3: Register keybind**

```java
JOURNAL_KEYBIND = KeyBindingHelper.registerKeyBinding(
    new KeyMapping("key.cradlemod.journal", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, CRADLE_CATEGORY)
);
```

In tick handler: `while (JOURNAL_KEYBIND.consumeClick()) { /* request journal data from server */ }`

**Step 4: Verify**

Run: `./gradlew runClient`
Expected: Press L → journal screen shows chapters and objectives.

**Step 5: Commit**

```bash
git add src/client/java/com/cradle/mod/screen/JournalScreen.java
git add src/main/java/com/cradle/mod/network/JournalDataPayload.java
git add src/client/java/com/cradle/mod/CradleModClient.java
git commit -m "feat: journal screen — L key, chapter list, objective tracking"
```

---

### Task 19: Location-Based Quest Triggers

**Files:**
- Create: `src/main/java/com/cradle/mod/story/LocationTrigger.java`
- Modify: `src/main/java/com/cradle/mod/story/StoryManager.java` — add location checking
- Modify: `src/main/java/com/cradle/mod/CradleMod.java` — tick-based location checks

**Step 1: Create LocationTrigger**

Defines named regions in the world. Each region has a center position and radius. When a player enters the region, it fires a trigger.

```java
public class LocationTrigger {
    private static final Map<String, TriggerZone> zones = new HashMap<>();

    public record TriggerZone(String id, BlockPos center, int radius) {}

    public static void registerZone(String id, BlockPos center, int radius) {
        zones.put(id, new TriggerZone(id, center, radius));
    }

    // Called every second (not every tick, for performance)
    public static void checkPlayerLocations(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            BlockPos playerPos = player.blockPosition();
            for (TriggerZone zone : zones.values()) {
                if (playerPos.closerThan(zone.center, zone.radius)) {
                    StoryManager.onLocationReached(player.getUUID(), zone.id);
                }
            }
        }
    }
}
```

**Step 2: Register Sacred Valley zones**

Register zones for: Wei clan area, Heaven's Glory, Mount Samara summit, Yerin's cave, valley exit point, training grounds.

**Step 3: Hook into server tick**

In `CradleMod.java` server tick, call `LocationTrigger.checkPlayerLocations()` every 20 ticks (1 second).

**Step 4: Verify**

Run: `./gradlew runClient`
Expected: Walking into a zone triggers the appropriate objective completion (visible in objective HUD).

**Step 5: Commit**

```bash
git add src/main/java/com/cradle/mod/story/LocationTrigger.java
git add src/main/java/com/cradle/mod/story/StoryManager.java
git add src/main/java/com/cradle/mod/CradleMod.java
git commit -m "feat: location-based quest triggers — zone detection for story objectives"
```

---

## Phase 6: Companion System

### Task 20: CompanionEntity — Following AI

**Files:**
- Create: `src/main/java/com/cradle/mod/entity/CompanionEntity.java`
- Create: `src/main/java/com/cradle/mod/entity/ai/FollowPlayerGoal.java`
- Modify: `src/main/java/com/cradle/mod/entity/CradleEntities.java` — register entity type

**Step 1: Create CompanionEntity**

Extends `StoryNpcEntity`. Adds:
- Owner UUID (the player they follow)
- Follow behavior — stays within 6 blocks
- Teleport if > 32 blocks away
- Stops following when player opens a screen (dialogue, inventory)

```java
public class CompanionEntity extends StoryNpcEntity {
    private UUID ownerUUID;

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FollowPlayerGoal(this, 1.2, 6.0f, 2.0f));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0f));
    }

    @Override
    public void tick() {
        super.tick();
        // Teleport if too far
        if (ownerUUID != null && !level().isClientSide) {
            Player owner = level().getPlayerByUUID(ownerUUID);
            if (owner != null && distanceTo(owner) > 32.0) {
                teleportTo(owner.getX(), owner.getY(), owner.getZ());
            }
        }
    }
}
```

**Step 2: Create FollowPlayerGoal**

```java
public class FollowPlayerGoal extends Goal {
    private final PathfinderMob mob;
    private final double speedModifier;
    private final float maxDistance;
    private final float minDistance;
    private Player owner;

    @Override
    public boolean canUse() {
        // Find owner within tracking range
        // Return true if distance > minDistance
    }

    @Override
    public void tick() {
        // Navigate toward owner, maintaining minDistance
    }
}
```

**Step 3: Register entity type**

Same pattern as STORY_NPC, separate entity type `COMPANION` in `CradleEntities`.

**Step 4: Verify**

Run: `./gradlew runClient`
Expected: Companion follows the player around, teleports when far away.

**Step 5: Commit**

```bash
git add src/main/java/com/cradle/mod/entity/CompanionEntity.java
git add src/main/java/com/cradle/mod/entity/ai/FollowPlayerGoal.java
git add src/main/java/com/cradle/mod/entity/CradleEntities.java
git commit -m "feat: CompanionEntity with follow AI and teleport"
```

---

### Task 21: Companion Combat AI

**Files:**
- Create: `src/main/java/com/cradle/mod/entity/ai/CompanionCombatGoal.java`
- Create: `src/main/java/com/cradle/mod/entity/CompanionAbilityExecutor.java`
- Modify: `src/main/java/com/cradle/mod/entity/CompanionEntity.java` — add combat goals

**Step 1: Create CompanionCombatGoal**

AI goal that detects threats and engages:
- **Target selection**: Attack what the owner attacks (owner's last hurt target), or what attacks the owner (owner's last hurt by).
- **Engagement range**: 16 blocks for ranged (Striker), 4 blocks for melee (Enforcer).
- **Ability usage**: Uses the companion's path abilities with cooldowns.

```java
public class CompanionCombatGoal extends Goal {
    @Override
    public void tick() {
        LivingEntity target = findTarget();
        if (target == null) return;

        double distance = mob.distanceTo(target);

        // Use abilities based on range
        if (distance < 4.0 && canUseEnforcer()) {
            useEnforcerAbility(target);
        } else if (distance < 16.0 && canUseStriker()) {
            useStrikerAbility(target);
        }

        // Navigate toward target
        mob.getNavigation().moveTo(target, 1.0);
    }
}
```

**Step 2: Create CompanionAbilityExecutor**

Reuses the existing `AbilityDefinitions` and `AbilityExecutor` logic. The companion has:
- A path (Endless Sword for NPC Yerin, Pure for NPC Lindon)
- A stage (advances with story)
- A madra pool (regenerates like player's)
- Cooldowns per ability

The executor looks up the companion's abilities by path and fires them using the same handler lambdas from `AbilityDefinitions`. Reuse `RemnantAbilityExecutor` patterns where possible.

**Step 3: Wire combat into CompanionEntity**

Add `CompanionCombatGoal` to goals with priority 0 (highest). Add madra regeneration in `tick()`.

**Step 4: Verify**

Run: `./gradlew runClient`
Expected: Companion attacks enemies that attack the player. Uses path abilities with visual effects.

**Step 5: Commit**

```bash
git add src/main/java/com/cradle/mod/entity/ai/CompanionCombatGoal.java
git add src/main/java/com/cradle/mod/entity/CompanionAbilityExecutor.java
git add src/main/java/com/cradle/mod/entity/CompanionEntity.java
git commit -m "feat: companion combat AI — targets threats, uses path abilities"
```

---

### Task 22: Companion Story Integration

**Files:**
- Modify: `src/main/java/com/cradle/mod/story/StoryManager.java` — companion spawn/despawn at story events
- Modify: `src/main/java/com/cradle/mod/entity/CompanionEntity.java` — contextual dialogue

**Step 1: Companion lifecycle**

The companion doesn't exist from the start:
- **Lindon's story**: Companion (Yerin) spawns after completing the "find Yerin" objective (chapter ~5-6).
- **Yerin's story**: Companion (Lindon) spawns when Lindon finds her in the cave (chapter ~4).

`StoryManager` calls `CompanionEntity.spawnCompanion(player, characterType)` at the right story beat. Before that point, the player is solo.

**Step 2: Contextual companion dialogue**

When the player right-clicks the companion, instead of a full dialogue tree, show a single context-aware line based on:
- Current chapter/objective
- Current location
- A pool of idle comments

Store these as a simple JSON file per companion (`data/cradlemod/dialogue/companion_yerin.json` / `companion_lindon.json`).

**Step 3: Companion stage advancement**

When the player completes certain story milestones, the companion's stage also advances. Define a mapping in `StoryManager`:
```
Chapter "leaving_valley" → companion advances to Iron
```

**Step 4: Verify**

Run: `./gradlew runClient`
Expected: Companion spawns at the right story moment. Right-click gives contextual lines. Companion advances with story.

**Step 5: Commit**

```bash
git add src/main/java/com/cradle/mod/story/StoryManager.java
git add src/main/java/com/cradle/mod/entity/CompanionEntity.java
git add src/main/resources/data/cradlemod/dialogue/companion_yerin.json
git add src/main/resources/data/cradlemod/dialogue/companion_lindon.json
git commit -m "feat: companion story integration — lifecycle, contextual dialogue, advancement"
```

---

## Phase 7: Cutscene System

### Task 23: CutsceneManager — Server Side

**Files:**
- Create: `src/main/java/com/cradle/mod/story/cutscene/CutsceneManager.java`
- Create: `src/main/java/com/cradle/mod/story/cutscene/CutsceneDefinition.java`
- Create: `src/main/java/com/cradle/mod/story/cutscene/CutsceneStep.java`
- Create: `src/main/java/com/cradle/mod/network/CutsceneStartPayload.java`
- Create: `src/main/java/com/cradle/mod/network/CutsceneStepPayload.java`
- Create: `src/main/java/com/cradle/mod/network/CutsceneEndPayload.java`
- Create: `src/main/java/com/cradle/mod/network/CutsceneAdvancePayload.java`

**Step 1: Define cutscene data model**

```java
public record CutsceneDefinition(
    String id,
    List<CutsceneStep> steps
) {}

public record CutsceneStep(
    String speakerName,     // Who's talking (null = narration)
    String text,            // Subtitle text
    int durationTicks,      // Auto-advance after this (0 = wait for click)
    String npcAction,       // "MOVE:x,y,z", "ATTACK", "EFFECT:particle", null
    String cameraTarget     // Entity ID or "PLAYER" — where camera looks
) {}
```

**Step 2: Create CutsceneManager**

Server-side controller:
```java
public class CutsceneManager {
    private static final Map<UUID, ActiveCutscene> activeCutscenes = new HashMap<>();

    public static void startCutscene(ServerPlayer player, String cutsceneId) {
        CutsceneDefinition def = loadCutscene(cutsceneId);
        activeCutscenes.put(player.getUUID(), new ActiveCutscene(def, 0));
        // Send CutsceneStartPayload to lock player input
        // Send first CutsceneStepPayload
    }

    public static void advance(ServerPlayer player) {
        ActiveCutscene active = activeCutscenes.get(player.getUUID());
        if (active == null) return;
        active.stepIndex++;
        if (active.stepIndex >= active.definition.steps().size()) {
            endCutscene(player);
            return;
        }
        // Execute NPC actions for this step
        // Send next CutsceneStepPayload
    }

    public static void endCutscene(ServerPlayer player) {
        activeCutscenes.remove(player.getUUID());
        // Send CutsceneEndPayload to unlock player
        // Trigger story objective completion
    }
}
```

**Step 3: Create network payloads**

- `CutsceneStartPayload` (S2C) — lock player input
- `CutsceneStepPayload` (S2C) — current step's speaker/text/duration
- `CutsceneEndPayload` (S2C) — unlock player input
- `CutsceneAdvancePayload` (C2S) — player clicked to advance

**Step 4: Register payloads**

Follow existing pattern in CradleMod and CradleModClient.

**Step 5: Verify**

Run: `./gradlew build`
Expected: Compiles.

**Step 6: Commit**

```bash
git add src/main/java/com/cradle/mod/story/cutscene/
git add src/main/java/com/cradle/mod/network/CutsceneStartPayload.java
git add src/main/java/com/cradle/mod/network/CutsceneStepPayload.java
git add src/main/java/com/cradle/mod/network/CutsceneEndPayload.java
git add src/main/java/com/cradle/mod/network/CutsceneAdvancePayload.java
git commit -m "feat: CutsceneManager — server-side cutscene controller with network payloads"
```

---

### Task 24: Cutscene Renderer (Client)

**Files:**
- Create: `src/client/java/com/cradle/mod/hud/CutsceneRenderer.java`
- Modify: `src/client/java/com/cradle/mod/CradleModClient.java` — register handlers and HUD

**Step 1: Create CutsceneRenderer**

Client-side cutscene display:
- **Letterbox bars** — black bars at top and bottom of screen (cinematic feel)
- **Subtitle area** — speaker name in gold, text in white, at the bottom above the letterbox bar
- **"Click to continue"** prompt — faded text below subtitle when waiting for player input
- **Input lock** — intercept all movement/action keys while cutscene is active

```java
public class CutsceneRenderer {
    private static boolean active = false;
    private static String speakerName = "";
    private static String text = "";
    private static boolean waitingForClick = false;

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!active) return;

        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int barHeight = 40;

        // Top letterbox bar
        graphics.fill(0, 0, screenW, barHeight, 0xFF000000);
        // Bottom letterbox bar
        graphics.fill(0, screenH - barHeight, screenW, screenH, 0xFF000000);

        // Subtitle panel
        int subtitleY = screenH - barHeight - 50;
        graphics.fill(20, subtitleY, screenW - 20, screenH - barHeight - 5, 0xAA000000);

        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, speakerName, 30, subtitleY + 8, 0xFFFFD700, false);
        // Word-wrap and render text
        graphics.drawString(font, text, 30, subtitleY + 22, 0xFFFFFFFF, false);

        if (waitingForClick) {
            graphics.drawCenteredString(font, "Click to continue...",
                screenW / 2, screenH - barHeight + 14, 0x88FFFFFF);
        }
    }

    public static boolean handleClick() {
        if (!active || !waitingForClick) return false;
        ClientPlayNetworking.send(new CutsceneAdvancePayload());
        return true;
    }

    public static boolean isActive() { return active; }
}
```

**Step 2: Input lock during cutscene**

In `CradleModClient` tick handler, skip all keybind processing when `CutsceneRenderer.isActive()`. Override mouse click in the cutscene to advance instead of normal action.

**Step 3: Register HUD element**

```java
HudElementRegistry.attachElementBefore(
    VanillaHudElements.CHAT,
    Identifier.fromNamespaceAndPath("cradlemod", "cutscene"),
    CutsceneRenderer::render
);
```

**Step 4: Verify**

Run: `./gradlew runClient`
Expected: Trigger a cutscene → letterbox bars appear, subtitle text displays, clicking advances.

**Step 5: Commit**

```bash
git add src/client/java/com/cradle/mod/hud/CutsceneRenderer.java
git add src/client/java/com/cradle/mod/CradleModClient.java
git commit -m "feat: cutscene renderer — letterbox bars, subtitles, input lock"
```

---

## Phase 8: Story Content — Sacred Valley Arc

### Task 25: Lindon Chapter 1 — Wei Clan Tutorial

**Files:**
- Create: `src/main/resources/data/cradlemod/story/lindon_ch1_wei_clan.json`
- Create: `src/main/resources/data/cradlemod/dialogue/wei_shi_seisha.json`
- Create: `src/main/resources/data/cradlemod/dialogue/wei_shi_jaran.json`
- Modify: `src/main/resources/data/cradlemod/dialogue/elder_whisper.json`

**Step 1: Create chapter data**

Chapter 1 objectives for Lindon:
1. "Explore the Wei clan village" — location trigger at training grounds
2. "Speak to your mother, Seisha" — talk to NPC
3. "Speak to Elder Whisper" — talk to NPC (teaches about cycling)
4. "Practice cycling at the training grounds" — cycle for 30 seconds (custom trigger)
5. "Return to Elder Whisper" — talk to NPC

**Step 2: Create NPC dialogue**

Write dialogue trees for Seisha, Jaran, and Elder Whisper. Each has story-relevant lines plus optional lore dialogue.

**Step 3: Verify**

Run: `./gradlew runClient`
Expected: Start as Lindon, objectives guide through Wei clan tutorial, dialogue works.

**Step 4: Commit**

```bash
git add src/main/resources/data/cradlemod/story/lindon_ch1_wei_clan.json
git add src/main/resources/data/cradlemod/dialogue/
git commit -m "feat: Lindon chapter 1 — Wei clan tutorial with dialogue"
```

---

### Task 26: Lindon Chapter 2 — Li Markuth Cutscene

**Files:**
- Create: `src/main/resources/data/cradlemod/story/lindon_ch2_li_markuth.json`
- Create: `src/main/resources/data/cradlemod/cutscene/li_markuth_arrival.json`

**Step 1: Create Li Markuth cutscene data**

Steps:
1. Sky darkens (could set time to night via server command)
2. "A terrible presence descends upon the valley..." (narration)
3. Li Markuth entity spawns dramatically
4. Li Markuth: "Sacred Valley... how small you have become." (monologue)
5. Li Markuth demonstrates power (explosion effects, lightning)
6. Suriel appears (bright light, particles)
7. Suriel stops Li Markuth silently
8. Suriel gives Lindon the marble — "I cannot change your fate directly. But I can give you this."
9. Vision of the outside world (screen flash/fade)
10. Cutscene ends

**Step 2: Create chapter objectives**

1. "Witness the arrival of Li Markuth" — cutscene trigger (auto-triggered by story progression)
2. "Examine Suriel's marble" — collect/interact with item

**Step 3: Verify**

Run: `./gradlew runClient`
Expected: Cutscene plays with letterbox bars, Li Markuth monologues, Suriel appears.

**Step 4: Commit**

```bash
git add src/main/resources/data/cradlemod/story/lindon_ch2_li_markuth.json
git add src/main/resources/data/cradlemod/cutscene/li_markuth_arrival.json
git commit -m "feat: Lindon chapter 2 — Li Markuth arrival cutscene"
```

---

### Task 27: Lindon Chapter 3 — Seven-Year Festival Tournament

**Files:**
- Create: `src/main/resources/data/cradlemod/story/lindon_ch3_tournament.json`
- Create: `src/main/java/com/cradle/mod/story/TournamentManager.java`
- Create: `src/main/resources/data/cradlemod/cutscene/suriel_vision.json`

**Step 1: Create TournamentManager**

Manages a series of NPC combat encounters:
- Tournament takes place at the Wei clan training grounds
- Player fights 3-4 NPC opponents in sequence (each is a temporary hostile NPC)
- Between fights, short dialogue exchanges
- After defeating an opponent mid-tournament, Suriel vision cutscene triggers
- Player wins the tournament

The NPC opponents are temporary combat NPCs (not story NPCs — they can take damage). They use abilities from various paths. Difficulty scales with each round.

**Step 2: Create Suriel vision cutscene**

Mid-tournament cutscene:
1. Time freezes (action bar message)
2. Suriel appears beside Lindon
3. Suriel shows vision of Yerin (text description)
4. Suriel gives the marble
5. "Your fate is your own to forge."
6. Cutscene ends, tournament resumes

**Step 3: Create chapter objectives**

1. "Go to the Wei clan training grounds for the Seven-Year Festival"
2. "Defeat your first opponent"
3. "Defeat your second opponent"
4. Suriel cutscene auto-triggers
5. "Win the Seven-Year Festival tournament"

**Step 4: Verify**

Run: `./gradlew runClient`
Expected: Tournament fights work, Suriel cutscene triggers mid-tournament, tournament completes.

**Step 5: Commit**

```bash
git add src/main/resources/data/cradlemod/story/lindon_ch3_tournament.json
git add src/main/java/com/cradle/mod/story/TournamentManager.java
git add src/main/resources/data/cradlemod/cutscene/suriel_vision.json
git commit -m "feat: Lindon chapter 3 — Seven-Year Festival tournament with Suriel vision"
```

---

### Task 28: Lindon Chapter 4 — Heaven's Glory Trial

**Files:**
- Create: `src/main/resources/data/cradlemod/story/lindon_ch4_heavens_glory.json`
- Create: `src/main/java/com/cradle/mod/story/TrialOfAscension.java`
- Create: `src/main/resources/data/cradlemod/dialogue/heavens_glory_elder.json`

**Step 1: Create TrialOfAscension**

A timed parkour challenge in Heaven's Glory:
- Player starts at the bottom of the course
- Timer starts (displayed on action bar)
- Player must reach the top within a time limit (60-90 seconds)
- On completion, teleport to the vault room
- In the vault, place 3-4 chests with different items — player picks one

```java
public class TrialOfAscension {
    private static final Map<UUID, Long> activeTrials = new HashMap<>();

    public static void startTrial(ServerPlayer player) {
        activeTrials.put(player.getUUID(), System.currentTimeMillis());
        player.displayClientMessage(
            Component.literal("Trial of Glorious Ascension — BEGIN!"), true);
        // Teleport to start position
    }

    public static void checkCompletion(ServerPlayer player) {
        // Check if player reached the top position
        // If within time limit, success → teleport to vault
    }
}
```

**Step 2: Create chapter objectives**

1. "Travel to Heaven's Glory school"
2. "Speak with the Heaven's Glory elder"
3. "Complete the Trial of Glorious Ascension" (parkour)
4. "Choose your reward from the vault"

**Step 3: Verify**

Run: `./gradlew runClient`
Expected: Parkour trial has a timer, completing it opens the vault. Player picks a reward.

**Step 4: Commit**

```bash
git add src/main/resources/data/cradlemod/story/lindon_ch4_heavens_glory.json
git add src/main/java/com/cradle/mod/story/TrialOfAscension.java
git add src/main/resources/data/cradlemod/dialogue/heavens_glory_elder.json
git commit -m "feat: Lindon chapter 4 — Heaven's Glory, Trial of Ascension, vault reward"
```

---

### Task 29: Lindon Chapter 5 — Finding Yerin & Companion Join

**Files:**
- Create: `src/main/resources/data/cradlemod/story/lindon_ch5_find_yerin.json`
- Create: `src/main/resources/data/cradlemod/dialogue/yerin_first_meeting.json`
- Modify: `src/main/java/com/cradle/mod/story/StoryManager.java` — spawn companion on chapter completion

**Step 1: Create chapter objectives**

1. "Search for the girl from Suriel's vision" — objective HUD guides toward Yerin's cave
2. "Enter the cave" — location trigger
3. "Speak to Yerin" — dialogue with multiple exchanges
4. "Yerin has joined you" — companion spawns, objective auto-completes

**Step 2: Yerin's first meeting dialogue**

Write dialogue tree for the first meeting — suspicious Yerin, cautious exchanges, eventual agreement to work together.

**Step 3: Companion spawn trigger**

When chapter 5 completes, `StoryManager` calls:
```java
CompanionEntity.spawnCompanion(player, PlayerCharacter.YERIN);
```

**Step 4: Verify**

Run: `./gradlew runClient`
Expected: Find cave, talk to Yerin, companion spawns and follows.

**Step 5: Commit**

```bash
git add src/main/resources/data/cradlemod/story/lindon_ch5_find_yerin.json
git add src/main/resources/data/cradlemod/dialogue/yerin_first_meeting.json
git add src/main/java/com/cradle/mod/story/StoryManager.java
git commit -m "feat: Lindon chapter 5 — find Yerin, first meeting, companion joins"
```

---

### Task 30: Lindon Chapter 6 — Ancestral Tomb & Leaving Sacred Valley

**Files:**
- Create: `src/main/resources/data/cradlemod/story/lindon_ch6_leaving.json`
- Create: `src/main/resources/data/cradlemod/cutscene/leaving_valley.json`
- Modify: `src/main/java/com/cradle/mod/story/StoryManager.java` — handle split-core event

**Step 1: Create chapter objectives**

1. "Travel to the Ancestral Tomb on Mount Samara"
2. "Retrieve Adama's sword" — collect item from tomb chest
3. "Return to Yerin" — Yerin is fighting enemies at a location (spawn hostile NPCs for her to fight)
4. "Lindon splits his core" — cutscene/dialogue moment
5. "Leave Sacred Valley" — reach the valley exit point

**Step 2: Split core mechanic**

When Lindon splits his core at this story point:
- `CradlePlayerData` gets a new flag: `splitCore = true`
- This enables the Twin Stars mechanic (Pure + Blackflame) for future gameplay
- For now, just set the flag and add Black Flame as a secondary path

**Step 3: Leaving cutscene**

Brief cutscene as the characters leave the valley — looking back at the mountains, dialogue about the journey ahead.

**Step 4: Story completion**

When the "Leave Sacred Valley" objective completes:
- `StoryManager` sets `mandatoryQuestsComplete = true`
- From this point, all remaining quests are optional
- Player receives an action bar message: "The world of Cradle is now open to you."

**Step 5: Verify**

Run: `./gradlew runClient`
Expected: Complete the Sacred Valley arc, get the "world open" message, free to explore.

**Step 6: Commit**

```bash
git add src/main/resources/data/cradlemod/story/lindon_ch6_leaving.json
git add src/main/resources/data/cradlemod/cutscene/leaving_valley.json
git add src/main/java/com/cradle/mod/story/StoryManager.java
git commit -m "feat: Lindon chapter 6 — Ancestral Tomb, split core, leave Sacred Valley"
```

---

### Task 31: Yerin's Story Chapters

**Files:**
- Create: `src/main/resources/data/cradlemod/story/yerin_ch1_arrival.json`
- Create: `src/main/resources/data/cradlemod/story/yerin_ch2_adama_death.json`
- Create: `src/main/resources/data/cradlemod/story/yerin_ch3_cave.json`
- Create: `src/main/resources/data/cradlemod/cutscene/adama_death.json`
- Create: `src/main/resources/data/cradlemod/dialogue/adama.json`
- Create: `src/main/resources/data/cradlemod/dialogue/lindon_first_meeting.json`

**Step 1: Yerin Chapter 1 — Arrival with Adama**

Objectives:
1. "Follow Adama into Sacred Valley" — Adama is a temporary companion NPC
2. "Explore the valley outskirts" — location trigger
3. "Speak with Adama about the valley" — dialogue (tutorial, explains cycling and sword arts)

**Step 2: Yerin Chapter 2 — Adama's Death**

Objectives:
1. "Something is wrong..." — location trigger (approach a specific area)
2. Adama's death cutscene auto-triggers
3. "Survive" — Yerin fights a few enemies alone (hostile NPCs attack)
4. "Find shelter" — location trigger at cave

**Step 3: Yerin Chapter 3 — Meeting Lindon**

Objectives:
1. "Rest in the cave" — wait / interact with bed
2. Lindon finds Yerin — NPC Lindon arrives, dialogue
3. "Lindon has joined you" — companion spawns
4. Merges into shared story from Lindon chapter 6 (Ancestral Tomb, leave valley)

**Step 4: Verify**

Run: `./gradlew runClient`
Expected: Yerin's story flows from arrival → Adama's death → cave → Lindon joins → shared ending.

**Step 5: Commit**

```bash
git add src/main/resources/data/cradlemod/story/yerin_ch1_arrival.json
git add src/main/resources/data/cradlemod/story/yerin_ch2_adama_death.json
git add src/main/resources/data/cradlemod/story/yerin_ch3_cave.json
git add src/main/resources/data/cradlemod/cutscene/adama_death.json
git add src/main/resources/data/cradlemod/dialogue/adama.json
git add src/main/resources/data/cradlemod/dialogue/lindon_first_meeting.json
git commit -m "feat: Yerin chapters 1-3 — arrival, Adama's death, meeting Lindon"
```

---

## Phase 9: Polish & Integration

### Task 32: Progression Gating in Cradle Mode

**Files:**
- Modify: `src/main/java/com/cradle/mod/BreakthroughManager.java` — add story gates
- Modify: `src/main/java/com/cradle/mod/story/StoryManager.java` — expose gate checks

**Step 1: Gate breakthroughs to story milestones**

In Cradle mode during the Sacred Valley arc (`mandatoryQuestsComplete == false`):
- Override `BreakthroughManager.canAdvance()` to also check story progress
- Example: can't reach Copper until chapter 1 is complete, can't reach Iron until leaving the valley
- After `mandatoryQuestsComplete`, normal progression rules apply

**Step 2: Verify**

Run: `./gradlew runClient`
Expected: Can't advance past story-gated stages during Sacred Valley arc.

**Step 3: Commit**

```bash
git add src/main/java/com/cradle/mod/BreakthroughManager.java
git add src/main/java/com/cradle/mod/story/StoryManager.java
git commit -m "feat: progression gating — breakthroughs tied to story milestones in Sacred Valley"
```

---

### Task 33: Cradle Mode Sync Flag

**Files:**
- Modify: `src/main/java/com/cradle/mod/network/CradleSyncPayload.java` — add story mode flags
- Modify: `src/client/java/com/cradle/mod/ClientCradleData.java` — add story mode client state

**Step 1: Add flags to CradleSyncPayload**

Add flags for:
- `FLAG_CRADLE_MODE` — whether this world is in Cradle mode (enables HUD elements)
- `FLAG_STORY_ACTIVE` — whether mandatory story is active

These use the existing bitmask system. Find available bit positions.

**Step 2: Update ClientCradleData**

Store `isCradleMode` and `isStoryActive` booleans. Use these to conditionally render the objective HUD and journal keybind.

**Step 3: Verify**

Run: `./gradlew build`
Expected: Compiles. Client correctly knows whether it's in Cradle mode.

**Step 4: Commit**

```bash
git add src/main/java/com/cradle/mod/network/CradleSyncPayload.java
git add src/client/java/com/cradle/mod/ClientCradleData.java
git commit -m "feat: sync Cradle mode and story state flags to client"
```

---

### Task 34: Final Integration & Debug Commands

**Files:**
- Modify: `src/main/java/com/cradle/mod/CycleCommand.java` — add story debug commands

**Step 1: Add debug commands**

Extend the existing `/cycle` command with story subcommands:
- `/cycle story chapter <chapterId>` — jump to a specific chapter
- `/cycle story objective <objectiveId>` — complete a specific objective
- `/cycle story reset` — reset all story progress
- `/cycle story cutscene <cutsceneId>` — play a cutscene
- `/cycle story companion` — spawn/despawn companion

**Step 2: Verify**

Run: `./gradlew runClient`
Expected: Debug commands work, can jump between story chapters for testing.

**Step 3: Commit**

```bash
git add src/main/java/com/cradle/mod/CycleCommand.java
git commit -m "feat: story debug commands — /cycle story chapter/objective/reset/cutscene"
```

---

### Task 35: Full Playthrough Test

**No new files — verification task.**

**Step 1: Test Lindon's story**

Create a new Cradle mode world as Lindon. Play through all 6 chapters:
1. Wei clan tutorial → cycling works, dialogue with NPCs
2. Li Markuth cutscene → letterbox bars, monologue text
3. Tournament → fight NPCs, Suriel vision mid-tournament, win
4. Heaven's Glory → parkour trial, vault reward
5. Find Yerin → companion spawns and follows
6. Ancestral Tomb → get sword, split core, leave valley
7. Post-valley → quests optional, free exploration

**Step 2: Test Yerin's story**

Create a new Cradle mode world as Yerin. Play through all 3 chapters + shared ending:
1. Arrival with Adama → tutorial dialogue
2. Adama's death → cutscene
3. Cave → meet Lindon, companion joins
4. Shared: Ancestral Tomb, leave valley

**Step 3: Test Free Mode**

Create a Free mode world. Verify existing systems work unchanged — path selection, cycling, abilities, duels, remnants.

**Step 4: Fix any issues found**

Address bugs, polish rough edges.

**Step 5: Commit**

```bash
git add -A
git commit -m "fix: playthrough testing fixes and polish"
```

---

## Summary

| Phase | Tasks | Description |
|-------|-------|-------------|
| 1 | 1-3 | Game mode foundation (manager, character select, world type) |
| 2 | 4-6 | Sacred Valley terrain (heightmap, biomes, spawn) |
| 3 | 7-10 | Structures (framework, Wei clan, Heaven's Glory, landmarks) |
| 4 | 11-15 | NPC framework (entity, renderer, AI, dialogue system, screen) |
| 5 | 16-19 | Quest system (StoryManager, HUD, journal, location triggers) |
| 6 | 20-22 | Companion system (follow AI, combat AI, story integration) |
| 7 | 23-24 | Cutscene system (manager, renderer) |
| 8 | 25-31 | Story content (Lindon 6 chapters, Yerin 3 chapters) |
| 9 | 32-35 | Polish (progression gating, sync flags, debug commands, testing) |

**Total: 35 tasks across 9 phases.**

Each phase builds on the previous. Phases 1-3 create the world. Phase 4 adds NPCs. Phase 5 adds quests. Phase 6 adds the companion. Phase 7 adds cutscenes. Phase 8 wires it all together with story content. Phase 9 polishes.

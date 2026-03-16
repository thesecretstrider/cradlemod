# Cradle World & Story Mode Design

## Overview

Add a Story Mode to the Cradle mod that replaces the vanilla overworld with Sacred Valley, introduces NPCs with dialogue, a companion system, cutscenes, and a faithful adaptation of the Cradle book series starting with the Sacred Valley arc (book 1). After completing the Sacred Valley prologue, the world opens up for free exploration with optional quests.

Inspired by Assassin's Creed Odyssey — Sacred Valley is Kephallonia. Guided intro, then the world is yours.

## Game Modes

Two modes selectable at world creation:

### Free Mode
- Vanilla overworld, random seed
- Current mod systems (pick any path, free progression, all 5 paths available)
- No story, no NPCs, no custom world gen
- Everything that exists today

### Cradle Mode
- Custom world gen replaces the overworld with Sacred Valley
- Character selection: play as **Lindon** or **Yerin**
- **Sacred Valley arc is mandatory** — guided prologue that teaches systems and tells the story
- **After leaving the valley** — world opens up, main quests are optional, side quests available
- The character you didn't pick becomes your always-following combat companion

### Character Differences

**Lindon:**
- Starts at Foundation stage, madra-deprived (not truly Unsouled — cycling works)
- Path is set to Pure initially, gains Blackflame later (Twin Stars)
- Companion: NPC Yerin (Endless Sword path) — joins after Heaven's Glory

**Yerin:**
- Starts at Copper on the Endless Sword path
- Begins with Adama (Sword Sage) alive
- After Adama's death and meeting Lindon, companion becomes NPC Lindon

## Sacred Valley World Generation

A custom `ChunkGenerator` replaces the vanilla overworld. Sacred Valley is a mountain-ringed basin, roughly **1500x1500 blocks**.

### Terrain Structure
- **Ring of mountains** around the perimeter — steep, impassable peaks. Barrier blocks at the tops for clean invisible walls.
- **Central basin** — rolling hills, forests, rivers. Elevation ~Y64-80.
- **Mount Samara** — tall mountain at the center (~Y180). Ancestor's Tomb at the summit. Samara ring of trees around the base.

### Factions & Territories

**4 Schools (scattered around the valley):**
1. Heaven's Glory school — built into a mountainside, temple-style. **Fully detailed** (interiors, NPCs — story location).
2. Holy Wind school — visible from afar. **Exterior only** for now.
3. Fallen Leaf school — visible from afar. **Exterior only** for now.
4. Golden Sword school — visible from afar. **Exterior only** for now.

**3 Clans:**
1. Wei clan — village with wooden buildings, training grounds, family homes. **Fully detailed** (interiors, NPCs — story location).
2. Kazan clan — visible from afar. **Exterior only** for now.
3. Li clan — visible from afar. **Exterior only** for now.

### Key Landmarks
- Ancestor's Tomb (Mount Samara peak)
- The cave where Yerin hides / Lindon and Yerin meet
- Wei clan transcription hall
- Wei clan training grounds
- Heaven's Glory treasure hall
- Forest areas with remnant spawns and vital fruit bushes
- Samara ring of ancient trees

### Generation Approach
All structures generated programmatically in Java (block-by-block placement). Heightmap function defines the valley bowl shape. Biome painter assigns custom biomes (valley floor, mountain, river, forest). Structure placement at fixed coordinates.

## NPC Framework

### StoryNpcEntity
New entity base class for all NPCs:
- **Right-click to talk** — opens custom DialogueScreen
- **Invulnerable** — damage is ignored, no knockback, cannot be killed
- **Named & persistent** — stored in world data, don't despawn
- **Path-aware** — each NPC has a path and stage, displays correct goldsign/aura visuals
- **Scheduled behavior** — simple daily routines via waypoint lists (stand at training grounds by day, go inside at night)
- **Faction tag** — Wei clan, Heaven's Glory, etc. Determines disposition toward player

### Dialogue System
- Dialogue trees stored as **JSON data files** in `data/cradlemod/dialogue/`
- Each node: speaker name, dialogue text, list of response options
- Response options can have **conditions** (story progress, player stage, items) and **effects** (advance quest, give item, set flag)
- **DialogueScreen** — custom screen with NPC name at top, dialogue text in center, clickable response buttons at bottom

### Key NPCs (Sacred Valley)
- Elder Whisper (Wei clan elder)
- Wei Shi Seisha (Lindon's mother)
- Wei Shi Jaran (Lindon's father)
- Lindon / Yerin (whichever you didn't pick — becomes companion)
- Adama (Sword Sage, Yerin's master — dies early)
- Heaven's Glory elders
- Wei clan members (training grounds, village life)
- Li Markuth (cutscene only — not a regular NPC)
- Suriel (cutscene only)

## Companion System

The companion (NPC Yerin for Lindon's story, NPC Lindon for Yerin's story) is a specialized NPC that always follows the player and fights alongside them.

### Movement AI
- Follows within 6 blocks of the player
- Teleports if further than 32 blocks (like tamed wolves)
- Pathfinds around obstacles, can jump gaps
- Stands still when player is talking to other NPCs

### Combat AI
- Attacks whatever the player attacks, or whatever attacks the player
- Uses abilities from their path (existing ability system)
- Has own madra pool and cooldowns — fights authentically
- Scales in power with story progression (advances stages at story milestones)

### Story Integration
- Right-click for contextual dialogue about current area or quest
- Invulnerable (same as all NPCs)
- Joins at specific story points (not from the very start for both characters)
- Separates from player during certain story segments where it makes sense

## Cutscene System

For major story moments that aren't interactive dialogue.

### How It Works
- Lock player movement and input
- Display speaker name + text at bottom (subtitle-style)
- Control NPC positions and actions (move, attack, effects)
- Advance on player click or automatically
- Characters speak (monologue) — no player dialogue choices during cutscenes

### Key Cutscenes (Sacred Valley)
- Adama's death (Yerin's story opening)
- Li Markuth's arrival — evil monologue, displays of power
- Suriel's intervention — stops Li Markuth, gives Lindon the marble
- Leaving Sacred Valley

## Quest & Story System

Only active in Cradle Mode.

### StoryManager
Server-side class tracking quest state per player:
- Current **chapter** (major story arc)
- Current **objective** within chapter
- Completed objectives stored as flags
- Gates breakthroughs during Sacred Valley arc
- After leaving valley: tracking only, no gating

### Objective HUD
- Small text in top-right showing current objective
- Updates automatically on completion

### Journal Screen (new keybind)
- Lists chapters, completed objectives, current objective
- Brief recap text per completed chapter
- Tracks both main story and side quests

### Quest Triggers
- Talk to NPC (dialogue option triggers completion)
- Reach a location (enter area)
- Defeat an enemy
- Collect an item
- Cutscene completion

### Story Data
Stored as JSON in `data/cradlemod/story/` — data-driven, easy to edit without Java changes.

## Sacred Valley Story Outline

### Lindon's Story
1. Start in Wei clan — tutorial: movement, cycling (cycling works — Lindon isn't actually Unsouled, just madra-deprived)
2. Meet Elder Whisper — learn about sacred arts, the valley
3. Li Markuth arrives — **cutscene**, early in the story
4. The Seven-Year Festival — tournament event. Mid-tournament, after defeating an opponent, **Suriel cutscene triggers**: Lindon receives the marble and vision of the outside world (including a vision of Yerin). Lindon wins the tournament.
5. Journey to Heaven's Glory school — the Trial of Glorious Ascension (speed trial / parkour challenge). Reward: pick an item from the vault.
6. Go adventuring to find Yerin — **companion joins**
7. Visit the Ancestral Tomb to retrieve Adama's sword for Yerin. Lindon splits his core. Meanwhile Yerin fights enemies (parallel action).
8. Leave Sacred Valley — **prologue ends, world opens up**

### Yerin's Story
1. Arrive in Sacred Valley with Adama (Sword Sage)
2. Adama's death — **cutscene**
3. Yerin hides in a cave, wounded and alone
4. Lindon finds her — **companion joins, stories converge**
5. Same as Lindon's story from step 7 onward (Ancestral Tomb, leave valley)

## Existing Systems Integration

### Player Paths
- In Cradle Mode, player path is set by character choice (not the PathSelectionScreen)
- Lindon: Pure path initially, gains Blackflame later (Twin Stars)
- Yerin: Endless Sword from the start
- Other paths (Stellar Spear, Cloud Hammer, Hollow King, Black Flame) used by NPCs and enemies

### Progression Gating (Sacred Valley Only)
- Breakthroughs tied to story milestones instead of just level + items
- After leaving valley: existing progression system resumes (level + items)

### Existing Abilities & Combat
- All existing ability and combat systems work unchanged
- NPCs and companions use the same ability system
- Remnants still spawn and can be absorbed at the right stage

## Future Expansion

### After Sacred Valley (Later Phases)
- Blackflame Empire region (books 2-3)
- Akura territory, Ninecloud Court, etc.
- More story chapters (optional quests post-valley)
- Side quest content
- Flesh out the 4 exterior-only schools and 2 exterior-only clans
- Additional companion NPCs from later books

### World Scaling
- Each new region added as world gen expands beyond Sacred Valley
- Portal/travel system to move between regions
- New NPCs, enemies, and story content per region

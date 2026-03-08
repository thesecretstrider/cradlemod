# Remnant System Overhaul Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Transform remnants from generic humanoid ghosts into mob-shaped spirits with glowing eyes, stage-relative damage, ability-using AI, and a reworked Herald trial.

**Architecture:** Polymorphic RemnantEntity stores source mob type as a synced string field. The client-side renderer dynamically resolves mob models and applies ghostly treatment (translucent tint + eye overlay texture). Ability AI is a new server-side component that reads a copied PlayerLoadout from the remnant's NBT.

**Tech Stack:** Minecraft 1.21.11, Fabric API, Java 21. Official Mojang mappings.

**Design Doc:** `docs/plans/2026-03-08-remnant-overhaul-design.md`

---

## Task 1: Add Source Mob Type and Render Scale to RemnantEntity

**Files:**
- Modify: `src/main/java/com/cradle/mod/entity/RemnantEntity.java`

**Context:** RemnantEntity currently has two synced fields (`REMNANT_PATH`, `POWER_LEVEL`). We need to add `SOURCE_MOB_TYPE` (String, e.g. `"minecraft:spider"`) and `RENDER_SCALE` (Float, default 1.0). These sync to the client for the renderer to use.

**Step 1: Add new synced data fields**

After line 44 (the POWER_LEVEL field), add:

```java
private static final EntityDataAccessor<String> SOURCE_MOB_TYPE =
        SynchedEntityData.defineId(RemnantEntity.class, EntityDataSerializers.STRING);
private static final EntityDataAccessor<Float> RENDER_SCALE =
        SynchedEntityData.defineId(RemnantEntity.class, EntityDataSerializers.FLOAT);
```

**Step 2: Register defaults in defineSynchedData**

In `defineSynchedData()` (line 70), after `builder.define(POWER_LEVEL, 1)`, add:

```java
builder.define(SOURCE_MOB_TYPE, "minecraft:zombie");
builder.define(RENDER_SCALE, 1.0f);
```

**Step 3: Add getters/setters**

After the existing power level getter/setter block (after line 92), add:

```java
public String getSourceMobType() {
    return this.entityData.get(SOURCE_MOB_TYPE);
}

public void setSourceMobType(String mobType) {
    this.entityData.set(SOURCE_MOB_TYPE, mobType != null ? mobType : "minecraft:zombie");
}

public float getRenderScale() {
    return this.entityData.get(RENDER_SCALE);
}

public void setRenderScale(float scale) {
    this.entityData.set(RENDER_SCALE, Math.max(0.1f, scale));
}
```

**Step 4: Update initRemnant to accept source mob type**

Change the `initRemnant` method signature (line 106) to accept a source mob type:

```java
public void initRemnant(CradlePlayerData.Path path, int powerLevel, UUID ownerUUID, String sourceMobType) {
    setRemnantPath(path);
    setPowerLevel(powerLevel);
    this.ownerUUID = ownerUUID;
    setSourceMobType(sourceMobType);

    // Scale health and damage based on power level (updated formula)
    float health = 20.0f + (powerLevel * 10.0f);
    this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
    this.setHealth(health);
    // Attack damage is now calculated dynamically via stage-relative scaling (Task 2)
    // Keep a base value for the attribute but actual damage is overridden in doHurtTarget
    this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0);
}
```

Also add an overload to preserve backward compat with existing callers until Task 3 updates them:

```java
public void initRemnant(CradlePlayerData.Path path, int powerLevel, UUID ownerUUID) {
    initRemnant(path, powerLevel, ownerUUID, "minecraft:zombie");
}
```

**Step 5: Update NBT persistence**

In `addAdditionalSaveData` (line 413), add after the OwnerUUID block:

```java
output.putString("SourceMobType", getSourceMobType());
output.putFloat("RenderScale", getRenderScale());
```

In `readAdditionalSaveData` (line 425), add after the OwnerUUID block:

```java
this.entityData.set(SOURCE_MOB_TYPE, input.getStringOr("SourceMobType", "minecraft:zombie"));
this.entityData.set(RENDER_SCALE, input.getFloatOr("RenderScale", 1.0f));
```

Also update the restored health formula in `readAdditionalSaveData`:

```java
float health = 20.0f + (power * 10.0f); // Was 5.0f, now 10.0f per design
```

**Step 6: Build and verify**

Run: `./gradlew build`
Expected: Build succeeds with no errors.

**Step 7: Commit**

```bash
git add src/main/java/com/cradle/mod/entity/RemnantEntity.java
git commit -m "feat(remnant): add source mob type and render scale synced data fields"
```

---

## Task 2: Stage-Relative Damage Scaling

**Files:**
- Modify: `src/main/java/com/cradle/mod/entity/RemnantEntity.java`

**Context:** Currently damage is a flat `3.0 + 1.5 * powerLevel` stored as an attribute. The new system calculates damage dynamically based on the gap between remnant power and victim stage: `2.0 + (gap * 4.5)` when remnant is stronger, `max(1.0, 2.0 + (gap * 0.5))` when victim is stronger.

**Step 1: Override doHurtTarget to apply stage-relative damage**

Add this method to RemnantEntity (after the `hurtServer` method around line 345):

```java
@Override
public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
    // Calculate stage-relative damage
    float damage = calculateStageDamage(target);

    // Apply damage directly instead of using attribute
    boolean hit = target.hurtOrSimulate(this.damageSources().mobAttack(this), damage);
    if (hit && target instanceof LivingEntity living) {
        // Apply knockback based on power level
        float knockback = 0.5f + (getPowerLevel() * 0.1f);
        living.knockback(knockback, Math.sin(this.getYRot() * Math.PI / 180.0),
                -Math.cos(this.getYRot() * Math.PI / 180.0));
    }
    return hit;
}

/**
 * Calculates damage relative to the gap between this remnant's power level
 * and the target's advancement stage. Wider gaps = more damage.
 */
private float calculateStageDamage(net.minecraft.world.entity.Entity target) {
    int victimStage = 0; // Default: treat non-players as Foundation

    if (target instanceof ServerPlayer player) {
        CradlePlayerData data = CradlePlayerData.get(player.getUUID());
        if (data != null) {
            victimStage = data.getAdvancementStage().ordinal();
        }
    }

    int stageGap = getPowerLevel() - victimStage;

    if (stageGap >= 0) {
        // Remnant is same stage or stronger
        return 2.0f + (stageGap * 4.5f);
    } else {
        // Victim is stronger — remnant can always do at least 0.5 hearts
        return Math.max(1.0f, 2.0f + (stageGap * 0.5f));
    }
}
```

**Step 2: Add the Entity import**

Ensure `net.minecraft.world.entity.Entity` is accessible (it should be via the existing `LivingEntity` import, but `doHurtTarget` takes `Entity`). Check if `ServerLevel` import is already present (it is at line 11).

**Step 3: Build and verify**

Run: `./gradlew build`
Expected: Build succeeds.

**Step 4: Commit**

```bash
git add src/main/java/com/cradle/mod/entity/RemnantEntity.java
git commit -m "feat(remnant): stage-relative damage scaling based on power gap"
```

---

## Task 3: Update RemnantManager to Pass Source Mob Type and Copy Loadout

**Files:**
- Modify: `src/main/java/com/cradle/mod/RemnantManager.java`
- Modify: `src/main/java/com/cradle/mod/entity/RemnantEntity.java`

**Context:** When a mob dies, we need to record which mob type it was. When a player dies, we also need to copy their ability loadout to the remnant. The mob's registry ID (e.g. `"minecraft:spider"`) is available from `entity.getType().builtInRegistryHolder().key().location().toString()`.

**Step 1: Add loadout storage to RemnantEntity**

Add new server-only fields to RemnantEntity (after the absorption channeling state block, ~line 54):

```java
// ── Ability AI state (player remnants only) ──────────────────
private com.cradle.mod.ability.PlayerLoadout storedLoadout = null;
private float madraPool = 0f;
private float maxMadraPool = 0f;
```

Add getters/setters:

```java
public com.cradle.mod.ability.PlayerLoadout getStoredLoadout() { return storedLoadout; }
public void setStoredLoadout(com.cradle.mod.ability.PlayerLoadout loadout) { this.storedLoadout = loadout; }
public float getMadraPool() { return madraPool; }
public void setMadraPool(float madra) { this.madraPool = Math.max(0, madra); }
public float getMaxMadraPool() { return maxMadraPool; }
public void setMaxMadraPool(float max) { this.maxMadraPool = max; }
```

Update NBT save to persist loadout and madra:

In `addAdditionalSaveData`, add:

```java
if (storedLoadout != null) {
    output.store("StoredLoadout", storedLoadout.toNbt());
}
output.putFloat("MadraPool", madraPool);
output.putFloat("MaxMadraPool", maxMadraPool);
```

In `readAdditionalSaveData`, add:

```java
madraPool = input.getFloatOr("MadraPool", 0f);
maxMadraPool = input.getFloatOr("MaxMadraPool", 0f);
// Loadout restore requires reading a CompoundTag — use input.read() if available,
// or check how other NBT compounds are loaded in this codebase
```

**Note:** The exact NBT compound read API may vary — check how `CradlePlayerData.fromNbt` loads the loadout compound. The ValueInput/ValueOutput API used here may differ from the raw CompoundTag used by PlayerLoadout. The implementer should verify this and adapt. If ValueInput doesn't support reading compound tags directly, you may need to use `input.read("StoredLoadout", CompoundTag.CODEC)` or similar pattern from this codebase.

**Step 2: Update RemnantManager.trySpawnPlayerRemnant**

In `trySpawnPlayerRemnant` (line 45), after creating the remnant entity, pass `"cradlemod:player"` as the source mob type and copy the player's loadout:

```java
// Replace the initRemnant call (line 79):
remnant.initRemnant(data.getChosenPath(), powerLevel, player.getUUID(), "cradlemod:player");

// Copy player's loadout to remnant
remnant.setStoredLoadout(data.getLoadout());
remnant.setMaxMadraPool(data.getMaxMadra());
remnant.setMadraPool(data.getMaxMadra()); // Remnant starts with full madra
```

**Step 3: Update RemnantManager.trySpawnMobRemnant**

In `trySpawnMobRemnant` (line 93), pass the mob's registry key as source type:

```java
// Get the mob's registry name (e.g. "minecraft:spider")
String mobType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
        .getKey(entity.getType()).toString();

// Replace the initRemnant call (line 118):
remnant.initRemnant(remnantPath, powerLevel, null, mobType);
```

Add the import at the top of RemnantManager.java:

```java
import net.minecraft.core.registries.BuiltInRegistries;
```

**Step 4: Build and verify**

Run: `./gradlew build`
Expected: Build succeeds.

**Step 5: Commit**

```bash
git add src/main/java/com/cradle/mod/entity/RemnantEntity.java \
        src/main/java/com/cradle/mod/RemnantManager.java
git commit -m "feat(remnant): pass source mob type on spawn, copy player loadout"
```

---

## Task 4: Remnant Ability AI

**Files:**
- Create: `src/main/java/com/cradle/mod/entity/RemnantAbilityAI.java`
- Modify: `src/main/java/com/cradle/mod/entity/RemnantEntity.java`

**Context:** Remnants from players should use abilities from the dead player's loadout. The number of abilities scales with stage (Copper-Iron: 0, Jade-Lowgold: 1, ... Monarch: 6). AI picks abilities based on distance: Enforcer close, Striker mid, Ruler far. Abilities consume from the remnant's madra pool and respect cooldowns.

**Step 1: Create RemnantAbilityAI.java**

```java
package com.cradle.mod.entity;

import com.cradle.mod.CradlePlayerData;
import com.cradle.mod.ability.AbilityDefinition;
import com.cradle.mod.ability.AbilityRegistry;
import com.cradle.mod.ability.AbilityType;
import com.cradle.mod.ability.PlayerLoadout;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Range-based tactical AI for remnant ability usage.
 * Picks abilities based on distance to target:
 * - Close (< 4 blocks): Enforcer
 * - Mid (4-10 blocks): Striker
 * - Long (10+ blocks): Ruler
 */
public final class RemnantAbilityAI {

    private static final int DECISION_INTERVAL = 20; // Re-evaluate every 1 second
    private static final double CLOSE_RANGE = 4.0;
    private static final double MID_RANGE = 10.0;

    private final RemnantEntity remnant;
    private final Map<Integer, Long> cooldowns = new HashMap<>(); // slot -> last use tick
    private final Set<Integer> activeToggleSlots = new HashSet<>(); // enforcer/ruler active slots
    private int decisionTicks = 0;
    private int abilityCount;

    public RemnantAbilityAI(RemnantEntity remnant) {
        this.remnant = remnant;
        this.abilityCount = getAbilityCountForPower(remnant.getPowerLevel());
    }

    /**
     * Called every server tick. Handles ability decisions and active ability effects.
     */
    public void tick(ServerLevel level) {
        PlayerLoadout loadout = remnant.getStoredLoadout();
        if (loadout == null || abilityCount <= 0) return;

        LivingEntity target = remnant.getTarget();
        if (target == null || !target.isAlive()) {
            deactivateAll(level);
            return;
        }

        // Tick active enforcers/rulers (drain madra)
        tickActiveAbilities(level);

        // Decision tick: pick an ability to use
        decisionTicks++;
        if (decisionTicks >= DECISION_INTERVAL) {
            decisionTicks = 0;
            makeAbilityDecision(level, target);
        }
    }

    private void makeAbilityDecision(ServerLevel level, LivingEntity target) {
        PlayerLoadout loadout = remnant.getStoredLoadout();
        if (loadout == null) return;

        double distance = remnant.distanceTo(target);

        // Determine preferred type based on range
        AbilityType preferred;
        if (distance < CLOSE_RANGE) {
            preferred = AbilityType.ENFORCER;
        } else if (distance < MID_RANGE) {
            preferred = AbilityType.STRIKER;
        } else {
            preferred = AbilityType.RULER;
        }

        // Build list of usable abilities (up to abilityCount), preferring the right type
        List<int[]> candidates = new ArrayList<>(); // [slot, priority]
        for (int slot = 0; slot < Math.min(PlayerLoadout.MAX_SLOTS, abilityCount); slot++) {
            if (!loadout.hasAbility(slot)) continue;
            AbilityDefinition def = AbilityRegistry.get(loadout.getAbility(slot));
            if (def == null) continue;
            if (isOnCooldown(slot, def, loadout.getUpgradeLevel(slot))) continue;

            int priority = (def.getType() == preferred) ? 0 : 1;
            candidates.add(new int[]{slot, priority});
        }

        // Sort by priority (preferred type first)
        candidates.sort(Comparator.comparingInt(a -> a[1]));

        // Try to use the best candidate
        for (int[] candidate : candidates) {
            int slot = candidate[0];
            if (tryUseAbility(level, slot, target)) {
                break; // Used one ability this decision tick
            }
        }
    }

    private boolean tryUseAbility(ServerLevel level, int slot, LivingEntity target) {
        PlayerLoadout loadout = remnant.getStoredLoadout();
        String abilityId = loadout.getAbility(slot);
        AbilityDefinition def = AbilityRegistry.get(abilityId);
        if (def == null) return false;

        int upgradeLevel = loadout.getUpgradeLevel(slot);
        float cost = def.getScaledMadraCost(upgradeLevel);

        if (remnant.getMadraPool() < cost) return false;

        switch (def.getType()) {
            case STRIKER -> {
                // Fire and forget: deduct madra, apply cooldown
                remnant.setMadraPool(remnant.getMadraPool() - cost);
                setCooldown(slot, def, upgradeLevel);
                // Look at target before firing
                remnant.getLookControl().setLookAt(target);
                if (def.getStrikerFire() != null) {
                    // Striker handlers expect ServerPlayer — we need to adapt.
                    // For now, skip handlers that require player context.
                    // The full implementation will need entity-aware handler overloads.
                }
                return true;
            }
            case ENFORCER -> {
                if (!activeToggleSlots.contains(slot)) {
                    // Activate
                    activeToggleSlots.add(slot);
                    return true;
                }
                return false;
            }
            case RULER -> {
                if (!activeToggleSlots.contains(slot)) {
                    activeToggleSlots.add(slot);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private void tickActiveAbilities(ServerLevel level) {
        PlayerLoadout loadout = remnant.getStoredLoadout();
        if (loadout == null) return;

        Iterator<Integer> iter = activeToggleSlots.iterator();
        while (iter.hasNext()) {
            int slot = iter.next();
            AbilityDefinition def = AbilityRegistry.get(loadout.getAbility(slot));
            if (def == null) { iter.remove(); continue; }

            int upgradeLevel = loadout.getUpgradeLevel(slot);
            float drain = def.getScaledMadraCost(upgradeLevel);
            remnant.setMadraPool(remnant.getMadraPool() - drain);

            if (remnant.getMadraPool() <= 0) {
                iter.remove();
                continue;
            }

            // Apply area effects for rulers every 10 ticks
            if (def.getType() == AbilityType.RULER && def.getRulerArea() != null) {
                // Ruler area effects need entity-aware handlers (future task)
            }
        }
    }

    private void deactivateAll(ServerLevel level) {
        activeToggleSlots.clear();
    }

    private boolean isOnCooldown(int slot, AbilityDefinition def, int upgradeLevel) {
        Long lastUse = cooldowns.get(slot);
        if (lastUse == null) return false;
        long cooldownTicks = def.getScaledCooldownMs(upgradeLevel) / 50; // ms to ticks
        return (remnant.tickCount - lastUse) < cooldownTicks;
    }

    private void setCooldown(int slot, AbilityDefinition def, int upgradeLevel) {
        cooldowns.put(slot, (long) remnant.tickCount);
    }

    /**
     * Returns the number of abilities a remnant can use based on its power level.
     * Maps stage ranges to ability counts.
     */
    private static int getAbilityCountForPower(int powerLevel) {
        // Power levels map to AdvancementStage ordinals:
        // 1=COPPER, 2=IRON, 3=JADE, 4=LOW_GOLD, 5=HIGH_GOLD, 6=TRUE_GOLD,
        // 7=UNDERLORD, 8=OVERLORD, 9=ARCHLORD, 10=SAGE, 11=HERALD, 12=MONARCH
        if (powerLevel <= 2) return 0;  // Copper-Iron: melee only
        if (powerLevel <= 4) return 1;  // Jade-Lowgold: 1 ability
        if (powerLevel <= 6) return 2;  // Highgold-Truegold: 2
        if (powerLevel <= 8) return 3;  // Underlord-Overlord: 3
        if (powerLevel <= 10) return 4; // Archlord-Sage: 4
        if (powerLevel == 11) return 5; // Herald: 5
        return 6;                       // Monarch: full loadout
    }
}
```

**Step 2: Wire up the AI in RemnantEntity.tick()**

In `RemnantEntity`, add a field:

```java
private RemnantAbilityAI abilityAI = null;
```

In `initRemnant` (the 4-param version), after setting source mob type, initialize the AI if the remnant has a loadout:

```java
// Initialize ability AI (will be populated after setStoredLoadout is called)
```

Add a lazy init in `tick()`, in the server-side block (after `tickAbsorption()`, line 158):

```java
// Tick ability AI for player remnants
if (storedLoadout != null && level() instanceof ServerLevel serverLevel) {
    if (abilityAI == null) {
        abilityAI = new RemnantAbilityAI(this);
    }
    abilityAI.tick(serverLevel);
}
```

**Step 3: Build and verify**

Run: `./gradlew build`
Expected: Build succeeds.

**Step 4: Commit**

```bash
git add src/main/java/com/cradle/mod/entity/RemnantAbilityAI.java \
        src/main/java/com/cradle/mod/entity/RemnantEntity.java
git commit -m "feat(remnant): add range-based ability AI with stage-scaled ability count"
```

---

## Task 5: Rewrite RemnantRenderer for Dynamic Mob Models

**Files:**
- Modify: `src/client/java/com/cradle/mod/entity/RemnantRenderer.java`
- Modify: `src/client/java/com/cradle/mod/entity/RemnantRenderState.java`

**Context:** The renderer currently uses a fixed HumanoidModel. It needs to dynamically resolve the source mob's model and render it with a translucent path-color tint and eye overlay. This is the most complex task — Minecraft's renderer system couples entity types to their renderers tightly.

**Important Minecraft rendering constraint:** Each entity type has one registered renderer. We can't easily "borrow" another mob's renderer at runtime. The practical approach is to render the mob's model directly using the model layer system.

**Approach:** For this initial implementation, we'll use a lookup table mapping common mob types to their ModelLayerLocation + model constructor. The renderer creates and caches models for each mob type encountered. Unsupported mob types fall back to the humanoid model.

**Step 1: Update RemnantRenderState**

Add new fields to `RemnantRenderState.java`:

```java
public String sourceMobType = "minecraft:zombie";
public float renderScale = 1.0f;
```

**Step 2: Update extractRenderState in RemnantRenderer**

Add to the `extractRenderState` method:

```java
state.sourceMobType = entity.getSourceMobType();
state.renderScale = entity.getRenderScale();
```

**Step 3: Rewrite RemnantRenderer**

The full rewrite approach: keep extending `HumanoidMobRenderer` (since the entity registration expects this), but override the `render` method to apply scaling. The model swap (using different mob models) is a complex undertaking that requires caching model instances per mob type.

For the initial implementation, focus on:
1. **Scale support** via `setupRotations` or model matrix manipulation
2. **Eye overlay** as a secondary render layer
3. Model swapping will be a follow-up task if needed — the translucent humanoid with proper coloring and glowing eyes already looks distinct

```java
@Override
protected void setupRotations(RemnantRenderState state, PoseStack poseStack,
                               float bodyYRot, float scale) {
    super.setupRotations(state, poseStack, bodyYRot, scale);
    // Apply render scale for Herald 2x remnant
    if (state.renderScale != 1.0f) {
        poseStack.scale(state.renderScale, state.renderScale, state.renderScale);
    }
}
```

**Note for implementer:** Look at `setupRotations` in the actual Minecraft source — the method signature may differ in 1.21.11. Check `MobRenderer` or `LivingEntityRenderer` for the exact override point. If `setupRotations` doesn't exist, use `render()` and push/pop the PoseStack with scale before calling `super.render()`.

**Step 4: Add eye overlay render layer**

Create a new render layer class or add inline rendering in the renderer that draws the eye overlay texture on top of the base model. The eye overlay uses the same UV mapping as the base texture but is rendered with emissive lighting (fullbright).

The implementer should:
1. Check if `RenderType.eyes()` or a custom emissive `RenderType` is available in 1.21.11
2. Use `RenderType.eyes(eyeTexture)` to render the overlay with full brightness
3. The eye texture path: `cradlemod:textures/entity/remnant_eyes/zombie.png` (etc.)

**Step 5: Build and verify**

Run: `./gradlew build`
Expected: Build succeeds.

**Step 6: In-game test**

Run: `./gradlew runClient`
- Use `/cycle stage monarch` then die to spawn a Monarch remnant
- Verify: remnant renders with translucent tint
- Verify: Herald trial remnant (if triggered) appears at 2x scale
- Check that the eye overlay renders if textures have been added (Task 7)

**Step 7: Commit**

```bash
git add src/client/java/com/cradle/mod/entity/RemnantRenderer.java \
        src/client/java/com/cradle/mod/entity/RemnantRenderState.java
git commit -m "feat(remnant): renderer with scale support and eye overlay layer"
```

---

## Task 6: Herald Trial Rework

**Files:**
- Modify: `src/main/java/com/cradle/mod/RevelationTrialManager.java`

**Context:** Replace the Iron Golem boss in `startHeraldTrial()` with a proper 2x-scaled RemnantEntity that copies the player's loadout. Death count scaling adds +25 HP and +10% madra per prior death.

**Step 1: Rewrite startHeraldTrial()**

Replace the Iron Golem creation block (lines 178-216) with RemnantEntity creation:

```java
public static void startHeraldTrial(ServerPlayer player, CradlePlayerData data) {
    UUID playerId = player.getUUID();

    if (isInTrial(playerId)) {
        player.displayClientMessage(Component.literal(
                "\u00A7cYou are already undergoing a trial!"), true);
        return;
    }

    if (!(player.level() instanceof ServerLevel serverLevel)) return;

    RevelationTrial trial = new RevelationTrial(
            playerId, CradlePlayerData.AdvancementStage.HERALD, true,
            player.getX(), player.getY(), player.getZ()
    );

    // Spawn the Remnant boss 5 blocks in front of the player
    Vec3 look = player.getLookAngle();
    double bossX = player.getX() + look.x * 5.0;
    double bossZ = player.getZ() + look.z * 5.0;
    double bossY = player.getY();

    RemnantEntity remnant = new RemnantEntity(CradleEntities.REMNANT, serverLevel);
    remnant.setPos(bossX, bossY, bossZ);

    // Death count scaling
    int deathCount = data.getRemnantDeathCount();
    float baseHealth = 200.0f + (deathCount * 25.0f);
    float madraMultiplier = 1.5f + (deathCount * 0.1f);

    // Initialize as a player-type remnant with full Monarch power
    remnant.initRemnant(data.getChosenPath(), 12, player.getUUID(), "cradlemod:player");
    remnant.setRenderScale(2.0f);

    // Override health for boss fight
    remnant.getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHealth);
    remnant.setHealth(baseHealth);

    // Copy player's full loadout
    remnant.setStoredLoadout(data.getLoadout());
    remnant.setMaxMadraPool(data.getMaxMadra() * madraMultiplier);
    remnant.setMadraPool(data.getMaxMadra() * madraMultiplier);

    // Custom name
    remnant.setCustomName(Component.literal(
            "\u00A7d" + player.getName().getString() + "'s Remnant"));
    remnant.setCustomNameVisible(true);

    // Effects: Glowing
    remnant.addEffect(new MobEffectInstance(MobEffects.GLOWING, 999999, 0, false, false));

    // Target the player
    remnant.setTarget(player);

    serverLevel.addFreshEntity(remnant);
    trial.spiritIds.add(remnant.getUUID());

    ACTIVE_TRIALS.put(playerId, trial);

    // Lore messages
    player.sendSystemMessage(Component.literal(
            "\u00A76[Cradle] \u00A7d\u2694 Your Remnant materializes before you. Your spirit made flesh \u2014 it mirrors your every strength."));
    player.sendSystemMessage(Component.literal(
            "\u00A76[Cradle] \u00A7d\u00A7oDefeat it to merge body and spirit. Fail, and you will be consumed."));

    CradleMod.LOGGER.info("Player {} started Herald Remnant trial (RemnantEntity boss, {} HP, {}x madra)",
            player.getName().getString(), baseHealth, madraMultiplier);
}
```

**Step 2: Add required imports**

At the top of `RevelationTrialManager.java`, add:

```java
import com.cradle.mod.entity.CradleEntities;
import com.cradle.mod.entity.RemnantEntity;
```

Remove or keep the Iron Golem import as unused (clean up if no longer needed).

**Step 3: Build and verify**

Run: `./gradlew build`
Expected: Build succeeds.

**Step 4: In-game test**

Run: `./gradlew runClient`
- Use `/cycle stage sage` and `/cycle sage true` to set up a Sage player
- Trigger Herald trial via the breakthrough system
- Verify: A 2x scaled RemnantEntity spawns instead of an Iron Golem
- Verify: Boss has the custom name and glowing effect
- Verify: Killing the boss completes the trial and advances to Herald/Monarch

**Step 5: Commit**

```bash
git add src/main/java/com/cradle/mod/RevelationTrialManager.java
git commit -m "feat(remnant): rework Herald trial to use 2x RemnantEntity with death count scaling"
```

---

## Task 7: Create Eye Overlay Textures

**Files:**
- Create: `src/main/resources/assets/cradlemod/textures/entity/remnant_eyes/*.png`

**Context:** Each supported mob type needs a transparent PNG where only the eye pixels are filled in with white. The texture uses the same UV layout as the vanilla mob texture. These render as an emissive overlay on top of the ghostly remnant model.

**Step 1: Create the texture directory**

```bash
mkdir -p src/main/resources/assets/cradlemod/textures/entity/remnant_eyes
```

**Step 2: Create eye textures programmatically**

Write a simple script or use the Minecraft texture coordinates to create transparent PNGs with white pixels at the eye positions for each mob. The textures should be the same dimensions as the vanilla mob textures.

**Supported mobs (initial set):**
1. `zombie.png` — 64x64, eyes at face UV (8,8 to 12,12 area)
2. `skeleton.png` — 64x32, eyes at face UV
3. `spider.png` — 64x32, 8 eye pattern
4. `creeper.png` — 64x32, dark pixel eyes
5. `enderman.png` — 64x32, purple eyes
6. `player.png` — 64x64, Steve-style eyes (for player remnants)

**Note:** The exact UV coordinates for each mob's eyes need to be looked up from the vanilla textures. The implementer should:
1. Open each vanilla mob texture
2. Identify the eye pixel coordinates
3. Create a same-sized transparent PNG with only those pixels filled white
4. Save to the remnant_eyes directory

**Step 3: Commit**

```bash
git add src/main/resources/assets/cradlemod/textures/entity/remnant_eyes/
git commit -m "feat(remnant): add eye overlay textures for supported mob types"
```

---

## Task 8: Update CradleEntities for Dynamic Sizing

**Files:**
- Modify: `src/main/java/com/cradle/mod/entity/CradleEntities.java`
- Modify: `src/main/java/com/cradle/mod/entity/RemnantEntity.java`

**Context:** The remnant entity is currently registered with fixed dimensions (0.6f x 1.8f). For mob-shaped remnants, the hitbox should match the source mob. Since entity type dimensions are immutable after registration, we need to handle this by overriding `getDimensions()` on the entity instance based on the source mob type.

**Step 1: Override getDimensions in RemnantEntity**

Add a dimension lookup and override:

```java
@Override
public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
    float scale = getRenderScale();
    net.minecraft.world.entity.EntityDimensions base = getMobDimensions(getSourceMobType());
    if (scale != 1.0f) {
        return base.scale(scale);
    }
    return base;
}

private static net.minecraft.world.entity.EntityDimensions getMobDimensions(String mobType) {
    return switch (mobType) {
        case "minecraft:spider" -> net.minecraft.world.entity.EntityDimensions.scalable(1.4f, 0.9f);
        case "minecraft:enderman" -> net.minecraft.world.entity.EntityDimensions.scalable(0.6f, 2.9f);
        case "minecraft:creeper" -> net.minecraft.world.entity.EntityDimensions.scalable(0.6f, 1.7f);
        case "minecraft:skeleton" -> net.minecraft.world.entity.EntityDimensions.scalable(0.6f, 1.99f);
        case "minecraft:blaze" -> net.minecraft.world.entity.EntityDimensions.scalable(0.6f, 1.8f);
        case "minecraft:guardian" -> net.minecraft.world.entity.EntityDimensions.scalable(0.85f, 0.85f);
        case "minecraft:phantom" -> net.minecraft.world.entity.EntityDimensions.scalable(0.9f, 0.5f);
        case "minecraft:warden" -> net.minecraft.world.entity.EntityDimensions.scalable(0.9f, 2.9f);
        case "minecraft:ravager" -> net.minecraft.world.entity.EntityDimensions.scalable(1.95f, 2.2f);
        case "minecraft:iron_golem" -> net.minecraft.world.entity.EntityDimensions.scalable(1.4f, 2.7f);
        case "minecraft:cow", "minecraft:pig", "minecraft:sheep" ->
                net.minecraft.world.entity.EntityDimensions.scalable(0.9f, 1.4f);
        case "minecraft:chicken" -> net.minecraft.world.entity.EntityDimensions.scalable(0.4f, 0.7f);
        case "minecraft:wolf" -> net.minecraft.world.entity.EntityDimensions.scalable(0.6f, 0.85f);
        case "minecraft:horse" -> net.minecraft.world.entity.EntityDimensions.scalable(1.4f, 1.6f);
        default -> net.minecraft.world.entity.EntityDimensions.scalable(0.6f, 1.8f); // humanoid default
    };
}
```

**Note for implementer:** Check if `getDefaultDimensions(Pose)` is the right override point in 1.21.11. The method name may be `getDimensions(Pose)` depending on the mapping version. Also verify that `EntityDimensions.scalable()` exists — it may be `EntityDimensions.fixed()` or constructed differently.

**Step 2: Refresh dimensions when source mob type changes**

In `setSourceMobType()`, call `refreshDimensions()`:

```java
public void setSourceMobType(String mobType) {
    this.entityData.set(SOURCE_MOB_TYPE, mobType != null ? mobType : "minecraft:zombie");
    refreshDimensions();
}
```

**Step 3: Build and verify**

Run: `./gradlew build`
Expected: Build succeeds.

**Step 4: Commit**

```bash
git add src/main/java/com/cradle/mod/entity/RemnantEntity.java
git commit -m "feat(remnant): dynamic hitbox dimensions based on source mob type"
```

---

## Task 9: Integration Testing and Polish

**Files:**
- All modified files

**Step 1: Full build**

Run: `./gradlew clean build`
Expected: Build succeeds with no errors.

**Step 2: In-game integration test checklist**

Run: `./gradlew runClient`

Test each scenario:

1. **Mob remnant visuals:** Kill a mob, check that the remnant spawns (15% chance). Verify it has the translucent path-colored tint.
2. **Player remnant:** Use `/cycle stage jade` then `/cycle path BLACK_FLAME`, die. Verify the remnant spawns with source type `cradlemod:player`.
3. **Damage scaling:** Use `/cycle stage copper` and get hit by a high-power remnant. Verify high damage. Use `/cycle stage monarch` and verify low damage from same remnant.
4. **Herald trial:** Set up a Sage player, trigger Herald breakthrough. Verify a 2x scaled RemnantEntity spawns (not Iron Golem). Kill it and verify advancement.
5. **Ability AI:** Spawn a Monarch-power player remnant. Observe if it uses abilities when attacking (madra draining, ability effects). This requires the ability handlers to be adapted for entity context (may need follow-up work).
6. **NBT persistence:** Spawn a remnant, save and reload the world, verify it still has the correct source mob type and render scale.

**Step 3: Fix any issues found during testing**

Address compile errors, rendering glitches, or logic bugs discovered during manual testing.

**Step 4: Final commit**

```bash
git add -A
git commit -m "feat(remnant): complete remnant overhaul with mob-shaped visuals, damage scaling, ability AI, and Herald rework"
```

---

## Implementation Notes

### Known Limitations / Follow-up Work

1. **Ability handler adaptation:** The current `AbilityHandlers` interfaces all take `ServerPlayer` as the first argument. For remnants to actually fire abilities (striker projectiles, enforcer buffs, ruler areas), the handlers need entity-aware overloads or an adapter that wraps the RemnantEntity to look like a player. This is a significant refactor of `AbilityExecutor` and `AbilityHandlers` — Task 4 sets up the AI framework but the actual ability effects won't fire until this adapter is built.

2. **Dynamic mob model rendering:** Task 5 adds scale support but doesn't fully implement the "render the spider model for a spider remnant" feature. This requires either:
   - Caching and rendering other mob type models within the RemnantRenderer (complex but correct)
   - Or using a mixin to intercept entity rendering and swap the model (hacky)

   The initial implementation uses the humanoid model for all remnants with correct scaling and eye overlay. Full model swapping is a follow-up.

3. **Sacred Beasts:** Deferred to a separate design and implementation cycle.

4. **Eye texture creation:** The plan describes the structure but actual pixel art for ~20 mob eye textures needs to be created. This can be done programmatically by sampling vanilla textures or manually in an image editor.

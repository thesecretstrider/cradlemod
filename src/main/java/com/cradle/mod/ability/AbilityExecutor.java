package com.cradle.mod.ability;

import com.cradle.mod.CradleMod;
import com.cradle.mod.CradlePlayerData;
import com.cradle.mod.CyclingManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central server-side ability execution engine.
 * Replaces the old hardcoded UseEnforcer/UseStriker/UseRuler handlers in CradleMod.
 * Looks up the ability from the player's loadout and delegates to its behavior lambdas.
 */
public final class AbilityExecutor {

	private AbilityExecutor() {}

	// Cooldown tracking: "playerUUID:slot" -> last use timestamp in ms
	private static final Map<String, Long> COOLDOWNS = new HashMap<>();

	// Ruler effect interval (ticks) — same as old CyclingManager constant
	private static final int RULER_EFFECT_INTERVAL = 10;
	private static final Map<String, Integer> RULER_EFFECT_TICKS = new HashMap<>();

	// Enforcer madra drain base rates — can be overridden per ability
	private static final float DEFAULT_ENFORCER_DRAIN = 0.5f;
	private static final float DEFAULT_RULER_DRAIN = 0.4f;

	// Hollow King enforcer madra regen offset
	private static final float HOLLOW_REGEN_BONUS = 0.3f;

	/**
	 * Called when UseAbilityPayload is received from client.
	 * Looks up the ability in the player's loadout slot and executes it.
	 */
	public static void handleUseAbility(ServerPlayer player, int slot) {
		CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
		PlayerLoadout loadout = data.getLoadout();

		if (!loadout.hasAbility(slot)) {
			return; // Empty slot
		}

		String abilityId = loadout.getAbility(slot);
		AbilityDefinition def = AbilityRegistry.get(abilityId);
		if (def == null) {
			CradleMod.LOGGER.warn("Unknown ability in slot {}: {}", slot, abilityId);
			return;
		}

		// Check if player has chosen a path (universal abilities are ok without path)
		if (!def.isUniversal() && !data.hasChosenPath()) {
			player.displayClientMessage(Component.literal("\u00a7cChoose a path first!"), true);
			return;
		}

		// Check minimum stage
		if (data.getAdvancementStage().ordinal() < def.getUnlockStage().ordinal()) {
			player.displayClientMessage(Component.literal(
				"\u00a7cRequires " + def.getUnlockStage().displayName() + " to use " + def.getDisplayName()), true);
			return;
		}

		int upgradeLevel = loadout.getUpgradeLevel(slot);

		switch (def.getType()) {
			case ENFORCER -> handleEnforcer(player, data, def, upgradeLevel, slot, loadout);
			case STRIKER -> handleStriker(player, data, def, upgradeLevel, slot, loadout);
			case RULER -> handleRuler(player, data, def, upgradeLevel, slot, loadout);
		}
	}

	// ── Enforcer toggle ───────────────────────────────────────────────

	private static void handleEnforcer(ServerPlayer player, CradlePlayerData data,
	                                    AbilityDefinition def, int upgradeLevel,
	                                    int slot, PlayerLoadout loadout) {
		if (loadout.isSlotActive(slot)) {
			// Deactivate
			loadout.setSlotActive(slot, false);
			if (def.getEnforcerDeactivate() != null) {
				def.getEnforcerDeactivate().deactivate(player, data, def, upgradeLevel);
			}
		} else {
			// Check madra
			if (data.getCurrentMadra() <= 0) {
				player.displayClientMessage(Component.literal("\u00a7cNot enough Madra!"), true);
				return;
			}

			// Deactivate any other active enforcer in a different slot
			deactivateOtherToggleSlots(player, data, loadout, slot, AbilityType.ENFORCER);

			// Disrupt cycling if below Underlord
			disruptCyclingIfNeeded(player, data);

			loadout.setSlotActive(slot, true);

			if (def.getEnforcerActivate() != null) {
				def.getEnforcerActivate().activate(player, data, def, upgradeLevel);
			}
		}
	}

	// ── Striker fire ──────────────────────────────────────────────────

	private static void handleStriker(ServerPlayer player, CradlePlayerData data,
	                                   AbilityDefinition def, int upgradeLevel,
	                                   int slot, PlayerLoadout loadout) {
		// Check cooldown
		String cooldownKey = player.getUUID().toString() + ":" + slot;
		long now = System.currentTimeMillis();
		long lastUse = COOLDOWNS.getOrDefault(cooldownKey, 0L);
		long cooldownMs = def.getScaledCooldownMs(upgradeLevel);
		long effectiveCooldown = (long) (cooldownMs * data.getCooldownMultiplier());

		if (now - lastUse < effectiveCooldown) {
			long remaining = (effectiveCooldown - (now - lastUse)) / 1000;
			player.displayClientMessage(Component.literal(
				"\u00a7c" + def.getDisplayName() + " on cooldown (" + remaining + "s)"), true);
			return;
		}

		// Check madra cost
		float cost = def.getScaledMadraCost(upgradeLevel) * data.getMadraCostMultiplier();
		if (data.getCurrentMadra() < cost) {
			player.displayClientMessage(Component.literal("\u00a7cNot enough Madra!"), true);
			return;
		}

		// Disrupt cycling if below Underlord
		disruptCyclingIfNeeded(player, data);

		// Deduct madra
		data.setCurrentMadra(data.getCurrentMadra() - cost);

		// Fire the ability
		if (def.getStrikerFire() != null) {
			def.getStrikerFire().fire(player, data, def, upgradeLevel);
		}

		// Set cooldown
		COOLDOWNS.put(cooldownKey, now);

		// Display message
		player.displayClientMessage(Component.literal("\u00a76" + def.getDisplayName() + "!"), true);
	}

	// ── Ruler toggle ──────────────────────────────────────────────────

	private static void handleRuler(ServerPlayer player, CradlePlayerData data,
	                                 AbilityDefinition def, int upgradeLevel,
	                                 int slot, PlayerLoadout loadout) {
		if (loadout.isSlotActive(slot)) {
			// Deactivate
			loadout.setSlotActive(slot, false);
			player.displayClientMessage(Component.literal(
				"\u00a7b" + def.getDisplayName() + " \u00a7cdeactivated"), true);
		} else {
			// Check madra
			if (data.getCurrentMadra() <= 0) {
				player.displayClientMessage(Component.literal("\u00a7cNot enough Madra!"), true);
				return;
			}

			// Deactivate any other active ruler in a different slot
			deactivateOtherToggleSlots(player, data, loadout, slot, AbilityType.RULER);

			// Disrupt cycling if below Underlord
			disruptCyclingIfNeeded(player, data);

			loadout.setSlotActive(slot, true);

			player.displayClientMessage(Component.literal(
				"\u00a7b" + def.getDisplayName() + " \u00a7aactivated"), true);
		}
	}

	// ── Tick handler (called from CyclingManager) ─────────────────────

	/**
	 * Called every server tick for each player.
	 * Processes active enforcer/ruler abilities in the loadout.
	 */
	public static void tickPlayerAbilities(ServerPlayer player, CradlePlayerData data) {
		PlayerLoadout loadout = data.getLoadout();

		for (int slot = 0; slot < PlayerLoadout.MAX_SLOTS; slot++) {
			if (!loadout.isSlotActive(slot)) continue;

			String abilityId = loadout.getAbility(slot);
			if (abilityId == null) continue;

			AbilityDefinition def = AbilityRegistry.get(abilityId);
			if (def == null) {
				loadout.setSlotActive(slot, false);
				continue;
			}

			int upgradeLevel = loadout.getUpgradeLevel(slot);

			switch (def.getType()) {
				case ENFORCER -> tickEnforcer(player, data, def, upgradeLevel, slot, loadout);
				case RULER -> tickRuler(player, data, def, upgradeLevel, slot, loadout);
				default -> {} // Strikers don't tick
			}
		}
	}

	private static void tickEnforcer(ServerPlayer player, CradlePlayerData data,
	                                   AbilityDefinition def, int upgradeLevel,
	                                   int slot, PlayerLoadout loadout) {
		// Madra drain (scaled by ability + stage)
		float drain = def.getScaledMadraCost(upgradeLevel) * data.getMadraCostMultiplier();

		// Hollow King gets regen bonus that offsets drain
		if ("hollow_circulation".equals(def.getId())) {
			drain -= HOLLOW_REGEN_BONUS;
		}

		if (drain > 0) {
			data.setCurrentMadra(data.getCurrentMadra() - drain);
		}

		if (data.getCurrentMadra() <= 0) {
			// Out of madra — deactivate
			loadout.setSlotActive(slot, false);
			if (def.getEnforcerDeactivate() != null) {
				def.getEnforcerDeactivate().deactivate(player, data, def, upgradeLevel);
			}
			player.displayClientMessage(Component.literal("\u00a7cOut of Madra!"), true);
			return;
		}

		// Apply tick effects
		if (def.getEnforcerTick() != null) {
			def.getEnforcerTick().tick(player, data, def, upgradeLevel);
		}
	}

	private static void tickRuler(ServerPlayer player, CradlePlayerData data,
	                                AbilityDefinition def, int upgradeLevel,
	                                int slot, PlayerLoadout loadout) {
		// Madra drain
		float drain = def.getScaledMadraCost(upgradeLevel) * data.getMadraCostMultiplier();
		data.setCurrentMadra(data.getCurrentMadra() - drain);

		if (data.getCurrentMadra() <= 0) {
			loadout.setSlotActive(slot, false);
			player.displayClientMessage(Component.literal("\u00a7cOut of Madra!"), true);
			return;
		}

		// Apply area effects every RULER_EFFECT_INTERVAL ticks
		String tickKey = player.getUUID().toString() + ":" + slot;
		int ticks = RULER_EFFECT_TICKS.getOrDefault(tickKey, 0) + 1;
		if (ticks >= RULER_EFFECT_INTERVAL) {
			ticks = 0;
			if (def.getRulerArea() != null) {
				float radius = def.getScaledRadius(upgradeLevel);
				AABB area = player.getBoundingBox().inflate(radius);
				ServerLevel level = player.level();
				List<LivingEntity> enemies = level.getEntitiesOfClass(
					LivingEntity.class, area, e -> e != player && e.isAlive() && !e.isAlliedTo(player));
				def.getRulerArea().applyArea(player, data, def, upgradeLevel, enemies, level);
			}
		}
		RULER_EFFECT_TICKS.put(tickKey, ticks);
	}

	// ── Helpers ───────────────────────────────────────────────────────

	/**
	 * Deactivate other active slots of the same ability type.
	 * Ensures only one enforcer or one ruler is active at a time.
	 */
	private static void deactivateOtherToggleSlots(ServerPlayer player, CradlePlayerData data,
	                                                PlayerLoadout loadout, int currentSlot, AbilityType type) {
		for (int i = 0; i < PlayerLoadout.MAX_SLOTS; i++) {
			if (i == currentSlot || !loadout.isSlotActive(i)) continue;
			String otherId = loadout.getAbility(i);
			if (otherId == null) continue;
			AbilityDefinition otherDef = AbilityRegistry.get(otherId);
			if (otherDef != null && otherDef.getType() == type) {
				loadout.setSlotActive(i, false);
				if (type == AbilityType.ENFORCER && otherDef.getEnforcerDeactivate() != null) {
					otherDef.getEnforcerDeactivate().deactivate(player, data, otherDef, loadout.getUpgradeLevel(i));
				}
			}
		}
	}

	/**
	 * Disrupts active cycling if the player is below Underlord.
	 * Underlord+ can use abilities while cycling.
	 */
	private static void disruptCyclingIfNeeded(ServerPlayer player, CradlePlayerData data) {
		if (data.isActivelyCycling() &&
			data.getAdvancementStage().ordinal() < CradlePlayerData.AdvancementStage.UNDERLORD.ordinal()) {
			data.setActivelyCycling(false);
			player.displayClientMessage(Component.literal("\u00a77Cycling disrupted by technique use"), true);
		}
	}

	/**
	 * Clean up all executor state for a player (on disconnect/death).
	 */
	public static void cleanupPlayer(ServerPlayer player) {
		String prefix = player.getUUID().toString() + ":";
		COOLDOWNS.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
		RULER_EFFECT_TICKS.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
	}

	/**
	 * Clear all executor state (on server stop).
	 */
	public static void clearAll() {
		COOLDOWNS.clear();
		RULER_EFFECT_TICKS.clear();
	}
}

package com.cradle.mod;

import com.cradle.mod.network.CradleSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the cycling tick loop. Every server tick (20 per second), this checks
 * every online player and grants XP + Madra if they're actively cycling.
 * Passive Madra regen happens for everyone regardless.
 *
 * While actively cycling, the player must stand still (movement stops cycling)
 * and gets a glowing outline colored by their Path.
 */
public final class CyclingManager {

	// ── Tuning constants ───────────────────────────────────────────────

	// Passive regen: everyone gets a tiny bit of Madra each tick
	private static final float PASSIVE_MADRA_PER_TICK = 0.01f;

	// Active cycling: XP and Madra gained per tick while cycling
	private static final int ACTIVE_XP_PER_TICK = 1;
	private static final float ACTIVE_MADRA_PER_TICK = 0.1f;

	// Base XP needed to level up. Flat — every level costs the same.
	private static final int BASE_XP_TO_LEVEL = 1500;

	// Scaling multiplier per level (1.0 = flat, no exponential growth)
	private static final float XP_SCALING_PER_LEVEL = 1.0f;

	// How much maxMadra increases per level
	private static final float MADRA_PER_LEVEL = 10.0f;

	// Movement threshold — if X or Z changes by more than this, cycling stops
	private static final double MOVE_THRESHOLD = 0.01;

	// Tracks player position when they start cycling (for movement detection)
	private static final Map<UUID, double[]> CYCLING_POSITIONS = new HashMap<>();

	// ── Enforcer tuning constants ─────────────────────────────────────
	// Madra drain per tick while Enforcer is active (base rate, before path modifier)
	private static final float ENFORCER_MADRA_DRAIN_PER_TICK = 0.5f;

	// Black Flame Burning Body: self-damage every N ticks (strain)
	private static final int BURNING_BODY_STRAIN_INTERVAL = 40; // 2 seconds
	private static final float BURNING_BODY_STRAIN_DAMAGE = 1.0f; // half a heart

	// Hollow King: Madra regen bonus per tick while Enforcer is active (offsets drain)
	private static final float HOLLOW_CIRCULATION_REGEN_BONUS = 0.3f;

	// Attribute modifier IDs for Enforcer effects (must be unique and stable)
	private static final Identifier ENFORCER_ATTACK_DAMAGE_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_attack_damage");
	private static final Identifier ENFORCER_SPEED_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_speed");
	private static final Identifier ENFORCER_ATTACK_SPEED_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_attack_speed");
	private static final Identifier ENFORCER_ARMOR_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_armor");
	private static final Identifier ENFORCER_KNOCKBACK_RESISTANCE_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_knockback_resistance");

	// Tracks strain tick counter for Burning Body (Black Flame)
	private static final Map<UUID, Integer> BURNING_BODY_STRAIN_TICKS = new HashMap<>();

	// ── Tick handler ───────────────────────────────────────────────────

	public static void onServerTick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID playerId = player.getUUID();
			CradlePlayerData data = CradlePlayerData.getOrCreate(playerId);

			// Passive Madra regen for everyone (scales with stage)
			if (data.getCurrentMadra() < data.getMaxMadra()) {
				float passiveRate = PASSIVE_MADRA_PER_TICK * data.getCyclingSpeedMultiplier();
				data.setCurrentMadra(data.getCurrentMadra() + passiveRate);
			}

			// Iron Body passive buff (Steelborn/Raindrop toggle; Bloodforged is instant on P press)
			// Only re-apply when the effect is missing or about to expire,
			// so the internal tick counter can progress and the effect actually works.
			if (data.isIronBodyActive()) {
				switch (data.getIronBody()) {
					case STEELBORN -> {
						if (!player.hasEffect(MobEffects.RESISTANCE) || player.getEffect(MobEffects.RESISTANCE).getDuration() < 10) {
							player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0, false, false));
						}
					}
					case RAINDROP -> {
						if (!player.hasEffect(MobEffects.SPEED) || player.getEffect(MobEffects.SPEED).getDuration() < 10) {
							player.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, false, false));
						}
					}
					default -> {}
				}
			}

			// ── Enforcer technique tick (R key toggle) ──────────────────
			if (data.isEnforcerActive()) {
				// Drain Madra each tick
				float drain = ENFORCER_MADRA_DRAIN_PER_TICK;

				// Hollow King: reduced drain (offset by regen bonus)
				if (data.getChosenPath() == CradlePlayerData.Path.HOLLOW_KING) {
					drain -= HOLLOW_CIRCULATION_REGEN_BONUS;
				}

				// Gold stage: 30% reduced Madra cost
				if (data.getAdvancementStage() == CradlePlayerData.AdvancementStage.GOLD) {
					drain *= 0.7f;
				}

				data.setCurrentMadra(data.getCurrentMadra() - Math.max(0, drain));

				// If out of Madra, deactivate Enforcer
				if (data.getCurrentMadra() <= 0) {
					deactivateEnforcer(player, data);
					player.sendSystemMessage(Component.literal(
							"\u00A76[Cradle] \u00A7cEnforcer technique deactivated — out of Madra!"
					));
				} else {
					// Apply path-specific effects
					applyEnforcerEffects(player, data);
				}
			}

			// Active cycling: faster XP + Madra gain, but must stand still
			if (data.isActivelyCycling()) {
				// Check for movement — if player moved, stop cycling
				double[] startPos = CYCLING_POSITIONS.get(playerId);
				if (startPos != null) {
					double dx = Math.abs(player.getX() - startPos[0]);
					double dz = Math.abs(player.getZ() - startPos[1]);
					if (dx > MOVE_THRESHOLD || dz > MOVE_THRESHOLD) {
						stopCycling(player, data);
						player.sendSystemMessage(Component.literal(
								"§6[Cradle] §fYou moved and stopped cycling."
						));
						// Still send sync this tick so client sees the change
						ServerPlayNetworking.send(player, createSyncPayload(player, data));
						continue;
					}
				}

				// Prevent sprinting while cycling
				player.setSprinting(false);

				// Glowing outline while cycling (refreshed every tick, 40 ticks duration as safety buffer)
				player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));

				// Add cycling XP (scales with stage)
				float multiplier = data.getCyclingSpeedMultiplier();
				data.setCyclingXp(data.getCyclingXp() + (int) (ACTIVE_XP_PER_TICK * multiplier));

				// Add Madra (faster than passive, scales with stage)
				data.setCurrentMadra(data.getCurrentMadra() + ACTIVE_MADRA_PER_TICK * multiplier);

				// Check for level-up (capped at next breakthrough level)
				int nextBreakthroughLevel = BreakthroughManager.getNextBreakthroughLevel(data.getAdvancementStage());
				boolean atCap = nextBreakthroughLevel > 0 && data.getPlayerLevel() >= nextBreakthroughLevel;

				if (!atCap) {
					int xpNeeded = xpToNextLevel(data.getPlayerLevel());
					if (data.getCyclingXp() >= xpNeeded) {
						levelUp(player, data);
						// Check for stage breakthrough after leveling up
						BreakthroughManager.checkBreakthrough(player, data);
					}
				} else {
					// Cap XP at the max so the bar shows full
					int xpNeeded = xpToNextLevel(data.getPlayerLevel());
					if (data.getCyclingXp() > xpNeeded) {
						data.setCyclingXp(xpNeeded);
					}
				}
			}

			// Send sync packet to client every tick (packet is tiny, ~30 bytes)
			ServerPlayNetworking.send(player, createSyncPayload(player, data));
		}
	}

	// ── Cycling start/stop ────────────────────────────────────────────

	/**
	 * Call when a player starts actively cycling.
	 * Records their position for movement detection.
	 */
	public static void startCycling(ServerPlayer player, CradlePlayerData data) {
		data.setActivelyCycling(true);
		CYCLING_POSITIONS.put(player.getUUID(), new double[]{player.getX(), player.getZ()});
	}

	/**
	 * Call when a player stops cycling (by command or by moving).
	 * Restores normal pose.
	 */
	public static void stopCycling(ServerPlayer player, CradlePlayerData data) {
		data.setActivelyCycling(false);
		CYCLING_POSITIONS.remove(player.getUUID());
		player.removeEffect(MobEffects.GLOWING);
	}

	// ── Enforcer technique management ─────────────────────────────────

	/**
	 * Activates the Enforcer technique for a player.
	 * Called from the server handler when the player presses R.
	 */
	public static void activateEnforcer(ServerPlayer player, CradlePlayerData data) {
		data.setEnforcerActive(true);
		BURNING_BODY_STRAIN_TICKS.put(player.getUUID(), 0);
	}

	/**
	 * Deactivates the Enforcer technique and removes all attribute modifiers.
	 */
	public static void deactivateEnforcer(ServerPlayer player, CradlePlayerData data) {
		data.setEnforcerActive(false);
		BURNING_BODY_STRAIN_TICKS.remove(player.getUUID());
		removeEnforcerModifiers(player);
	}

	/**
	 * Applies path-specific Enforcer effects each tick.
	 * Uses attribute modifiers for stat boosts (applied once, checked each tick)
	 * and potion effects for visual/status effects.
	 */
	private static void applyEnforcerEffects(ServerPlayer player, CradlePlayerData data) {
		boolean isGold = data.getAdvancementStage() == CradlePlayerData.AdvancementStage.GOLD;

		switch (data.getChosenPath()) {
			case BLACK_FLAME -> {
				// Burning Body: +attack damage, +sprint speed, attacks ignite enemies (via Fire Aspect-like effect)
				// Self-damage over time as strain
				double dmgBonus = isGold ? 6.0 : 4.0;  // +4 damage (Gold: +6)
				double speedBonus = isGold ? 0.06 : 0.04; // +speed

				ensureModifier(player, Attributes.ATTACK_DAMAGE, ENFORCER_ATTACK_DAMAGE_ID,
						dmgBonus, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(player, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						speedBonus, AttributeModifier.Operation.ADD_VALUE);

				// Fire resistance (so self-fire from Fire Aspect doesn't annoy)
				// Actually, apply fire aspect via setting attack targets on fire in a separate event
				// For now: FIRE_RESISTANCE so the player doesn't get annoyed by their own fire
				if (!player.hasEffect(MobEffects.FIRE_RESISTANCE) || player.getEffect(MobEffects.FIRE_RESISTANCE).getDuration() < 10) {
					player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false));
				}

				// Self-damage strain every BURNING_BODY_STRAIN_INTERVAL ticks
				int strainTicks = BURNING_BODY_STRAIN_TICKS.getOrDefault(player.getUUID(), 0) + 1;
				BURNING_BODY_STRAIN_TICKS.put(player.getUUID(), strainTicks);
				if (strainTicks >= BURNING_BODY_STRAIN_INTERVAL) {
					BURNING_BODY_STRAIN_TICKS.put(player.getUUID(), 0);
					// Use generic damage that bypasses armor
					player.hurtServer(player.level(), player.damageSources().magic(), BURNING_BODY_STRAIN_DAMAGE);
				}
			}

			case ENDLESS_SWORD -> {
				// Flowing Edge: +attack speed, reduced attack cooldown
				double atkSpeedBonus = isGold ? 1.5 : 1.0; // +1.0 attack speed (Gold: +1.5)

				ensureModifier(player, Attributes.ATTACK_SPEED, ENFORCER_ATTACK_SPEED_ID,
						atkSpeedBonus, AttributeModifier.Operation.ADD_VALUE);
			}

			case STELLAR_SPEAR -> {
				// Stellar Alignment: +forward speed, reduced knockback taken
				double speedBonus = isGold ? 0.06 : 0.04;
				double kbResist = isGold ? 0.8 : 0.6;

				ensureModifier(player, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						speedBonus, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(player, Attributes.KNOCKBACK_RESISTANCE, ENFORCER_KNOCKBACK_RESISTANCE_ID,
						kbResist, AttributeModifier.Operation.ADD_VALUE);
			}

			case CLOUD_HAMMER -> {
				// Thunderous Weight: +armor, +knockback dealt, -movement speed
				double armorBonus = isGold ? 8.0 : 6.0;
				double speedPenalty = isGold ? -0.02 : -0.03; // Less penalty at Gold

				ensureModifier(player, Attributes.ARMOR, ENFORCER_ARMOR_ID,
						armorBonus, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(player, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						speedPenalty, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(player, Attributes.ATTACK_KNOCKBACK, ENFORCER_KNOCKBACK_RESISTANCE_ID,
						isGold ? 3.0 : 2.0, AttributeModifier.Operation.ADD_VALUE);
			}

			case HOLLOW_KING -> {
				// Hollow Circulation: slight damage reduction, passive Madra regen
				// Madra regen is handled above (drain offset). Damage reduction via Resistance.
				if (!player.hasEffect(MobEffects.RESISTANCE) || player.getEffect(MobEffects.RESISTANCE).getDuration() < 10) {
					int amp = isGold ? 1 : 0; // Resistance I (Gold: Resistance II)
					player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, amp, false, false));
				}
			}

			default -> {} // UNSET path — no effects
		}
	}

	/**
	 * Ensures an attribute modifier is present on the player. If it's already there
	 * with the correct value, does nothing. If the value changed (e.g., Gold upgrade),
	 * removes and re-adds it.
	 */
	private static void ensureModifier(ServerPlayer player,
										net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
										Identifier id, double amount, AttributeModifier.Operation operation) {
		var instance = player.getAttribute(attribute);
		if (instance == null) return;

		AttributeModifier existing = instance.getModifier(id);
		if (existing != null) {
			if (existing.amount() == amount && existing.operation() == operation) {
				return; // Already correct
			}
			instance.removeModifier(id);
		}
		instance.addTransientModifier(new AttributeModifier(id, amount, operation));
	}

	/**
	 * Removes all Enforcer-related attribute modifiers from the player.
	 * Called when the Enforcer technique is deactivated.
	 */
	public static void removeEnforcerModifiers(ServerPlayer player) {
		Identifier[] modifierIds = {
				ENFORCER_ATTACK_DAMAGE_ID,
				ENFORCER_SPEED_ID,
				ENFORCER_ATTACK_SPEED_ID,
				ENFORCER_ARMOR_ID,
				ENFORCER_KNOCKBACK_RESISTANCE_ID
		};

		for (Identifier id : modifierIds) {
			// Try removing from all relevant attributes
			removeModifierSafe(player, Attributes.ATTACK_DAMAGE, id);
			removeModifierSafe(player, Attributes.MOVEMENT_SPEED, id);
			removeModifierSafe(player, Attributes.ATTACK_SPEED, id);
			removeModifierSafe(player, Attributes.ARMOR, id);
			removeModifierSafe(player, Attributes.KNOCKBACK_RESISTANCE, id);
			removeModifierSafe(player, Attributes.ATTACK_KNOCKBACK, id);
		}
	}

	private static void removeModifierSafe(ServerPlayer player,
											net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
											Identifier id) {
		var instance = player.getAttribute(attribute);
		if (instance != null && instance.getModifier(id) != null) {
			instance.removeModifier(id);
		}
	}

	// ── Level-up logic ─────────────────────────────────────────────────

	/**
	 * Calculate XP needed for the next level.
	 * Higher levels need more XP.
	 */
	public static int xpToNextLevel(int currentLevel) {
		return (int) (BASE_XP_TO_LEVEL * Math.pow(XP_SCALING_PER_LEVEL, currentLevel));
	}

	private static void levelUp(ServerPlayer player, CradlePlayerData data) {
		int xpNeeded = xpToNextLevel(data.getPlayerLevel());
		data.setCyclingXp(data.getCyclingXp() - xpNeeded);
		data.setPlayerLevel(data.getPlayerLevel() + 1);

		// Increase max Madra with each level
		data.setMaxMadra(data.getMaxMadra() + MADRA_PER_LEVEL);

		// Tell the player
		player.sendSystemMessage(Component.literal(
				"§6[Cradle] §aLevel up! You are now level " + data.getPlayerLevel() + "!"
		));

		// Save immediately — level-up is important progress
		CradleMod.autoSave(player.level().getServer());

		CradleMod.LOGGER.info("Player {} leveled up to {}", player.getName().getString(), data.getPlayerLevel());
	}

	// ── Sync payload helper ────────────────────────────────────────────

	/**
	 * Creates a sync packet from the current player data.
	 * Requires the ServerPlayer to check if they can advance (inventory check).
	 */
	public static CradleSyncPayload createSyncPayload(ServerPlayer player, CradlePlayerData data) {
		return new CradleSyncPayload(
				data.getPlayerLevel(),
				data.getCyclingXp(),
				xpToNextLevel(data.getPlayerLevel()),
				data.getChosenPath().name(),
				data.getAdvancementStage().name(),
				data.getCurrentMadra(),
				data.getMaxMadra(),
				data.isActivelyCycling(),
				BreakthroughManager.canAdvance(player, data),
				data.getIronBody().name(),
				data.isIronBodyActive(),
				data.isEnforcerActive()
		);
	}
}

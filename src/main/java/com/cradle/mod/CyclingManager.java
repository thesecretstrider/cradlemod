package com.cradle.mod;

import com.cradle.mod.network.CradleSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirtPathBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
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

	// Sword cycling: multiplier applied when cycling with sword stabbed into block
	// Only applies to sword paths (Endless Sword, Stellar Spear)
	private static final float SWORD_CYCLING_MULTIPLIER = 2.0f;

	// Combat XP: granted per point of mob max health on kill (e.g. zombie with 20hp = 100 XP)
	private static final float COMBAT_XP_PER_HP = 5.0f;

	// Environmental cycling bonus: applied when cycling near path-specific environment
	private static final float ENVIRONMENTAL_BONUS = 1.5f;
	// How often to check environment (every N ticks) — avoids scanning blocks every tick
	private static final int ENVIRONMENT_CHECK_INTERVAL = 20; // 1 second
	// Tracks environment check counter per player
	private static final Map<UUID, Integer> ENVIRONMENT_CHECK_TICKS = new HashMap<>();
	// Cached environment bonus per player (updated every ENVIRONMENT_CHECK_INTERVAL)
	private static final Map<UUID, Float> CACHED_ENV_BONUS = new HashMap<>();

	// Flight: particle spawn interval (every N ticks while flying)
	private static final int FLIGHT_PARTICLE_INTERVAL = 5;
	private static final Map<UUID, Integer> FLIGHT_PARTICLE_TICKS = new HashMap<>();
	// Tracks whether the player was actively flying last tick (for cushioned landing detection)
	private static final Map<UUID, Boolean> WAS_FLYING_LAST_TICK = new HashMap<>();
	// Slow Falling duration for cushioned landing (3 seconds = 60 ticks)
	private static final int CUSHIONED_LANDING_DURATION = 60;

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

	// ── Ruler tuning constants ────────────────────────────────────────
	// Madra drain per tick while Ruler is active
	private static final float RULER_MADRA_DRAIN_PER_TICK = 0.4f;
	// Ruler area radius (blocks)
	private static final double RULER_RADIUS = 6.0;
	// How often Ruler effects tick on nearby enemies (every N ticks)
	private static final int RULER_EFFECT_INTERVAL = 10; // every 0.5 seconds
	// Tracks Ruler effect tick counter
	private static final Map<UUID, Integer> RULER_EFFECT_TICKS = new HashMap<>();

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

			// Iron Body passive buff (Steelborn/Raindrop toggle)
			if (data.isIronBodyActive()) {
				switch (data.getIronBody()) {
					case STEELBORN -> refreshEffect(player, MobEffects.RESISTANCE, 60, 0);
					case RAINDROP -> refreshEffect(player, MobEffects.SPEED, 60, 0);
					default -> {}
				}
			}

			// ── Flight tick (Underlord+ / Cloud Hammer Copper+) ─────────
			boolean canFly = data.canFly();
			boolean wasFlying = WAS_FLYING_LAST_TICK.getOrDefault(playerId, false);
			if (data.isUnderlordFlying()) {
				boolean currentlyFlying = player.getAbilities().flying;

				if (!canFly) {
					// Stage was lowered — revoke flight (no cushion)
					disableFlight(player, data);
				} else if (currentlyFlying) {
					// Actually airborne and flying — drain madra
					float drain = data.getFlightMadraDrain();
					data.setCurrentMadra(data.getCurrentMadra() - drain);
					if (data.getCurrentMadra() <= 0) {
						// Ran out of madra — hard fall, no cushion
						disableFlight(player, data);
						player.displayClientMessage(Component.literal(
								"\u00A7cFlight deactivated \u2014 out of Madra!"), true);
					} else {
						// Path-colored particles below feet every 5 ticks
						int pTick = FLIGHT_PARTICLE_TICKS.getOrDefault(playerId, 0) + 1;
						FLIGHT_PARTICLE_TICKS.put(playerId, pTick);
						if (pTick >= FLIGHT_PARTICLE_INTERVAL) {
							FLIGHT_PARTICLE_TICKS.put(playerId, 0);
							((ServerLevel) player.level()).sendParticles(
									net.minecraft.core.particles.ParticleTypes.END_ROD,
									player.getX(), player.getY() - 0.5, player.getZ(),
									3, 0.2, 0.0, 0.2, 0.01);
						}
					}
				} else if (wasFlying && !currentlyFlying) {
					// Player voluntarily stopped flying (double-tap space or landed)
					// Cushion the fall with Slow Falling — clouds break their descent
					player.addEffect(new MobEffectInstance(
							MobEffects.SLOW_FALLING, CUSHIONED_LANDING_DURATION, 0, false, true));
				}

				WAS_FLYING_LAST_TICK.put(playerId, currentlyFlying);
			} else {
				WAS_FLYING_LAST_TICK.remove(playerId);
				if (canFly && !player.isCreative() && !player.isSpectator()) {
					// Auto-grant flight capability when stage requirement is met
					enableFlight(player, data);
				}
			}

			// ── Cloud Hammer fall cushion (wind catches them before impact) ──
			if (data.getChosenPath() == CradlePlayerData.Path.CLOUD_HAMMER
					&& data.getCurrentMadra() > 0
					&& !player.isCreative() && !player.isSpectator()) {
				// fallDistance tracks how far the player has fallen (resets on landing)
				// 3+ blocks of falling = would take fall damage. Apply Slow Falling to cushion.
				if (player.fallDistance >= 3.0f && !player.hasEffect(MobEffects.SLOW_FALLING)) {
					player.addEffect(new MobEffectInstance(
							MobEffects.SLOW_FALLING, CUSHIONED_LANDING_DURATION, 0, false, true));
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

				// Higher stages reduce Madra cost (graduated from 1.0x down to 0.4x at Monarch)
				drain *= data.getMadraCostMultiplier();

				data.setCurrentMadra(data.getCurrentMadra() - Math.max(0, drain));

				// If out of Madra, deactivate Enforcer
				if (data.getCurrentMadra() <= 0) {
					deactivateEnforcer(player, data);
					player.displayClientMessage(Component.literal(
							"\u00A7cEnforcer deactivated — out of Madra!"
					), true);
				} else {
					// Apply path-specific effects
					applyEnforcerEffects(player, data);
				}
			}

			// ── Ruler technique tick (C key toggle) ──────────────────
			if (data.isRulerActive()) {
				float drain = RULER_MADRA_DRAIN_PER_TICK * data.getMadraCostMultiplier();
				data.setCurrentMadra(data.getCurrentMadra() - drain);

				if (data.getCurrentMadra() <= 0) {
					data.setRulerActive(false);
					RULER_EFFECT_TICKS.remove(playerId);
					player.displayClientMessage(Component.literal(
							"\u00A7cRuler deactivated — out of Madra!"
					), true);
				} else {
					// Apply area effects every RULER_EFFECT_INTERVAL ticks
					int rulerTick = RULER_EFFECT_TICKS.getOrDefault(playerId, 0) + 1;
					RULER_EFFECT_TICKS.put(playerId, rulerTick);
					if (rulerTick >= RULER_EFFECT_INTERVAL) {
						RULER_EFFECT_TICKS.put(playerId, 0);
						applyRulerEffects(player, data);
					}
				}
			}

			// Active cycling: faster XP + Madra gain, but must stand still (unless Herald)
			if (data.isActivelyCycling()) {
				// Check for movement — if player moved, stop cycling
				// Herald can cycle while moving (body transcends physical limits)
				if (!canCycleWhileMoving(data)) {
					double[] startPos = CYCLING_POSITIONS.get(playerId);
					if (startPos != null) {
						double dx = Math.abs(player.getX() - startPos[0]);
						double dz = Math.abs(player.getZ() - startPos[1]);
						if (dx > MOVE_THRESHOLD || dz > MOVE_THRESHOLD) {
							boolean wasSwordCycling = data.isSwordCycling();
							stopCycling(player, data);
							player.displayClientMessage(Component.literal(wasSwordCycling
									? "\u00A77You pull your blade free from the earth."
									: "\u00A7fYou moved and stopped cycling."
							), true);
							// Still send sync this tick so client sees the change
							ServerPlayNetworking.send(player, createSyncPayload(player, data));
							continue;
						}
					}
				}

				// Prevent sprinting while cycling (unless Herald)
				if (!canCycleWhileMoving(data)) {
					player.setSprinting(false);
				}

				// Glowing outline while cycling (refreshed every tick, 40 ticks duration as safety buffer)
				player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));

				// Validate sword cycling — clear if player no longer holds a sword
				if (data.isSwordCycling()) {
					if (!isHoldingSword(player)) {
						data.setSwordCycling(false);
					}
				}

				// Add cycling XP (scales with stage)
				float multiplier = data.getCyclingSpeedMultiplier();

				// Sword cycling: 2x bonus for sword paths (Endless Sword / Stellar Spear)
				if (data.isSwordCycling()) {
					multiplier *= SWORD_CYCLING_MULTIPLIER;
				}

				// Environmental cycling bonus (path-specific)
				float envBonus = getEnvironmentalBonus(player, data);
				if (envBonus > 1.0f) {
					multiplier *= envBonus;
				}

				// Underlord–Archlord: cycling while using abilities runs at half speed
				// Sage/Monarch: full speed (perfect aura control)
				boolean usingAbility = data.isEnforcerActive() || data.isRulerActive();
				if (usingAbility && !data.hasSage()) {
					multiplier *= 0.5f;
				}

				data.setCyclingXp(data.getCyclingXp() + (int) (ACTIVE_XP_PER_TICK * multiplier));

				// Add Madra (faster than passive, scales with stage)
				data.setCurrentMadra(data.getCurrentMadra() + ACTIVE_MADRA_PER_TICK * multiplier);

				// Check for level-up (capped at next breakthrough level)
				int nextBreakthroughLevel = BreakthroughManager.getNextBreakthroughLevel(data);
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

	// ── Cycling + ability conflict ────────────────────────────────────

	/**
	 * Returns true if the player can maintain cycling while using abilities.
	 * Below Underlord: techniques disrupt cycling entirely.
	 * Underlord–Archlord: can cycle while using abilities, but at half speed.
	 * Sage/Monarch: full speed cycling while using abilities (perfect aura control).
	 */
	public static boolean canCycleWhileUsingAbilities(CradlePlayerData data) {
		if (data.hasSage()) return true;
		if (data.hasHerald()) return true; // Herald's body transcends physical limits
		return data.getAdvancementStage().ordinal() >= CradlePlayerData.AdvancementStage.UNDERLORD.ordinal();
	}

	/**
	 * Whether the player can cycle while moving.
	 * Herald's body transcends physical limits — no need to sit still.
	 */
	public static boolean canCycleWhileMoving(CradlePlayerData data) {
		return data.hasHerald();
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
		data.setSwordCycling(false);
		CYCLING_POSITIONS.remove(player.getUUID());
		ENVIRONMENT_CHECK_TICKS.remove(player.getUUID());
		CACHED_ENV_BONUS.remove(player.getUUID());
		player.removeEffect(MobEffects.GLOWING);
	}

	// ── Flight management ─────────────────────────────────────────

	/**
	 * Grants flight capability (mayfly) to the player.
	 * Does NOT force the player into flight — they must double-tap jump.
	 */
	public static void enableFlight(ServerPlayer player, CradlePlayerData data) {
		if (!player.isCreative() && !player.isSpectator()) {
			player.getAbilities().mayfly = true;
			player.onUpdateAbilities();
		}
		data.setUnderlordFlying(true);
	}

	/**
	 * Revokes flight capability and forces the player out of flight.
	 * Safe to call even if the player is in creative/spectator.
	 */
	public static void disableFlight(ServerPlayer player, CradlePlayerData data) {
		data.setUnderlordFlying(false);
		FLIGHT_PARTICLE_TICKS.remove(player.getUUID());
		WAS_FLYING_LAST_TICK.remove(player.getUUID());
		if (!player.isCreative() && !player.isSpectator()) {
			player.getAbilities().mayfly = false;
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
		}
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

	/** Applies path-specific Enforcer effects each tick (attribute modifiers + status effects). */
	private static void applyEnforcerEffects(ServerPlayer player, CradlePlayerData data) {
		float pm = data.getAbilityPowerMultiplier();

		switch (data.getChosenPath()) {
			case BLACK_FLAME -> {
				ensureModifier(player, Attributes.ATTACK_DAMAGE, ENFORCER_ATTACK_DAMAGE_ID,
						4.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(player, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						0.04 * pm, AttributeModifier.Operation.ADD_VALUE);
				refreshEffect(player, MobEffects.FIRE_RESISTANCE, 60, 0);

				// Self-damage strain (Blackflame burns its user)
				int strain = BURNING_BODY_STRAIN_TICKS.getOrDefault(player.getUUID(), 0) + 1;
				BURNING_BODY_STRAIN_TICKS.put(player.getUUID(), strain);
				if (strain >= BURNING_BODY_STRAIN_INTERVAL) {
					BURNING_BODY_STRAIN_TICKS.put(player.getUUID(), 0);
					player.hurtServer(player.level(), player.damageSources().magic(), BURNING_BODY_STRAIN_DAMAGE);
				}
			}
			case ENDLESS_SWORD -> ensureModifier(player, Attributes.ATTACK_SPEED,
					ENFORCER_ATTACK_SPEED_ID, 1.0 * pm, AttributeModifier.Operation.ADD_VALUE);
			case STELLAR_SPEAR -> {
				ensureModifier(player, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						0.04 * pm, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(player, Attributes.KNOCKBACK_RESISTANCE, ENFORCER_KNOCKBACK_RESISTANCE_ID,
						Math.min(0.6 * pm, 1.0), AttributeModifier.Operation.ADD_VALUE);
			}
			case CLOUD_HAMMER -> {
				ensureModifier(player, Attributes.ARMOR, ENFORCER_ARMOR_ID,
						6.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(player, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						-0.03 + (pm - 1.0) * 0.01, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(player, Attributes.ATTACK_KNOCKBACK, ENFORCER_KNOCKBACK_RESISTANCE_ID,
						2.0 * pm, AttributeModifier.Operation.ADD_VALUE);
			}
			case HOLLOW_KING -> refreshEffect(player, MobEffects.RESISTANCE, 60, pm >= 1.5f ? 1 : 0);
			default -> {}
		}
	}

	/** Refreshes a potion effect only when it's missing or about to expire. */
	private static void refreshEffect(ServerPlayer player,
										net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
										int duration, int amplifier) {
		if (!player.hasEffect(effect) || player.getEffect(effect).getDuration() < 10) {
			player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false));
		}
	}

	/** Ensures an attribute modifier is present with the correct value. */
	private static void ensureModifier(ServerPlayer player, Holder<Attribute> attribute,
										Identifier id, double amount, AttributeModifier.Operation op) {
		var instance = player.getAttribute(attribute);
		if (instance == null) return;
		AttributeModifier existing = instance.getModifier(id);
		if (existing != null) {
			if (existing.amount() == amount && existing.operation() == op) return;
			instance.removeModifier(id);
		}
		instance.addTransientModifier(new AttributeModifier(id, amount, op));
	}

	/** Removes all Enforcer attribute modifiers from the player. */
	@SuppressWarnings("unchecked")
	public static void removeEnforcerModifiers(ServerPlayer player) {
		Identifier[] ids = {
			ENFORCER_ATTACK_DAMAGE_ID, ENFORCER_SPEED_ID, ENFORCER_ATTACK_SPEED_ID,
			ENFORCER_ARMOR_ID, ENFORCER_KNOCKBACK_RESISTANCE_ID
		};
		Holder<Attribute>[] attrs = new Holder[]{
			Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED, Attributes.ATTACK_SPEED,
			Attributes.ARMOR, Attributes.KNOCKBACK_RESISTANCE, Attributes.ATTACK_KNOCKBACK
		};
		for (Identifier id : ids) {
			for (Holder<Attribute> attr : attrs) {
				removeModifierSafe(player, attr, id);
			}
		}
	}

	private static void removeModifierSafe(ServerPlayer player, Holder<Attribute> attribute, Identifier id) {
		var instance = player.getAttribute(attribute);
		if (instance != null && instance.getModifier(id) != null) instance.removeModifier(id);
	}

	// ── Ruler technique effects ────────────────────────────────────────

	/** Applies Ruler area effects to nearby enemies. Called every 0.5s while Ruler is active. */
	private static void applyRulerEffects(ServerPlayer player, CradlePlayerData data) {
		float powerMult = data.getAbilityPowerMultiplier();
		AABB area = player.getBoundingBox().inflate(RULER_RADIUS);
		List<LivingEntity> enemies = player.level().getEntitiesOfClass(
				LivingEntity.class, area, e -> e != player && e.isAlive() && !e.isAlliedTo(player));
		if (enemies.isEmpty()) return;

		ServerLevel level = player.level();
		boolean underlordPlus = powerMult >= 1.5f;

		switch (data.getChosenPath()) {
			case BLACK_FLAME -> {
				float damage = 2.0f * powerMult;
				for (LivingEntity e : enemies) {
					e.hurtServer(level, player.damageSources().magic(), damage);
					e.igniteForSeconds(2.0f);
				}
			}
			case ENDLESS_SWORD -> {
				float damage = 1.5f * powerMult;
				for (LivingEntity e : enemies) {
					if (e.getDeltaMovement().horizontalDistance() > 0.01) {
						e.hurtServer(level, player.damageSources().magic(), damage);
					}
				}
			}
			case STELLAR_SPEAR -> {
				float damage = 2.0f * powerMult;
				for (LivingEntity e : enemies) {
					Vec3 toPlayer = player.position().subtract(e.position()).normalize();
					Vec3 movement = e.getDeltaMovement().normalize();
					if (toPlayer.x * movement.x + toPlayer.z * movement.z > 0.3) {
						e.hurtServer(level, player.damageSources().magic(), damage);
					}
				}
			}
			case CLOUD_HAMMER -> {
				int amp = underlordPlus ? 2 : 1;
				for (LivingEntity e : enemies) {
					e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, amp, false, false));
					e.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20, 128, false, false));
				}
			}
			case HOLLOW_KING -> {
				int weakAmp = underlordPlus ? 1 : 0;
				for (LivingEntity e : enemies) {
					e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, weakAmp, false, false));
				}
				if (!player.hasEffect(MobEffects.RESISTANCE) || player.getEffect(MobEffects.RESISTANCE).getDuration() < 10) {
					player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20, weakAmp, false, false));
				}
			}
			default -> {}
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

	// ── Combat XP ─────────────────────────────────────────────────────

	/**
	 * Grants cycling XP for killing a mob. XP scales with the mob's max health
	 * and the player's cycling speed multiplier. Respects breakthrough level cap.
	 */
	public static void grantCombatXp(ServerPlayer player, LivingEntity killed) {
		CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

		// XP based on mob toughness (max health) scaled by stage multiplier
		float maxHp = killed.getMaxHealth();
		int xpGain = (int) (maxHp * COMBAT_XP_PER_HP * data.getCyclingSpeedMultiplier());
		if (xpGain <= 0) return;

		// Check breakthrough cap
		int nextBreakthroughLevel = BreakthroughManager.getNextBreakthroughLevel(data);
		boolean atCap = nextBreakthroughLevel > 0 && data.getPlayerLevel() >= nextBreakthroughLevel;
		if (atCap) return; // No XP when capped at breakthrough

		data.setCyclingXp(data.getCyclingXp() + xpGain);

		// Small Madra restore on kill (10% of max health as Madra)
		float madraGain = maxHp * 0.1f;
		data.setCurrentMadra(Math.min(data.getCurrentMadra() + madraGain, data.getMaxMadra()));

		// Check for level-up (may chain multiple levels for big kills)
		boolean leveled = false;
		while (!atCap) {
			int xpNeeded = xpToNextLevel(data.getPlayerLevel());
			if (data.getCyclingXp() >= xpNeeded) {
				levelUp(player, data);
				BreakthroughManager.checkBreakthrough(player, data);
				leveled = true;
				// Re-check cap after leveling
				nextBreakthroughLevel = BreakthroughManager.getNextBreakthroughLevel(data);
				atCap = nextBreakthroughLevel > 0 && data.getPlayerLevel() >= nextBreakthroughLevel;
			} else {
				break;
			}
		}

		// Cap XP if at breakthrough
		if (atCap) {
			int xpNeeded = xpToNextLevel(data.getPlayerLevel());
			if (data.getCyclingXp() > xpNeeded) {
				data.setCyclingXp(xpNeeded);
			}
		}
	}

	// ── Environmental cycling bonus ───────────────────────────────────

	/**
	 * Returns the environmental cycling bonus (cached, rechecked every second).
	 * Black Flame 1.5x near fire/lava, Cloud Hammer 1.5x at Y>=128, others 1.0x.
	 * Shows an action bar message when the bonus first activates.
	 */
	private static float getEnvironmentalBonus(ServerPlayer player, CradlePlayerData data) {
		UUID id = player.getUUID();
		int tick = ENVIRONMENT_CHECK_TICKS.getOrDefault(id, 0) + 1;
		ENVIRONMENT_CHECK_TICKS.put(id, tick);
		if (tick < ENVIRONMENT_CHECK_INTERVAL) {
			return CACHED_ENV_BONUS.getOrDefault(id, 1.0f);
		}
		ENVIRONMENT_CHECK_TICKS.put(id, 0);
		float prevBonus = CACHED_ENV_BONUS.getOrDefault(id, 1.0f);
		float bonus = calculateEnvironmentalBonus(player, data);
		CACHED_ENV_BONUS.put(id, bonus);

		// Notify when bonus activates (transition from 1.0 to >1.0)
		if (bonus > 1.0f && prevBonus <= 1.0f) {
			String msg = switch (data.getChosenPath()) {
				case BLACK_FLAME -> "\u00A76The heat fuels your cycling.";
				case CLOUD_HAMMER -> "\u00A7bThe high winds empower your cycling.";
				default -> null;
			};
			if (msg != null) player.displayClientMessage(Component.literal(msg), true);
		}
		return bonus;
	}

	private static float calculateEnvironmentalBonus(ServerPlayer player, CradlePlayerData data) {
		return switch (data.getChosenPath()) {
			case BLACK_FLAME -> isNearHeatSource(player) ? ENVIRONMENTAL_BONUS : 1.0f;
			case CLOUD_HAMMER -> player.getY() >= 128 ? ENVIRONMENTAL_BONUS : 1.0f;
			default -> 1.0f; // sword paths use sword cycling, Hollow King has no env bonus
		};
	}

	/** Scans a 7×7×7 cube around the player for fire, lava, or magma blocks. */
	private static boolean isNearHeatSource(ServerPlayer player) {
		BlockPos pos = player.blockPosition();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dy = -3; dy <= 3; dy++) {
				for (int dz = -3; dz <= 3; dz++) {
					BlockState state = player.level().getBlockState(pos.offset(dx, dy, dz));
					if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
							|| state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// ── Sword cycling helpers ─────────────────────────────────────────

	/** Returns true if the player is holding any sword in either hand. */
	public static boolean isHoldingSword(ServerPlayer player) {
		return player.getMainHandItem().is(ItemTags.SWORDS)
				|| player.getOffhandItem().is(ItemTags.SWORDS);
	}

	/** Returns true if the block is soft enough to stab a sword into. */
	public static boolean isSoftBlock(BlockState blockState) {
		Block block = blockState.getBlock();
		return block instanceof GrassBlock || block instanceof DirtPathBlock
				|| blockState.is(Blocks.DIRT) || blockState.is(Blocks.COARSE_DIRT)
				|| blockState.is(Blocks.ROOTED_DIRT) || blockState.is(Blocks.SAND)
				|| blockState.is(Blocks.RED_SAND) || blockState.is(Blocks.GRAVEL)
				|| blockState.is(Blocks.SOUL_SAND) || blockState.is(Blocks.SOUL_SOIL)
				|| blockState.is(Blocks.CLAY) || blockState.is(Blocks.MUD)
				|| blockState.is(Blocks.FARMLAND) || blockState.is(Blocks.MYCELIUM)
				|| blockState.is(Blocks.PODZOL) || blockState.is(Blocks.SNOW_BLOCK);
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
				CradleSyncPayload.buildFlags(
						data.isActivelyCycling(),
						BreakthroughManager.canAdvance(player, data),
						data.isIronBodyActive(),
						data.isEnforcerActive(),
						data.isRulerActive(),
						data.hasSage(),
						data.hasHerald(),
						data.isSwordCycling(),
						data.isUnderlordFlying()
				),
				data.getIronBody().name(),
				data.getCurrentWillpower(),
				data.getMaxWillpower()
		);
	}
}

package com.cradle.mod;

import com.cradle.mod.network.CradleSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

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
				data.isIronBodyActive()
		);
	}
}

package com.cradle.mod;

import com.cradle.mod.network.CradleSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.zombie.Zombie;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages revelation trials for Underlord/Overlord/Archlord breakthroughs.
 * When a player attempts to advance to a Lord stage, instead of instantly
 * advancing, hostile spirits spawn that must be defeated.
 *
 * Trial rules:
 * - Underlord: Kill 3 Vex-like spirits
 * - Overlord: Kill 5 Phantom-like spirits
 * - Archlord: Kill 10 pale Zombie-like spirits
 * - No time limit — spirits persist until killed
 * - Player death = trial failed (item already consumed)
 * - Player must stay within 1000 blocks of trial origin
 */
public final class RevelationTrialManager {

	// Maximum distance from trial origin before trial fails
	private static final double MAX_DISTANCE = 1000.0;

	// Active trials indexed by player UUID — one trial per player max
	private static final Map<UUID, RevelationTrial> ACTIVE_TRIALS = new ConcurrentHashMap<>();

	// ── Trial state ───────────────────────────────────────────────────

	public static class RevelationTrial {
		public final UUID playerId;
		public final CradlePlayerData.AdvancementStage targetStage;
		public final List<UUID> spiritIds;
		public final double originX, originY, originZ;

		public RevelationTrial(UUID playerId, CradlePlayerData.AdvancementStage targetStage,
							   double originX, double originY, double originZ) {
			this.playerId = playerId;
			this.targetStage = targetStage;
			this.spiritIds = new ArrayList<>();
			this.originX = originX;
			this.originY = originY;
			this.originZ = originZ;
		}
	}

	// ── Public API ────────────────────────────────────────────────────

	/**
	 * Returns true if the given player currently has an active trial.
	 */
	public static boolean isInTrial(UUID playerId) {
		return ACTIVE_TRIALS.containsKey(playerId);
	}

	/**
	 * Starts a revelation trial for the given player advancing to the given Lord stage.
	 * Spawns the appropriate spirits around the player.
	 */
	public static void startTrial(ServerPlayer player, CradlePlayerData.AdvancementStage targetStage) {
		UUID playerId = player.getUUID();

		// Don't allow multiple concurrent trials
		if (isInTrial(playerId)) {
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cYou are already undergoing a revelation trial!"
			));
			return;
		}

		RevelationTrial trial = new RevelationTrial(
				playerId, targetStage,
				player.getX(), player.getY(), player.getZ()
		);

		if (!(player.level() instanceof ServerLevel serverLevel)) return;

		// Determine spirit count and type based on target stage
		int spiritCount = getSpiritCount(targetStage);
		double radius = 5.0;

		for (int i = 0; i < spiritCount; i++) {
			double angle = (2 * Math.PI * i) / spiritCount;
			double x = player.getX() + radius * Math.cos(angle);
			double z = player.getZ() + radius * Math.sin(angle);
			double y = player.getY();

			Mob spirit = createSpirit(serverLevel, targetStage, x, y, z);
			if (spirit != null) {
				spirit.setTarget(player);
				serverLevel.addFreshEntity(spirit);
				trial.spiritIds.add(spirit.getUUID());
			}
		}

		ACTIVE_TRIALS.put(playerId, trial);

		// Notify player
		player.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A7d\u2694 Your revelation begins... defeat the spirits to prove your worth!"
		));
		player.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A77Kill all " + spiritCount + " spirits to advance to " +
						targetStage.displayName() + "!"
		));

		CradleMod.LOGGER.info("Player {} started {} revelation trial ({} spirits)",
				player.getName().getString(), targetStage.name(), spiritCount);
	}

	/**
	 * Called every server tick to check trial status.
	 */
	public static void onServerTick(MinecraftServer server) {
		if (ACTIVE_TRIALS.isEmpty()) return;

		// Iterate over a copy to avoid ConcurrentModificationException
		for (var entry : new ArrayList<>(ACTIVE_TRIALS.entrySet())) {
			UUID playerId = entry.getKey();
			RevelationTrial trial = entry.getValue();

			// Find the player
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			if (player == null) {
				// Player went offline — cancel trial silently
				cancelTrialCleanup(server, trial);
				ACTIVE_TRIALS.remove(playerId);
				continue;
			}

			// Check distance leash
			double dist = Math.sqrt(
					Math.pow(player.getX() - trial.originX, 2) +
					Math.pow(player.getZ() - trial.originZ, 2)
			);
			if (dist > MAX_DISTANCE) {
				failTrial(player, trial, "You fled too far from your revelation...");
				continue;
			}

			// Count how many spirits are still alive
			ServerLevel serverLevel = player.level();
			int aliveCount = 0;
			for (UUID spiritId : trial.spiritIds) {
				var entity = serverLevel.getEntity(spiritId);
				if (entity != null && entity.isAlive()) {
					aliveCount++;
				}
			}

			// All spirits dead = trial complete!
			if (aliveCount == 0) {
				completeTrial(player, trial);
			}
		}
	}

	/**
	 * Called when any LivingEntity dies. We check if it's a player in a trial.
	 */
	public static void onEntityDeath(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			RevelationTrial trial = ACTIVE_TRIALS.get(player.getUUID());
			if (trial != null) {
				failTrial(player, trial, "Your revelation was incomplete... You must try again.");
			}
		}
	}

	/**
	 * Cancel a trial when a player disconnects. Cleans up spirits.
	 */
	public static void cancelTrial(UUID playerId) {
		RevelationTrial trial = ACTIVE_TRIALS.remove(playerId);
		if (trial != null) {
			CradleMod.LOGGER.info("Cancelling revelation trial for disconnected player {}", playerId);
			// Spirits will be cleaned up on next tick check (they'll lose target and despawn naturally,
			// or we can try to clean them up if we have the server)
		}
	}

	// ── Internal helpers ──────────────────────────────────────────────

	private static int getSpiritCount(CradlePlayerData.AdvancementStage stage) {
		return switch (stage) {
			case UNDERLORD -> 3;
			case OVERLORD -> 5;
			case ARCHLORD -> 10;
			default -> 3;
		};
	}

	/**
	 * Creates and configures a spirit mob for the given trial stage.
	 * Does NOT add it to the world — caller must do that.
	 */
	private static Mob createSpirit(ServerLevel level, CradlePlayerData.AdvancementStage stage,
									double x, double y, double z) {
		return switch (stage) {
			case UNDERLORD -> createVexSpirit(level, x, y, z);
			case OVERLORD -> createPhantomSpirit(level, x, y, z);
			case ARCHLORD -> createZombieSpirit(level, x, y, z);
			default -> null;
		};
	}

	private static Vex createVexSpirit(ServerLevel level, double x, double y, double z) {
		Vex vex = new Vex(EntityType.VEX, level);
		vex.setPos(x, y + 1.0, z); // Vex float, spawn slightly above
		vex.setCustomName(Component.literal("\u00A7dRevelation Spirit"));
		vex.setCustomNameVisible(true);
		// Long-lasting glowing so they're always visible
		vex.addEffect(new MobEffectInstance(MobEffects.GLOWING, 999999, 0, false, false));
		// Make them tougher — fire resistance so they don't die to lava
		vex.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 999999, 0, false, false));
		return vex;
	}

	private static Phantom createPhantomSpirit(ServerLevel level, double x, double y, double z) {
		Phantom phantom = new Phantom(EntityType.PHANTOM, level);
		phantom.setPos(x, y + 3.0, z); // Phantoms fly, spawn above
		phantom.setCustomName(Component.literal("\u00A75Revelation Phantom"));
		phantom.setCustomNameVisible(true);
		phantom.addEffect(new MobEffectInstance(MobEffects.GLOWING, 999999, 0, false, false));
		phantom.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 999999, 0, false, false));
		// Make them a bit tougher
		phantom.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 999999, 0, false, false));
		return phantom;
	}

	private static Zombie createZombieSpirit(ServerLevel level, double x, double y, double z) {
		Zombie zombie = new Zombie(EntityType.ZOMBIE, level);
		zombie.setPos(x, y, z);
		zombie.setCustomName(Component.literal("\u00A78Revelation Wraith"));
		zombie.setCustomNameVisible(true);
		zombie.addEffect(new MobEffectInstance(MobEffects.GLOWING, 999999, 0, false, false));
		zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 999999, 0, false, false));
		zombie.addEffect(new MobEffectInstance(MobEffects.SPEED, 999999, 0, false, false));
		// Prevent them from burning in sunlight — they're spirits, not undead
		zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 999999, 1, false, false));
		// No armor drops — they're spirits
		zombie.setBaby(false);
		return zombie;
	}

	private static void completeTrial(ServerPlayer player, RevelationTrial trial) {
		ACTIVE_TRIALS.remove(trial.playerId);

		CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

		// Perform the actual breakthrough
		BreakthroughManager.performBreakthrough(player, data, trial.targetStage);

		// Save and sync
		CradleMod.autoSave(player.level().getServer());
		ServerPlayNetworking.send(player, CyclingManager.createSyncPayload(player, data));

		CradleMod.LOGGER.info("Player {} completed {} revelation trial",
				player.getName().getString(), trial.targetStage.name());
	}

	private static void failTrial(ServerPlayer player, RevelationTrial trial, String message) {
		ACTIVE_TRIALS.remove(trial.playerId);

		// Despawn remaining spirits
		if (player.level() instanceof ServerLevel serverLevel) {
			for (UUID spiritId : trial.spiritIds) {
				var entity = serverLevel.getEntity(spiritId);
				if (entity != null && entity.isAlive()) {
					entity.discard();
				}
			}
		}

		player.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A7c" + message
		));

		CradleMod.LOGGER.info("Player {} failed {} revelation trial",
				player.getName().getString(), trial.targetStage.name());
	}

	/**
	 * Clean up spirits when a trial is cancelled (e.g., player disconnect).
	 * Tries to find and remove the spirits from the world.
	 */
	private static void cancelTrialCleanup(MinecraftServer server, RevelationTrial trial) {
		for (ServerLevel level : server.getAllLevels()) {
			for (UUID spiritId : trial.spiritIds) {
				var entity = level.getEntity(spiritId);
				if (entity != null) {
					entity.discard();
				}
			}
		}
	}

	/**
	 * Returns true if the given stage requires a revelation trial.
	 */
	public static boolean requiresTrial(CradlePlayerData.AdvancementStage stage) {
		return stage == CradlePlayerData.AdvancementStage.UNDERLORD
				|| stage == CradlePlayerData.AdvancementStage.OVERLORD
				|| stage == CradlePlayerData.AdvancementStage.ARCHLORD;
	}
}

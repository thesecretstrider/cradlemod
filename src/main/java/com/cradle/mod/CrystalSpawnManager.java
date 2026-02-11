package com.cradle.mod;

import com.cradle.mod.block.CradleBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

/**
 * Spawns Iron Body crystal blocks during special world events:
 * - Raindrop Crystal: during rain
 * - Steelborn Crystal: during thunderstorms
 * - Bloodforged Crystal: when a player kills a hostile mob
 *
 * Crystals appear as glowing cross-shaped blocks at the world surface.
 */
public final class CrystalSpawnManager {

	private static final Random RANDOM = new Random();

	// How often to check weather-based spawns (every 100 ticks = 5 seconds)
	private static final int WEATHER_CHECK_INTERVAL = 100;

	// 1 in 500 chance per check during the right weather
	private static final int RAIN_SPAWN_CHANCE = 500;
	private static final int THUNDER_SPAWN_CHANCE = 500;

	// 1 in 200 chance per mob kill
	private static final int KILL_SPAWN_CHANCE = 200;

	// How far from a player crystals can spawn (in blocks)
	private static final int SPAWN_RANGE = 128;

	private static int weatherCheckTicks = 0;

	/**
	 * Called every server tick. Checks weather conditions and possibly spawns crystals.
	 */
	public static void onServerTick(MinecraftServer server) {
		weatherCheckTicks++;
		if (weatherCheckTicks < WEATHER_CHECK_INTERVAL) {
			return;
		}
		weatherCheckTicks = 0;

		for (ServerLevel level : server.getAllLevels()) {
			// Only spawn in the overworld
			if (level.dimension() != ServerLevel.OVERWORLD) {
				continue;
			}

			if (level.players().isEmpty()) {
				continue;
			}

			// Raindrop Crystal — during rain (not thunder, that's for Steelborn)
			if (level.isRaining() && !level.isThundering()) {
				if (RANDOM.nextInt(RAIN_SPAWN_CHANCE) == 0) {
					trySpawnNearRandomPlayer(level, CradleBlocks.RAINDROP_CRYSTAL,
							"\u00A76[Cradle] \u00A7bYou sense a crystalline presence nearby...");
				}
			}

			// Steelborn Crystal — during thunderstorms
			if (level.isThundering()) {
				if (RANDOM.nextInt(THUNDER_SPAWN_CHANCE) == 0) {
					trySpawnNearRandomPlayer(level, CradleBlocks.STEELBORN_CRYSTAL,
							"\u00A76[Cradle] \u00A77You sense a crystalline presence nearby...");
				}
			}
		}
	}

	/**
	 * Called when an entity is killed. Chance to spawn a Bloodforged Crystal near the killer.
	 */
	public static void onEntityKilled(ServerLevel level, Entity attacker, LivingEntity killed) {
		// Only trigger for player kills
		if (!(attacker instanceof ServerPlayer player)) {
			return;
		}

		// Only overworld
		if (level.dimension() != ServerLevel.OVERWORLD) {
			return;
		}

		if (RANDOM.nextInt(KILL_SPAWN_CHANCE) == 0) {
			BlockPos pos = findSurfaceNear(level, player.blockPosition(), 32);
			if (pos != null) {
				level.setBlock(pos, CradleBlocks.BLOODFORGED_CRYSTAL.defaultBlockState(), 3);
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7cYou sense a crystalline presence nearby..."
				));
			}
		}
	}

	/**
	 * Picks a random online player and tries to spawn a crystal block near them.
	 */
	private static void trySpawnNearRandomPlayer(ServerLevel level, Block crystalBlock, String message) {
		var players = level.players();
		if (players.isEmpty()) {
			return;
		}

		ServerPlayer player = players.get(RANDOM.nextInt(players.size()));
		BlockPos pos = findSurfaceNear(level, player.blockPosition(), SPAWN_RANGE);
		if (pos != null) {
			level.setBlock(pos, crystalBlock.defaultBlockState(), 3);
			// Notify all nearby players
			for (ServerPlayer nearby : players) {
				if (nearby.blockPosition().closerThan(pos, SPAWN_RANGE * 2)) {
					nearby.sendSystemMessage(Component.literal(message));
				}
			}
		}
	}

	/**
	 * Finds a valid surface position within range of the given center.
	 * Returns null if no valid position found after a few attempts.
	 */
	private static BlockPos findSurfaceNear(ServerLevel level, BlockPos center, int range) {
		for (int attempt = 0; attempt < 5; attempt++) {
			int x = center.getX() + RANDOM.nextInt(range * 2) - range;
			int z = center.getZ() + RANDOM.nextInt(range * 2) - range;

			// Make sure the chunk is loaded
			if (!level.isLoaded(new BlockPos(x, 0, z))) {
				continue;
			}

			int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
			BlockPos pos = new BlockPos(x, y, z);

			// Check the crystal can survive here (air above dirt/grass)
			if (level.isEmptyBlock(pos) && level.getBlockState(pos.below()).isSolid()) {
				return pos;
			}
		}
		return null;
	}

}

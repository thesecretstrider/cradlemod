package com.cradle.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;

import java.util.Random;

/**
 * Client-side particle spawning while the player is actively cycling.
 *
 * Standard paths: particles spawn on a ring around the player and drift inward
 * (energy being drawn from the world).
 *
 * Hollow King: particles originate from the player and drift outward
 * (pure madra circulates internally, not absorbed from environment).
 *
 * Particle count, radius, and scale increase with advancement stage.
 */
public final class CyclingParticleRenderer {

	private static final Random RANDOM = new Random();
	private static int tickCounter = 0;

	public static void tick(Minecraft client) {
		if (!ClientCradleData.cycling) {
			tickCounter = 0;
			return;
		}

		LocalPlayer player = client.player;
		Level level = client.level;
		if (player == null || level == null) {
			return;
		}

		tickCounter++;

		// Determine spawn rate, radius, and scale based on advancement stage
		int spawnInterval = getSpawnInterval();
		int particlesPerSpawn = getParticlesPerSpawn();
		float radius = getRadius();
		float scale = getScale();

		// Only spawn on the right interval
		if (spawnInterval > 1 && tickCounter % spawnInterval != 0) {
			return;
		}

		int color = ClientCradleData.getPathParticleColor();
		DustParticleOptions options = new DustParticleOptions(color, scale);

		double playerX = player.getX();
		double playerY = player.getY();
		double playerZ = player.getZ();

		boolean isHollowKing = "HOLLOW_KING".equals(ClientCradleData.path);

		for (int i = 0; i < particlesPerSpawn; i++) {
			if (isHollowKing) {
				spawnHollowKingParticle(level, options, playerX, playerY, playerZ);
			} else {
				spawnStandardParticle(level, options, playerX, playerY, playerZ, radius);
			}
		}
	}

	/**
	 * Standard paths: particle spawns on a ring around the player and drifts inward.
	 */
	private static void spawnStandardParticle(Level level, DustParticleOptions options,
			double playerX, double playerY, double playerZ, float radius) {
		double angle = RANDOM.nextDouble() * 2 * Math.PI;
		double spawnX = playerX + radius * Math.cos(angle);
		double spawnZ = playerZ + radius * Math.sin(angle);
		double spawnY = playerY + 0.5 + RANDOM.nextDouble(); // chest to head height

		// Velocity pointing inward toward the player
		double speed = 0.04;
		double vx = (playerX - spawnX) * speed;
		double vz = (playerZ - spawnZ) * speed;
		double vy = (RANDOM.nextDouble() - 0.5) * 0.02; // small random vertical drift

		level.addParticle(options, spawnX, spawnY, spawnZ, vx, vy, vz);
	}

	/**
	 * Hollow King: particle spawns from the player and drifts outward.
	 * Represents pure madra circulation rather than environmental absorption.
	 */
	private static void spawnHollowKingParticle(Level level, DustParticleOptions options,
			double playerX, double playerY, double playerZ) {
		double spawnX = playerX + (RANDOM.nextDouble() - 0.5) * 0.6;
		double spawnY = playerY + 0.8 + RANDOM.nextDouble() * 0.5;
		double spawnZ = playerZ + (RANDOM.nextDouble() - 0.5) * 0.6;

		// Velocity outward from player
		double angle = RANDOM.nextDouble() * 2 * Math.PI;
		double speed = 0.02 + RANDOM.nextDouble() * 0.02;
		double vx = Math.cos(angle) * speed;
		double vz = Math.sin(angle) * speed;
		double vy = 0.01 + RANDOM.nextDouble() * 0.02; // slight upward drift

		level.addParticle(options, spawnX, spawnY, spawnZ, vx, vy, vz);
	}

	// ── Stage-based scaling ──────────────────────────────────────────

	/**
	 * How often particles spawn (in ticks). Lower = more frequent.
	 * Returns 1 for "every tick", 2 for "every 2 ticks", etc.
	 */
	private static int getSpawnInterval() {
		return switch (ClientCradleData.stage) {
			case "GOLD", "JADE" -> 1;
			case "IRON" -> 1;
			case "COPPER" -> 2;
			default -> 4; // Foundation
		};
	}

	/**
	 * How many particles spawn per spawn event.
	 */
	private static int getParticlesPerSpawn() {
		return switch (ClientCradleData.stage) {
			case "GOLD" -> 3;
			case "JADE" -> 2;
			default -> 1; // Iron, Copper, Foundation
		};
	}

	/**
	 * Radius of the spawn ring (for standard paths).
	 */
	private static float getRadius() {
		return switch (ClientCradleData.stage) {
			case "GOLD" -> 4.0f;
			case "JADE" -> 3.5f;
			case "IRON" -> 3.0f;
			case "COPPER" -> 2.5f;
			default -> 2.0f; // Foundation
		};
	}

	/**
	 * Scale of each particle.
	 */
	private static float getScale() {
		return switch (ClientCradleData.stage) {
			case "GOLD" -> 1.2f;
			case "JADE" -> 1.0f;
			case "IRON" -> 0.8f;
			case "COPPER" -> 0.6f;
			default -> 0.4f; // Foundation
		};
	}
}

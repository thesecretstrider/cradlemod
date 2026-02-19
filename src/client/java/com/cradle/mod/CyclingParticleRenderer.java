package com.cradle.mod;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

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
	private static int rulerTickCounter = 0;

	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		Level level = client.level;
		if (player == null || level == null) {
			rulerTickCounter = 0;
			tickCounter = 0;
			return;
		}

		// ── Ruler aura particles ──────────────────────────────────────
		if (ClientCradleData.rulerActive) {
			rulerTickCounter++;
			tickRulerParticles(level, player);
		} else {
			rulerTickCounter = 0;
		}

		// ── Cycling particles ─────────────────────────────────────────
		if (!ClientCradleData.cycling) {
			tickCounter = 0;
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
			case "MONARCH", "HERALD", "SAGE", "ARCHLORD", "OVERLORD", "UNDERLORD" -> 1;
			case "TRUEGOLD", "HIGH_GOLD", "LOW_GOLD", "JADE" -> 1;
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
			case "MONARCH" -> 6;
			case "HERALD", "SAGE" -> 5;
			case "ARCHLORD" -> 4;
			case "OVERLORD", "UNDERLORD" -> 3;
			case "TRUEGOLD", "HIGH_GOLD" -> 3;
			case "LOW_GOLD", "JADE" -> 2;
			default -> 1; // Iron, Copper, Foundation
		};
	}

	/**
	 * Radius of the spawn ring (for standard paths).
	 */
	private static float getRadius() {
		return switch (ClientCradleData.stage) {
			case "MONARCH" -> 6.0f;
			case "HERALD", "SAGE" -> 5.5f;
			case "ARCHLORD" -> 5.0f;
			case "OVERLORD" -> 4.8f;
			case "UNDERLORD" -> 4.5f;
			case "TRUEGOLD" -> 4.0f;
			case "HIGH_GOLD" -> 3.8f;
			case "LOW_GOLD" -> 3.5f;
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
			case "MONARCH" -> 1.8f;
			case "HERALD", "SAGE" -> 1.6f;
			case "ARCHLORD" -> 1.5f;
			case "OVERLORD" -> 1.4f;
			case "UNDERLORD" -> 1.3f;
			case "TRUEGOLD" -> 1.2f;
			case "HIGH_GOLD" -> 1.1f;
			case "LOW_GOLD" -> 1.0f;
			case "JADE" -> 1.0f;
			case "IRON" -> 0.8f;
			case "COPPER" -> 0.6f;
			default -> 0.4f; // Foundation
		};
	}

	// ── Ruler aura particles ─────────────────────────────────────────

	private static final double RULER_RING_RADIUS = 6.0;

	/**
	 * Spawns a rotating ring of path-colored particles around the player
	 * at ground level, showing the Ruler area of effect.
	 */
	private static void tickRulerParticles(Level level, LocalPlayer player) {
		double playerX = player.getX();
		double playerY = player.getY() + 0.1; // just above ground
		double playerZ = player.getZ();

		int color = getRulerParticleColor();
		DustParticleOptions dust = new DustParticleOptions(color, 0.8f);

		// Spawn ring particles — rotating arc each tick for a sweeping effect
		int particlesPerTick = 4;
		double baseAngle = (rulerTickCounter * 0.15) % (2 * Math.PI); // rotates over time

		for (int i = 0; i < particlesPerTick; i++) {
			double angle = baseAngle + (i * 2.0 * Math.PI / particlesPerTick);
			// Add slight randomness so it doesn't look too mechanical
			angle += (RANDOM.nextDouble() - 0.5) * 0.3;

			double x = playerX + RULER_RING_RADIUS * Math.cos(angle);
			double z = playerZ + RULER_RING_RADIUS * Math.sin(angle);

			// Slight upward drift
			level.addParticle(dust, x, playerY, z, 0, 0.02, 0);
		}

		// Inner ring at half radius for a denser look
		if (rulerTickCounter % 2 == 0) {
			double innerAngle = baseAngle + Math.PI; // offset from outer ring
			for (int i = 0; i < 2; i++) {
				double angle = innerAngle + (i * Math.PI);
				angle += (RANDOM.nextDouble() - 0.5) * 0.4;

				double x = playerX + (RULER_RING_RADIUS * 0.5) * Math.cos(angle);
				double z = playerZ + (RULER_RING_RADIUS * 0.5) * Math.sin(angle);

				level.addParticle(dust, x, playerY, z, 0, 0.01, 0);
			}
		}

		// Path-specific accent particles every few ticks
		if (rulerTickCounter % 5 == 0) {
			spawnRulerAccentParticle(level, playerX, playerY, playerZ);
		}
	}

	/**
	 * Spawns path-specific accent particles for Ruler aura flavor.
	 */
	private static void spawnRulerAccentParticle(Level level, double px, double py, double pz) {
		double angle = RANDOM.nextDouble() * 2 * Math.PI;
		double dist = RANDOM.nextDouble() * RULER_RING_RADIUS;
		double x = px + dist * Math.cos(angle);
		double z = pz + dist * Math.sin(angle);
		double y = py + RANDOM.nextDouble() * 0.5;

		switch (ClientCradleData.path) {
			case "BLACK_FLAME" -> level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.03, 0);
			case "ENDLESS_SWORD" -> level.addParticle(ParticleTypes.CRIT, x, y, z, 0, 0.05, 0);
			case "STELLAR_SPEAR" -> level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.04, 0);
			case "CLOUD_HAMMER" -> level.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0.01, 0);
			case "HOLLOW_KING" -> level.addParticle(ParticleTypes.ENCHANT, x, y + 1.0, z, 0, -0.05, 0);
			default -> {}
		}
	}

	/**
	 * Returns the ARGB color for Ruler ring particles based on the current path.
	 */
	private static int getRulerParticleColor() {
		return switch (ClientCradleData.path) {
			case "BLACK_FLAME" -> 0xFFFF4400;   // fiery orange-red
			case "ENDLESS_SWORD" -> 0xFFAADDFF;  // icy light blue
			case "STELLAR_SPEAR" -> 0xFFFFDD44;  // golden
			case "CLOUD_HAMMER" -> 0xFF6666AA;   // stormy purple-grey
			case "HOLLOW_KING" -> 0xFFEEEEFF;    // pale white
			default -> 0xFFC0C0C0;
		};
	}
}

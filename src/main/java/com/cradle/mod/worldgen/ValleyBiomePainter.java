package com.cradle.mod.worldgen;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Assigns terrain zones to positions in Sacred Valley for surface decoration.
 */
public class ValleyBiomePainter {

	public enum Zone {
		MOUNT_SAMARA,   // Central mountain — old growth forest
		FOREST,         // Scattered forest patches on valley floor
		RIVER,          // River corridors through the valley
		PLAINS,         // Open grassland (default valley floor)
		MOUNTAIN_SLOPE, // Mountain ring slopes
		MOUNTAIN_PEAK   // Snow-capped mountain tops
	}

	private static final SimplexNoise ZONE_NOISE = new SimplexNoise(new LegacyRandomSource(42424242L));
	private static final SimplexNoise RIVER_NOISE = new SimplexNoise(new LegacyRandomSource(98765L));

	/**
	 * Determines the terrain zone at a given world position.
	 */
	public static Zone getZone(int blockX, int blockZ) {
		double dist = Math.sqrt(blockX * blockX + blockZ * blockZ);

		// Mountain peaks
		int surfaceY = ValleyHeightmap.getHeight(blockX, blockZ);
		if (surfaceY > 150) return Zone.MOUNTAIN_PEAK;
		if (surfaceY > 100) return Zone.MOUNTAIN_SLOPE;

		// Mount Samara
		if (dist < ValleyHeightmap.MOUNT_SAMARA_RADIUS + 30) return Zone.MOUNT_SAMARA;

		// Rivers — sinusoidal corridors radiating from center
		if (isRiver(blockX, blockZ, dist)) return Zone.RIVER;

		// Forest patches — noise-driven
		double forestNoise = ZONE_NOISE.getValue(blockX * 0.008, blockZ * 0.008);
		if (forestNoise > 0.3) return Zone.FOREST;

		return Zone.PLAINS;
	}

	/**
	 * Check if position is in a river corridor. Rivers radiate outward from
	 * Mount Samara in 4 directions with sinusoidal meandering.
	 */
	private static boolean isRiver(int x, int z, double dist) {
		if (dist < ValleyHeightmap.MOUNT_SAMARA_RADIUS + 50) return false;
		if (dist > ValleyHeightmap.VALLEY_RADIUS - ValleyHeightmap.MOUNTAIN_RING_WIDTH) return false;

		// Four river corridors at roughly 45/135/225/315 degrees
		double angle = Math.atan2(z, x);
		double[] riverAngles = {Math.PI / 4, 3 * Math.PI / 4, -Math.PI / 4, -3 * Math.PI / 4};

		for (double ra : riverAngles) {
			double angleDiff = Math.abs(normalizeAngle(angle - ra));
			// River width narrows with distance, plus noise for meandering
			double meander = RIVER_NOISE.getValue(x * 0.01, z * 0.01) * 15;
			double riverWidth = 4 + meander;
			// Convert angle difference to block distance at this radius
			double blockDiff = angleDiff * dist;
			if (blockDiff < Math.abs(riverWidth)) return true;
		}
		return false;
	}

	private static double normalizeAngle(double angle) {
		while (angle > Math.PI) angle -= 2 * Math.PI;
		while (angle < -Math.PI) angle += 2 * Math.PI;
		return angle;
	}

	/**
	 * Returns true if a tree should be placed at this position.
	 * Uses deterministic noise so placement is consistent.
	 */
	public static boolean shouldPlaceTree(int blockX, int blockZ, Zone zone) {
		double density;
		switch (zone) {
			case MOUNT_SAMARA -> density = 0.15;
			case FOREST -> density = 0.08;
			case PLAINS -> density = 0.005;
			default -> { return false; }
		}
		// Hash-based pseudo-random placement
		int hash = (blockX * 73856093) ^ (blockZ * 19349663);
		double roll = ((hash & 0x7FFFFFFF) % 10000) / 10000.0;
		return roll < density;
	}

	/**
	 * Returns true if tall grass or flowers should be placed.
	 */
	public static boolean shouldPlaceGrass(int blockX, int blockZ, Zone zone) {
		if (zone == Zone.RIVER || zone == Zone.MOUNTAIN_PEAK || zone == Zone.MOUNTAIN_SLOPE) return false;
		int hash = (blockX * 12345) ^ (blockZ * 67890);
		double roll = ((hash & 0x7FFFFFFF) % 100) / 100.0;
		return roll < 0.3;
	}

	/**
	 * Returns true if this should be a flower instead of grass.
	 */
	public static boolean shouldBeFlower(int blockX, int blockZ) {
		int hash = (blockX * 54321) ^ (blockZ * 98765);
		return ((hash & 0x7FFFFFFF) % 10) < 2; // 20% of grass is flowers
	}
}

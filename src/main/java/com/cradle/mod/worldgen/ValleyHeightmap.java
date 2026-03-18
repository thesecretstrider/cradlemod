package com.cradle.mod.worldgen;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Computes terrain height for Sacred Valley — a bowl-shaped valley with
 * Mount Samara at center and a mountain ring around the perimeter.
 */
public class ValleyHeightmap {
	public static final int VALLEY_CENTER_X = 0;
	public static final int VALLEY_CENTER_Z = 0;
	public static final int VALLEY_RADIUS = 750;
	public static final int MOUNTAIN_RING_WIDTH = 200;
	public static final int VALLEY_FLOOR_Y = 68;
	public static final int MOUNTAIN_PEAK_Y = 180;
	public static final int MOUNT_SAMARA_RADIUS = 80;
	public static final int MOUNT_SAMARA_PEAK_Y = 180;

	private static final SimplexNoise NOISE = new SimplexNoise(new LegacyRandomSource(7777L));

	/**
	 * Returns surface height at the given world coordinates.
	 */
	public static int getHeight(int blockX, int blockZ) {
		double dx = blockX - VALLEY_CENTER_X;
		double dz = blockZ - VALLEY_CENTER_Z;
		double distFromCenter = Math.sqrt(dx * dx + dz * dz);

		// Mount Samara at center — smooth cone
		if (distFromCenter < MOUNT_SAMARA_RADIUS) {
			double t = distFromCenter / MOUNT_SAMARA_RADIUS;
			// Smooth hermite curve for natural mountain shape
			double samaraFactor = 1.0 - (3 * t * t - 2 * t * t * t);
			int samaraHeight = (int) (samaraFactor * (MOUNT_SAMARA_PEAK_Y - VALLEY_FLOOR_Y));
			return VALLEY_FLOOR_Y + samaraHeight + gentleHills(blockX, blockZ, 0.3);
		}

		int innerEdge = VALLEY_RADIUS - MOUNTAIN_RING_WIDTH;

		// Valley floor — gentle rolling hills
		if (distFromCenter < innerEdge) {
			return VALLEY_FLOOR_Y + gentleHills(blockX, blockZ, 1.0);
		}

		// Mountain ring — rises from valley floor to peak
		double ringProgress = (distFromCenter - innerEdge) / MOUNTAIN_RING_WIDTH;
		ringProgress = Math.min(1.0, ringProgress);
		// Smoothstep for natural slope
		double smooth = ringProgress * ringProgress * (3 - 2 * ringProgress);
		int mountainHeight = (int) (smooth * (MOUNTAIN_PEAK_Y - VALLEY_FLOOR_Y));
		// Add some noise to mountain slopes for variety
		int noise = (int) (NOISE.getValue(blockX * 0.02, blockZ * 0.02) * 8);
		return VALLEY_FLOOR_Y + mountainHeight + noise;
	}

	/**
	 * Gentle rolling hills — 0 to ~12 blocks of variation.
	 */
	private static int gentleHills(int x, int z, double amplitude) {
		// Layer two octaves for natural-looking terrain
		double n1 = NOISE.getValue(x * 0.005, z * 0.005) * 8;
		double n2 = NOISE.getValue(x * 0.02, z * 0.02) * 3;
		return (int) ((n1 + n2) * amplitude);
	}
}

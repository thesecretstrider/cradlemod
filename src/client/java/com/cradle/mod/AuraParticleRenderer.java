package com.cradle.mod;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client-side ambient particle renderer for Copper Sight.
 * When active, spawns colored aura lines rising from the ground near the player,
 * like veins of vital aura energy flowing upward through the earth.
 *
 * Only renders when:
 * 1. The player is at Copper stage or higher
 * 2. Copper Sight is toggled on (H key)
 *
 * Particles are purely cosmetic — no server interaction.
 * Biome aura type is cached and rechecked every 20 ticks (1 second)
 * to avoid unnecessary biome lookups.
 */
public final class AuraParticleRenderer {

	private static final Random RANDOM = new Random();

	// How far from the player aura lines spawn (in blocks)
	private static final double SPAWN_RADIUS = 14.0;
	// Number of new aura lines to start per tick
	private static final int LINES_PER_TICK = 2;
	// Number of particles stacked vertically per line
	private static final int PARTICLES_PER_LINE = 5;
	// Vertical spacing between particles in a line (blocks)
	private static final double LINE_SPACING = 0.6;
	// Size of aura particles (smaller = more wispy/line-like)
	private static final float PARTICLE_SCALE = 0.45f;
	// Biome cache update interval (ticks)
	private static final int BIOME_CHECK_INTERVAL = 20;
	// Max distance below player to search for ground
	private static final int GROUND_SEARCH_DEPTH = 8;

	// Cached state
	private static int cachedAuraColor = 0xFFA0825A; // default Earth (brown)
	private static String cachedAuraName = "Earth Aura";
	private static String cachedBiomeName = ""; // actual biome name for info screen
	private static int tickCounter = 0;

	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		Level level = client.level;
		if (player == null || level == null) {
			tickCounter = 0;
			return;
		}

		// Only render when Copper Sight is active
		if (!ClientCradleData.copperSightActive) {
			tickCounter = 0;
			return;
		}

		tickCounter++;

		// Recache biome aura every second
		if (tickCounter % BIOME_CHECK_INTERVAL == 1) {
			Holder<Biome> biomeHolder = level.getBiome(player.blockPosition());
			VitalAura aura = VitalAura.getAuraForBiome(biomeHolder);
			cachedAuraColor = aura.getColor();
			cachedAuraName = aura.getDisplayName();
			cachedBiomeName = biomeHolder.unwrapKey()
					.map(key -> key.identifier().getPath().replace("_", " "))
					.orElse("unknown");
		}

		double px = player.getX();
		double py = player.getY();
		double pz = player.getZ();

		for (int line = 0; line < LINES_PER_TICK; line++) {
			// Pick a random XZ position around the player
			double dx = (RANDOM.nextDouble() - 0.5) * 2.0 * SPAWN_RADIUS;
			double dz = (RANDOM.nextDouble() - 0.5) * 2.0 * SPAWN_RADIUS;

			double spawnX = px + dx;
			double spawnZ = pz + dz;

			// Find the ground level at this position
			double groundY = findGroundLevel(level, spawnX, py, spawnZ);

			// Slight random offset so lines don't always start at exact block tops
			double baseY = groundY + RANDOM.nextDouble() * 0.3;

			// Slightly vary color brightness per line for visual variety
			float brightnessVariance = 0.85f + RANDOM.nextFloat() * 0.3f;
			int lineColor = varyBrightness(cachedAuraColor, brightnessVariance);

			// Slightly vary particle scale per line
			float lineScale = PARTICLE_SCALE + (RANDOM.nextFloat() - 0.5f) * 0.15f;
			DustParticleOptions options = new DustParticleOptions(lineColor, Math.max(0.2f, lineScale));

			// Spawn a vertical column of particles rising from the ground
			for (int i = 0; i < PARTICLES_PER_LINE; i++) {
				double particleY = baseY + i * LINE_SPACING;

				// Small horizontal jitter to make the line look organic, not perfectly straight
				double jitterX = (RANDOM.nextDouble() - 0.5) * 0.15;
				double jitterZ = (RANDOM.nextDouble() - 0.5) * 0.15;

				// Strong upward velocity — particles streak upward
				// Lower particles move faster, upper ones drift (creates a tapering effect)
				double vy = 0.03 + (PARTICLES_PER_LINE - i) * 0.008;
				// Minimal horizontal drift
				double vx = (RANDOM.nextDouble() - 0.5) * 0.003;
				double vz = (RANDOM.nextDouble() - 0.5) * 0.003;

				level.addParticle(options,
						spawnX + jitterX, particleY, spawnZ + jitterZ,
						vx, vy, vz);
			}
		}
	}

	/**
	 * Finds the Y level of the topmost solid block at the given XZ position,
	 * searching downward from the player's Y level. Returns the top of the block.
	 */
	private static double findGroundLevel(Level level, double x, double playerY, double z) {
		int bx = (int) Math.floor(x);
		int bz = (int) Math.floor(z);
		int startY = (int) Math.floor(playerY) + 2; // Start slightly above player

		for (int dy = 0; dy <= GROUND_SEARCH_DEPTH + 4; dy++) {
			int checkY = startY - dy;
			BlockPos pos = new BlockPos(bx, checkY, bz);
			BlockState state = level.getBlockState(pos);

			// Found a solid block — return the top of it
			if (state.blocksMotion()) {
				return checkY + 1.0;
			}
		}

		// No ground found — default to player Y minus a few blocks
		return playerY - 2.0;
	}

	/**
	 * Adjusts the brightness of an ARGB color by a multiplier.
	 */
	private static int varyBrightness(int argb, float multiplier) {
		int a = (argb >> 24) & 0xFF;
		int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * multiplier));
		int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * multiplier));
		int b = Math.min(255, (int) ((argb & 0xFF) * multiplier));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/**
	 * Returns the cached aura display name for the current biome.
	 * Used by the info screen to show what aura type the player is in.
	 */
	public static String getCachedAuraName() {
		return cachedAuraName;
	}

	/**
	 * Returns the cached aura color for HUD rendering.
	 */
	public static int getCachedAuraColor() {
		return cachedAuraColor;
	}

	/**
	 * Returns the actual biome name (e.g., "desert", "plains") for debug display.
	 */
	public static String getCachedBiomeName() {
		return cachedBiomeName;
	}
}

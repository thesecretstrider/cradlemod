package com.cradle.mod;

import java.util.HashMap;
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
 * When active, renders two types of aura particles:
 *
 * 1. BIOME AURA RIPPLES — lines of particles at ground level that radiate
 *    outward from the player, colored by the biome's dominant aura type.
 *    Like ripples of energy spreading along the ground.
 *
 * 2. SOURCE AURA EMISSIONS — specific blocks (fire, water, flowers, etc.)
 *    emit their matching aura type. Fire emits FIRE aura, water emits WATER
 *    aura, etc. These rise from the source block itself.
 *
 * Only renders when:
 * 1. The player is at Copper stage or higher
 * 2. Copper Sight is toggled on (H key)
 *
 * Particles are purely cosmetic — no server interaction.
 */
public final class AuraParticleRenderer {

	private static final Random RANDOM = new Random();

	// ── Biome aura ripple settings ──
	private static final int RIPPLE_LINES_PER_TICK = 1;
	private static final int RIPPLE_PARTICLES_PER_LINE = 4;
	private static final double RIPPLE_MAX_DISTANCE = 14.0;
	private static final float RIPPLE_PARTICLE_SCALE = 0.4f;

	// ── Source aura emission settings ──
	private static final int SOURCE_SCAN_RADIUS = 10;
	private static final int SOURCE_SCANS_PER_TICK = 1;
	private static final int SOURCE_PARTICLES = 3;
	private static final float SOURCE_PARTICLE_SCALE = 0.55f;

	// ── General settings ──
	private static final int BIOME_CHECK_INTERVAL = 20;
	private static final int GROUND_SEARCH_DEPTH = 8;

	// Cached state
	private static int cachedAuraColor = 0xFFA0825A;
	private static String cachedAuraName = "Earth Aura";
	private static String cachedBiomeName = "";
	private static int tickCounter = 0;

	// Performance: cache ground levels to avoid repeated block searches
	private static final HashMap<Long, Integer> groundLevelCache = new HashMap<>();
	private static int groundCacheTicks = 0;
	private static final int GROUND_CACHE_CLEAR_INTERVAL = 40; // Clear cache every 2 seconds

	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		Level level = client.level;
		if (player == null || level == null) {
			tickCounter = 0;
			return;
		}

		if (!ClientCradleData.copperSightActive) {
			tickCounter = 0;
			return;
		}

		// Clear ground level cache periodically
		groundCacheTicks++;
		if (groundCacheTicks >= GROUND_CACHE_CLEAR_INTERVAL) {
			groundCacheTicks = 0;
			groundLevelCache.clear();
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

		// ── 1. Biome aura ripples radiating outward from the player ──
		spawnBiomeRipples(level, px, py, pz);

		// ── 2. Source aura emissions from nearby blocks ──
		spawnSourceEmissions(level, px, py, pz);
	}

	/**
	 * Spawns lines of particles at ground level radiating outward from the player.
	 * Each line is a trail of particles along the ground in a random direction.
	 */
	private static void spawnBiomeRipples(Level level, double px, double py, double pz) {
		for (int line = 0; line < RIPPLE_LINES_PER_TICK; line++) {
			// Pick a random direction angle from the player
			double angle = RANDOM.nextDouble() * Math.PI * 2.0;
			double dirX = Math.cos(angle);
			double dirZ = Math.sin(angle);

			// Brightness variation per line
			float brightness = 0.85f + RANDOM.nextFloat() * 0.3f;
			int lineColor = varyBrightness(cachedAuraColor, brightness);
			float scale = RIPPLE_PARTICLE_SCALE + (RANDOM.nextFloat() - 0.5f) * 0.1f;
			DustParticleOptions options = new DustParticleOptions(lineColor, Math.max(0.2f, scale));

			// Spawn particles along the line, starting near the player and going outward
			for (int i = 0; i < RIPPLE_PARTICLES_PER_LINE; i++) {
				// Distance from player increases with each particle
				double dist = 1.5 + (RIPPLE_MAX_DISTANCE - 1.5) * ((double) i / RIPPLE_PARTICLES_PER_LINE);
				// Add some spread so lines aren't perfectly straight
				double spread = (RANDOM.nextDouble() - 0.5) * 0.4;

				double spawnX = px + dirX * dist + (-dirZ) * spread;
				double spawnZ = pz + dirZ * dist + dirX * spread;

				// Find ground level at this point
				int groundY = findGroundLevel(level, (int) Math.floor(spawnX), (int) Math.floor(spawnZ), (int) Math.floor(py) + 2);
				double spawnY = groundY + 1.0 + 0.05 + RANDOM.nextDouble() * 0.15;

				// Velocity: outward along the ground + slight upward drift
				double speed = 0.015 + RANDOM.nextDouble() * 0.01;
				double vx = dirX * speed;
				double vz = dirZ * speed;
				double vy = 0.002 + RANDOM.nextDouble() * 0.005;

				level.addParticle(options, spawnX, spawnY, spawnZ, vx, vy, vz);
			}
		}
	}

	/**
	 * Scans random nearby blocks for aura sources and spawns colored particles
	 * rising from them. Fire blocks emit FIRE aura, water emits WATER, etc.
	 */
	private static void spawnSourceEmissions(Level level, double px, double py, double pz) {
		int playerBX = (int) Math.floor(px);
		int playerBY = (int) Math.floor(py);
		int playerBZ = (int) Math.floor(pz);

		for (int scan = 0; scan < SOURCE_SCANS_PER_TICK; scan++) {
			// Pick a random block within scan radius
			int dx = RANDOM.nextInt(SOURCE_SCAN_RADIUS * 2 + 1) - SOURCE_SCAN_RADIUS;
			int dy = RANDOM.nextInt(7) - 3; // check +-3 vertically
			int dz = RANDOM.nextInt(SOURCE_SCAN_RADIUS * 2 + 1) - SOURCE_SCAN_RADIUS;

			BlockPos pos = new BlockPos(playerBX + dx, playerBY + dy, playerBZ + dz);
			BlockState state = level.getBlockState(pos);

			VitalAura sourceAura = VitalAura.getAuraForBlock(state);
			if (sourceAura == null) continue;

			// Found an aura source! Spawn particles rising from it
			int color = sourceAura.getColor();
			float brightness = 0.9f + RANDOM.nextFloat() * 0.2f;
			int sourceColor = varyBrightness(color, brightness);
			DustParticleOptions options = new DustParticleOptions(sourceColor, SOURCE_PARTICLE_SCALE);

			double blockX = pos.getX() + 0.5;
			double blockY = pos.getY() + 0.8;
			double blockZ = pos.getZ() + 0.5;

			for (int i = 0; i < SOURCE_PARTICLES; i++) {
				double sx = blockX + (RANDOM.nextDouble() - 0.5) * 0.6;
				double sy = blockY + i * 0.4 + RANDOM.nextDouble() * 0.2;
				double sz = blockZ + (RANDOM.nextDouble() - 0.5) * 0.6;

				// Rise upward with slight wander
				double vx = (RANDOM.nextDouble() - 0.5) * 0.005;
				double vy = 0.02 + RANDOM.nextDouble() * 0.015;
				double vz = (RANDOM.nextDouble() - 0.5) * 0.005;

				level.addParticle(options, sx, sy, sz, vx, vy, vz);
			}
		}
	}

	private static int findGroundLevel(Level level, int x, int z, int startY) {
		long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
		Integer cached = groundLevelCache.get(key);
		if (cached != null) return cached;

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, startY, z);
		int minY = startY - GROUND_SEARCH_DEPTH - 4;
		for (int y = startY; y >= minY; y--) {
			pos.setY(y);
			if (!level.getBlockState(pos).isAir()) {
				groundLevelCache.put(key, y);
				return y;
			}
		}
		groundLevelCache.put(key, minY);
		return minY;
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

	/** Returns the cached aura display name for the current biome. */
	public static String getCachedAuraName() {
		return cachedAuraName;
	}

	/** Returns the cached aura color for HUD rendering. */
	public static int getCachedAuraColor() {
		return cachedAuraColor;
	}

	/** Returns the actual biome name for debug display. */
	public static String getCachedBiomeName() {
		return cachedBiomeName;
	}
}

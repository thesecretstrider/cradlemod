package com.cradle.mod;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The 7 types of vital aura that saturate the world in the Cradle universe.
 * Each biome has a dominant aura type that can be seen with Copper Sight
 * and provides cycling bonuses to aligned sacred artists.
 */
public enum VitalAura {

	FIRE(0xFFFF6600, "Fire Aura"),
	EARTH(0xFFA0825A, "Earth Aura"),
	WIND(0xFFCCCCDD, "Wind Aura"),
	WATER(0xFF3399FF, "Water Aura"),
	FORCE(0xFF9933FF, "Force Aura"),
	BLOOD(0xFFCC2222, "Blood Aura"),
	LIFE(0xFF33CC66, "Life Aura");

	private final int color;
	private final String displayName;

	VitalAura(int color, String displayName) {
		this.color = color;
		this.displayName = displayName;
	}

	public int getColor() { return color; }
	public String getDisplayName() { return displayName; }

	// ── Path-to-aura alignment ─────────────────────────────────
	// Returns the set of aura types that give a cycling bonus to a given path.
	// Black Flame benefits from fire aura; Cloud Hammer from wind aura.
	// Sword paths use sword cycling (2x) instead. Hollow King has no env bonus.

	public static Set<VitalAura> getAlignedAuras(CradlePlayerData.Path path) {
		return switch (path) {
			case BLACK_FLAME -> Set.of(FIRE);
			case CLOUD_HAMMER -> Set.of(WIND);
			// Endless Sword & Stellar Spear use sword cycling bonus, not aura alignment
			// Hollow King has no environmental bonus (pure madra, internal only)
			default -> Set.of();
		};
	}

	// ── Biome-to-aura mapping ──────────────────────────────────
	// Unmapped biomes default to EARTH (the most common aura type).

	private static final Map<ResourceKey<Biome>, VitalAura> BIOME_AURA = new HashMap<>();

	static {
		// FIRE biomes — hot, volcanic, fiery
		BIOME_AURA.put(Biomes.DESERT, FIRE);
		BIOME_AURA.put(Biomes.BADLANDS, FIRE);
		BIOME_AURA.put(Biomes.ERODED_BADLANDS, FIRE);
		BIOME_AURA.put(Biomes.WOODED_BADLANDS, FIRE);
		BIOME_AURA.put(Biomes.NETHER_WASTES, FIRE);
		BIOME_AURA.put(Biomes.SOUL_SAND_VALLEY, FIRE);
		BIOME_AURA.put(Biomes.CRIMSON_FOREST, FIRE);
		BIOME_AURA.put(Biomes.WARPED_FOREST, FIRE);
		BIOME_AURA.put(Biomes.BASALT_DELTAS, FIRE);

		// WATER biomes — oceans, rivers, frozen, snowy
		BIOME_AURA.put(Biomes.OCEAN, WATER);
		BIOME_AURA.put(Biomes.DEEP_OCEAN, WATER);
		BIOME_AURA.put(Biomes.WARM_OCEAN, WATER);
		BIOME_AURA.put(Biomes.LUKEWARM_OCEAN, WATER);
		BIOME_AURA.put(Biomes.COLD_OCEAN, WATER);
		BIOME_AURA.put(Biomes.DEEP_COLD_OCEAN, WATER);
		BIOME_AURA.put(Biomes.DEEP_LUKEWARM_OCEAN, WATER);
		BIOME_AURA.put(Biomes.FROZEN_OCEAN, WATER);
		BIOME_AURA.put(Biomes.DEEP_FROZEN_OCEAN, WATER);
		BIOME_AURA.put(Biomes.RIVER, WATER);
		BIOME_AURA.put(Biomes.FROZEN_RIVER, WATER);
		BIOME_AURA.put(Biomes.BEACH, WATER);
		BIOME_AURA.put(Biomes.SNOWY_BEACH, WATER);
		BIOME_AURA.put(Biomes.SWAMP, WATER);
		BIOME_AURA.put(Biomes.STONY_SHORE, WATER);
		BIOME_AURA.put(Biomes.FROZEN_PEAKS, WATER);
		BIOME_AURA.put(Biomes.ICE_SPIKES, WATER);
		BIOME_AURA.put(Biomes.SNOWY_PLAINS, WATER);
		BIOME_AURA.put(Biomes.SNOWY_TAIGA, WATER);
		BIOME_AURA.put(Biomes.SNOWY_SLOPES, WATER);

		// WIND biomes — high altitude, windswept, exposed
		BIOME_AURA.put(Biomes.WINDSWEPT_HILLS, WIND);
		BIOME_AURA.put(Biomes.WINDSWEPT_GRAVELLY_HILLS, WIND);
		BIOME_AURA.put(Biomes.WINDSWEPT_FOREST, WIND);
		BIOME_AURA.put(Biomes.WINDSWEPT_SAVANNA, WIND);
		BIOME_AURA.put(Biomes.JAGGED_PEAKS, WIND);
		BIOME_AURA.put(Biomes.STONY_PEAKS, WIND);
		BIOME_AURA.put(Biomes.MEADOW, WIND);

		// FORCE biomes — otherworldly, void, deep power
		BIOME_AURA.put(Biomes.THE_END, FORCE);
		BIOME_AURA.put(Biomes.END_HIGHLANDS, FORCE);
		BIOME_AURA.put(Biomes.END_MIDLANDS, FORCE);
		BIOME_AURA.put(Biomes.END_BARRENS, FORCE);
		BIOME_AURA.put(Biomes.SMALL_END_ISLANDS, FORCE);
		BIOME_AURA.put(Biomes.DEEP_DARK, FORCE);

		// BLOOD biomes — dense organic life, predatory ecosystems
		BIOME_AURA.put(Biomes.JUNGLE, BLOOD);
		BIOME_AURA.put(Biomes.SPARSE_JUNGLE, BLOOD);
		BIOME_AURA.put(Biomes.BAMBOO_JUNGLE, BLOOD);
		BIOME_AURA.put(Biomes.MUSHROOM_FIELDS, BLOOD);
		BIOME_AURA.put(Biomes.MANGROVE_SWAMP, BLOOD);
		BIOME_AURA.put(Biomes.DARK_FOREST, BLOOD);

		// LIFE biomes — lush, fertile, flowering, nurturing
		BIOME_AURA.put(Biomes.FLOWER_FOREST, LIFE);
		BIOME_AURA.put(Biomes.LUSH_CAVES, LIFE);
		BIOME_AURA.put(Biomes.SUNFLOWER_PLAINS, LIFE);
		BIOME_AURA.put(Biomes.CHERRY_GROVE, LIFE);
		BIOME_AURA.put(Biomes.GROVE, LIFE);

		// EARTH biomes — common, grounded (also the default fallback)
		BIOME_AURA.put(Biomes.PLAINS, EARTH);
		BIOME_AURA.put(Biomes.FOREST, EARTH);
		BIOME_AURA.put(Biomes.BIRCH_FOREST, EARTH);
		BIOME_AURA.put(Biomes.OLD_GROWTH_BIRCH_FOREST, EARTH);
		BIOME_AURA.put(Biomes.TAIGA, EARTH);
		BIOME_AURA.put(Biomes.OLD_GROWTH_PINE_TAIGA, EARTH);
		BIOME_AURA.put(Biomes.OLD_GROWTH_SPRUCE_TAIGA, EARTH);
		BIOME_AURA.put(Biomes.SAVANNA, EARTH);
		BIOME_AURA.put(Biomes.SAVANNA_PLATEAU, EARTH);
		BIOME_AURA.put(Biomes.DRIPSTONE_CAVES, EARTH);
	}

	/**
	 * Returns the dominant aura type for a biome.
	 * Falls back to EARTH for any unmapped biome (modded biomes, etc.).
	 *
	 * Uses location-based lookup as a fallback in case the ResourceKey instances
	 * don't match between client and server registries.
	 */
	public static VitalAura getAuraForBiome(Holder<Biome> biomeHolder) {
		return biomeHolder.unwrapKey()
				.map(key -> {
					// Direct ResourceKey match (fastest)
					VitalAura result = BIOME_AURA.get(key);
					if (result != null) return result;
					// Fallback: match by location string (handles client/server key mismatch)
					String loc = key.identifier().toString();
					for (Map.Entry<ResourceKey<Biome>, VitalAura> entry : BIOME_AURA.entrySet()) {
						if (entry.getKey().identifier().toString().equals(loc)) {
							return entry.getValue();
						}
					}
					return EARTH;
				})
				.orElse(EARTH);
	}
}

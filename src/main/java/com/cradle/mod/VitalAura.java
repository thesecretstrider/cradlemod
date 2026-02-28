package com.cradle.mod;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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

	// ── Block-to-aura source mapping ──────────────────────────
	// Specific blocks that emit their aura type when Copper Sight is active.
	// Used by the particle renderer to show aura emanating from sources.

	private static final Map<Block, VitalAura> BLOCK_AURA = new HashMap<>();

	static {
		// FIRE sources — heat, flame, lava
		BLOCK_AURA.put(Blocks.FIRE, FIRE);
		BLOCK_AURA.put(Blocks.SOUL_FIRE, FIRE);
		BLOCK_AURA.put(Blocks.LAVA, FIRE);
		BLOCK_AURA.put(Blocks.MAGMA_BLOCK, FIRE);
		BLOCK_AURA.put(Blocks.CAMPFIRE, FIRE);
		BLOCK_AURA.put(Blocks.SOUL_CAMPFIRE, FIRE);
		BLOCK_AURA.put(Blocks.FURNACE, FIRE);
		BLOCK_AURA.put(Blocks.BLAST_FURNACE, FIRE);
		BLOCK_AURA.put(Blocks.SMOKER, FIRE);
		BLOCK_AURA.put(Blocks.TORCH, FIRE);
		BLOCK_AURA.put(Blocks.WALL_TORCH, FIRE);
		BLOCK_AURA.put(Blocks.SOUL_TORCH, FIRE);
		BLOCK_AURA.put(Blocks.SOUL_WALL_TORCH, FIRE);
		BLOCK_AURA.put(Blocks.LAVA_CAULDRON, FIRE);

		// WATER sources — water, ice, packed ice
		BLOCK_AURA.put(Blocks.WATER, WATER);
		BLOCK_AURA.put(Blocks.ICE, WATER);
		BLOCK_AURA.put(Blocks.PACKED_ICE, WATER);
		BLOCK_AURA.put(Blocks.BLUE_ICE, WATER);
		BLOCK_AURA.put(Blocks.SNOW_BLOCK, WATER);
		BLOCK_AURA.put(Blocks.POWDER_SNOW, WATER);
		BLOCK_AURA.put(Blocks.WATER_CAULDRON, WATER);
		BLOCK_AURA.put(Blocks.FROSTED_ICE, WATER);

		// WIND sources — air-associated blocks (exposed sky blocks handled by tag)
		// Wind doesn't have many specific blocks, it comes from biomes mostly

		// LIFE sources — flowers, crops, growing things
		BLOCK_AURA.put(Blocks.DANDELION, LIFE);
		BLOCK_AURA.put(Blocks.POPPY, LIFE);
		BLOCK_AURA.put(Blocks.BLUE_ORCHID, LIFE);
		BLOCK_AURA.put(Blocks.ALLIUM, LIFE);
		BLOCK_AURA.put(Blocks.AZURE_BLUET, LIFE);
		BLOCK_AURA.put(Blocks.OXEYE_DAISY, LIFE);
		BLOCK_AURA.put(Blocks.CORNFLOWER, LIFE);
		BLOCK_AURA.put(Blocks.LILY_OF_THE_VALLEY, LIFE);
		BLOCK_AURA.put(Blocks.SUNFLOWER, LIFE);
		BLOCK_AURA.put(Blocks.LILAC, LIFE);
		BLOCK_AURA.put(Blocks.ROSE_BUSH, LIFE);
		BLOCK_AURA.put(Blocks.PEONY, LIFE);
		BLOCK_AURA.put(Blocks.FLOWERING_AZALEA, LIFE);
		BLOCK_AURA.put(Blocks.FLOWERING_AZALEA_LEAVES, LIFE);
		BLOCK_AURA.put(Blocks.SPORE_BLOSSOM, LIFE);
		BLOCK_AURA.put(Blocks.MOSS_BLOCK, LIFE);
		BLOCK_AURA.put(Blocks.BEE_NEST, LIFE);
		BLOCK_AURA.put(Blocks.BEEHIVE, LIFE);

		// BLOOD sources — flesh, bone, predatory
		BLOCK_AURA.put(Blocks.BONE_BLOCK, BLOOD);
		BLOCK_AURA.put(Blocks.CRIMSON_NYLIUM, BLOOD);
		BLOCK_AURA.put(Blocks.CRIMSON_STEM, BLOOD);
		BLOCK_AURA.put(Blocks.NETHER_WART_BLOCK, BLOOD);
		BLOCK_AURA.put(Blocks.SHROOMLIGHT, BLOOD);
		BLOCK_AURA.put(Blocks.SCULK, BLOOD);
		BLOCK_AURA.put(Blocks.SCULK_CATALYST, BLOOD);
		BLOCK_AURA.put(Blocks.SCULK_SHRIEKER, BLOOD);
		BLOCK_AURA.put(Blocks.SCULK_SENSOR, BLOOD);

		// FORCE sources — end-related, enchanting, arcane
		BLOCK_AURA.put(Blocks.END_STONE, FORCE);
		BLOCK_AURA.put(Blocks.END_STONE_BRICKS, FORCE);
		BLOCK_AURA.put(Blocks.PURPUR_BLOCK, FORCE);
		BLOCK_AURA.put(Blocks.PURPUR_PILLAR, FORCE);
		BLOCK_AURA.put(Blocks.ENCHANTING_TABLE, FORCE);
		BLOCK_AURA.put(Blocks.END_ROD, FORCE);
		BLOCK_AURA.put(Blocks.OBSIDIAN, FORCE);
		BLOCK_AURA.put(Blocks.CRYING_OBSIDIAN, FORCE);
		BLOCK_AURA.put(Blocks.RESPAWN_ANCHOR, FORCE);
		BLOCK_AURA.put(Blocks.END_PORTAL_FRAME, FORCE);

		// EARTH sources — stone, ore, earthen
		BLOCK_AURA.put(Blocks.DIAMOND_ORE, EARTH);
		BLOCK_AURA.put(Blocks.DEEPSLATE_DIAMOND_ORE, EARTH);
		BLOCK_AURA.put(Blocks.EMERALD_ORE, EARTH);
		BLOCK_AURA.put(Blocks.DEEPSLATE_EMERALD_ORE, EARTH);
		BLOCK_AURA.put(Blocks.ANCIENT_DEBRIS, EARTH);
		BLOCK_AURA.put(Blocks.AMETHYST_BLOCK, EARTH);
		BLOCK_AURA.put(Blocks.BUDDING_AMETHYST, EARTH);
		BLOCK_AURA.put(Blocks.AMETHYST_CLUSTER, EARTH);
	}

	/**
	 * Returns the aura type emitted by a specific block, or null if the block
	 * is not an aura source.
	 */
	public static VitalAura getAuraForBlock(BlockState state) {
		return BLOCK_AURA.get(state.getBlock());
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

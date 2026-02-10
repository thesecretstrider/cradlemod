package com.cradle.mod.worldgen;

import com.cradle.mod.CradleMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Registers worldgen features so that Vital Fruit Bushes and Spirit Fruit Bushes
 * spawn naturally in the overworld.
 *
 * The actual feature configuration is data-driven via JSON files in:
 *   data/cradlemod/worldgen/configured_feature/
 *   data/cradlemod/worldgen/placed_feature/
 *
 * This class uses Fabric's BiomeModifications API to inject those placed features
 * into overworld biomes.
 */
public final class CradleWorldGen {

    // ResourceKeys that match the JSON file names in data/cradlemod/worldgen/placed_feature/
    public static final ResourceKey<PlacedFeature> VITAL_FRUIT_BUSH_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE,
            Identifier.fromNamespaceAndPath(CradleMod.MOD_ID, "vital_fruit_bush")
    );

    public static final ResourceKey<PlacedFeature> SPIRIT_FRUIT_BUSH_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE,
            Identifier.fromNamespaceAndPath(CradleMod.MOD_ID, "spirit_fruit_bush")
    );

    /**
     * Register biome modifications to add our bushes to overworld biomes.
     * Call this from CradleMod.onInitialize().
     */
    public static void register() {
        // Vital Fruit Bush — common, spawns in most overworld biomes (like berry bushes)
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                VITAL_FRUIT_BUSH_PLACED
        );

        // Spirit Fruit Bush — rarer, also spawns in overworld biomes but much less frequently
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                SPIRIT_FRUIT_BUSH_PLACED
        );

        CradleMod.LOGGER.info("Registered Cradle worldgen features");
    }
}

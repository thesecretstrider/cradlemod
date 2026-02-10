package com.cradle.mod.worldgen;

import com.cradle.mod.CradleMod;
import com.cradle.mod.item.CradleItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * Injects Cradle mod items into vanilla loot tables.
 *
 * Spirit Stones are added to dungeon and temple chest loot tables
 * with a 5% chance per chest.
 */
public final class CradleLootTables {

    /**
     * Register loot table modifications.
     * Call this from CradleMod.onInitialize().
     */
    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            // Only modify vanilla/built-in loot tables, not data pack overrides
            if (!source.isBuiltin()) {
                return;
            }

            // Add Spirit Stone to dungeon chests, desert pyramids, jungle temples, and strongholds
            if (key == BuiltInLootTables.SIMPLE_DUNGEON
                    || key == BuiltInLootTables.DESERT_PYRAMID
                    || key == BuiltInLootTables.JUNGLE_TEMPLE
                    || key == BuiltInLootTables.STRONGHOLD_CORRIDOR
                    || key == BuiltInLootTables.STRONGHOLD_CROSSING
                    || key == BuiltInLootTables.ABANDONED_MINESHAFT) {

                // Add a new pool: rolls once, 5% chance to produce a Spirit Stone
                tableBuilder.pool(
                        LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1))
                                .add(LootItem.lootTableItem(CradleItems.SPIRIT_STONE))
                                .when(LootItemRandomChanceCondition.randomChance(0.05f))
                                .build()
                );
            }
        });

        CradleMod.LOGGER.info("Registered Cradle loot table modifications");
    }
}

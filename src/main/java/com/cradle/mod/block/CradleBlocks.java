package com.cradle.mod.block;

import com.cradle.mod.CradleMod;
import com.cradle.mod.item.CradleItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Supplier;

/**
 * Registers all custom blocks for the Cradle mod.
 *
 * MC 1.21.11 requires BlockBehaviour.Properties.setId() and
 * Item.Properties.setId() before constructing blocks and their items.
 */
public final class CradleBlocks {

	public static final Block VITAL_FRUIT_BUSH = registerFruitBush(
			"vital_fruit_bush", () -> CradleItems.VITAL_FRUIT
	);

	public static final Block SPIRIT_FRUIT_BUSH = registerFruitBush(
			"spirit_fruit_bush", () -> CradleItems.SPIRIT_FRUIT
	);

	/**
	 * Creates and registers a fruit bush block + its BlockItem.
	 */
	private static Block registerFruitBush(String name, Supplier<Item> fruitItem) {
		Identifier id = Identifier.fromNamespaceAndPath(CradleMod.MOD_ID, name);

		// Create block with ID set on properties
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		BlockBehaviour.Properties blockProps = BlockBehaviour.Properties.of()
				.setId(blockKey)
				.mapColor(MapColor.PLANT)
				.noCollision()
				.instabreak()
				.sound(SoundType.GRASS)
				.lightLevel(state -> 7)
				.pushReaction(PushReaction.DESTROY)
				.offsetType(BlockBehaviour.OffsetType.XZ);

		Block block = new FruitBushBlock(blockProps, fruitItem);
		Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

		// Create BlockItem with ID set on properties
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		Item.Properties itemProps = new Item.Properties()
				.setId(itemKey)
				.useBlockDescriptionPrefix();
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, itemProps));

		return block;
	}

	/**
	 * Call this in ModInitializer to force class loading and trigger registration.
	 */
	public static void register() {
		CradleMod.LOGGER.info("Registering Cradle blocks");
	}
}

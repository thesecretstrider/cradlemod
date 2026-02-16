package com.cradle.mod.item;

import com.cradle.mod.CradleMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * Registers all custom items for the Cradle mod.
 *
 * MC 1.21.11 requires Item.Properties.setId() to be called before
 * constructing the Item, otherwise effectiveDescriptionId() returns null.
 */
public final class CradleItems {

	public static final Item VITAL_FRUIT = registerItem("vital_fruit", new Item.Properties());
	public static final Item SPIRIT_FRUIT = registerItem("spirit_fruit", new Item.Properties());
	public static final Item SPIRIT_STONE = registerItem("spirit_stone", new Item.Properties());

	// Iron Body crystals — found in the world during special events
	public static final Item BLOODFORGED_CRYSTAL = registerItem("bloodforged_crystal", new Item.Properties());
	public static final Item STEELBORN_CRYSTAL = registerItem("steelborn_crystal", new Item.Properties());
	public static final Item RAINDROP_CRYSTAL = registerItem("raindrop_crystal", new Item.Properties());

	// Revelation items — crafted and consumed to start Lord-stage breakthrough trials
	public static final Item UNDERLORD_REVELATION = registerItem("underlord_revelation", new Item.Properties().stacksTo(1));
	public static final Item OVERLORD_REVELATION = registerItem("overlord_revelation", new Item.Properties().stacksTo(1));
	public static final Item ARCHLORD_REVELATION = registerItem("archlord_revelation", new Item.Properties().stacksTo(1));

	/**
	 * Helper: creates a ResourceKey, sets it on the properties, then registers.
	 */
	private static Item registerItem(String name, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(
				Registries.ITEM,
				Identifier.fromNamespaceAndPath(CradleMod.MOD_ID, name)
		);
		properties.setId(key).useItemDescriptionPrefix();
		Item item = new Item(properties);
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	/**
	 * Call this in ModInitializer to force class loading and trigger registration.
	 */
	public static void register() {
		CradleMod.LOGGER.info("Registering Cradle items");
	}
}

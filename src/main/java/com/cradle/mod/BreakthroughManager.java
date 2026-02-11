package com.cradle.mod;

import com.cradle.mod.item.CradleItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Handles advancement stage breakthroughs. When a player reaches the
 * required level AND has the required items, they break through to the next stage.
 *
 * Item requirements:
 *   Foundation -> Copper: 5 Vital Fruits
 *   Copper -> Iron:       5 Spirit Fruits
 *   Iron -> Jade:         1 Spirit Stone (from dungeon chests)
 *   Jade -> Gold:         Level only (placeholder for Remnant mob later)
 */
public final class BreakthroughManager {

	/**
	 * Returns the level required to reach the given stage.
	 * Returns Integer.MAX_VALUE for FOUNDATION (you start there)
	 * and for any unknown stage.
	 */
	public static int getLevelForStage(CradlePlayerData.AdvancementStage stage) {
		return switch (stage) {
			case COPPER -> 10;
			case IRON -> 25;
			case JADE -> 50;
			case GOLD -> 100;
			default -> Integer.MAX_VALUE;
		};
	}

	/**
	 * Returns the item required to advance TO the given stage, or null if none needed.
	 */
	public static Item getRequiredItem(CradlePlayerData.AdvancementStage stage) {
		return switch (stage) {
			case COPPER -> CradleItems.VITAL_FRUIT;
			case IRON -> CradleItems.SPIRIT_FRUIT;
			case JADE -> CradleItems.SPIRIT_STONE;
			case GOLD -> null; // Placeholder for Remnant mob drop later
			default -> null;
		};
	}

	/**
	 * Returns how many of the required item are needed to advance TO the given stage.
	 */
	public static int getRequiredItemCount(CradlePlayerData.AdvancementStage stage) {
		return switch (stage) {
			case COPPER -> 5;  // 5 Vital Fruits
			case IRON -> 5;    // 5 Spirit Fruits
			case JADE -> 1;    // 1 Spirit Stone
			case GOLD -> 0;    // No item (placeholder)
			default -> 0;
		};
	}

	/**
	 * Returns the display name for the required item (for chat messages).
	 */
	public static String getRequiredItemName(CradlePlayerData.AdvancementStage stage) {
		return switch (stage) {
			case COPPER -> "Vital Fruit";
			case IRON -> "Spirit Fruit";
			case JADE -> "Spirit Stone";
			default -> "";
		};
	}

	/**
	 * Returns the maxMadra boost granted when reaching the given stage.
	 */
	public static float getMaxMadraBoost(CradlePlayerData.AdvancementStage stage) {
		return switch (stage) {
			case COPPER -> 200f;
			case IRON -> 400f;
			case JADE -> 800f;
			case GOLD -> 1500f;
			default -> 0f;
		};
	}

	/**
	 * Counts how many of the given item the player has in their inventory.
	 */
	private static int countItemInInventory(ServerPlayer player, Item item) {
		int count = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/**
	 * Removes a specific count of the given item from the player's inventory.
	 * Assumes the player has enough (check with countItemInInventory first).
	 */
	private static void removeItemFromInventory(ServerPlayer player, Item item, int amount) {
		int remaining = amount;
		for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) {
				int toRemove = Math.min(remaining, stack.getCount());
				stack.shrink(toRemove);
				remaining -= toRemove;
			}
		}
	}

	/**
	 * Check if the player qualifies for a breakthrough after leveling up.
	 * Only NOTIFIES the player when they reach the required level — does NOT
	 * auto-advance. The player must use the "Advance" button (Phase 9D) to
	 * actually break through.
	 */
	public static void checkBreakthrough(ServerPlayer player, CradlePlayerData data) {
		CradlePlayerData.AdvancementStage currentStage = data.getAdvancementStage();

		if (!currentStage.hasNext()) {
			return; // Already at max stage (Gold)
		}

		CradlePlayerData.AdvancementStage nextStage = currentStage.next();
		int requiredLevel = getLevelForStage(nextStage);

		// Only notify once when they first reach the required level
		if (data.getPlayerLevel() == requiredLevel) {
			Item requiredItem = getRequiredItem(nextStage);
			int requiredCount = getRequiredItemCount(nextStage);

			if (requiredItem != null && requiredCount > 0) {
				String itemName = getRequiredItemName(nextStage);
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7fYou have reached the level for \u00A7e" +
								nextStage.displayName() + "\u00A7f! Collect \u00A7c" +
								requiredCount + "x " + itemName + "\u00A7f and press \u00A7eAdvance\u00A7f in your Sacred Artist Status (J) to break through!"
				));
			} else {
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7fYou have reached the level for \u00A7e" +
								nextStage.displayName() + "\u00A7f! Press \u00A7eAdvance\u00A7f in your Sacred Artist Status (J) to break through!"
				));
			}
		}
	}

	/**
	 * Returns true if the player meets ALL requirements to advance to the next stage:
	 * - Has the required level
	 * - Has the required items in inventory
	 */
	public static boolean canAdvance(ServerPlayer player, CradlePlayerData data) {
		CradlePlayerData.AdvancementStage currentStage = data.getAdvancementStage();
		if (!currentStage.hasNext()) {
			return false;
		}

		CradlePlayerData.AdvancementStage nextStage = currentStage.next();
		int requiredLevel = getLevelForStage(nextStage);

		if (data.getPlayerLevel() < requiredLevel) {
			return false;
		}

		Item requiredItem = getRequiredItem(nextStage);
		int requiredCount = getRequiredItemCount(nextStage);
		if (requiredItem != null && requiredCount > 0) {
			int playerHas = countItemInInventory(player, requiredItem);
			if (playerHas < requiredCount) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Actually perform the breakthrough: consume items, advance stage, boost stats.
	 * Called when the player presses the "Advance" button (Phase 9D).
	 * Returns true if the breakthrough was successful.
	 */
	public static boolean attemptBreakthrough(ServerPlayer player, CradlePlayerData data) {
		if (!canAdvance(player, data)) {
			return false;
		}

		CradlePlayerData.AdvancementStage nextStage = data.getAdvancementStage().next();

		// Consume required items
		Item requiredItem = getRequiredItem(nextStage);
		int requiredCount = getRequiredItemCount(nextStage);
		if (requiredItem != null && requiredCount > 0) {
			removeItemFromInventory(player, requiredItem, requiredCount);
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A77" + requiredCount + "x " +
							getRequiredItemName(nextStage) + " consumed."
			));
		}

		// Check for Iron Body crystal when advancing to Iron stage
		if (nextStage == CradlePlayerData.AdvancementStage.IRON) {
			CradlePlayerData.IronBody bodyType = detectIronBodyCrystal(player);
			data.setIronBody(bodyType);
			if (bodyType != CradlePlayerData.IronBody.NONE) {
				// Consume the crystal from whichever hand it's in
				consumeCrystalFromHands(player, bodyType);
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7d" + bodyType.displayName() +
								" Iron Body awakened!"
				));
			} else {
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A77You advance without an Iron Body."
				));
			}
		}

		// Advance stage
		data.setAdvancementStage(nextStage);

		// Boost maxMadra
		float boost = getMaxMadraBoost(nextStage);
		data.setMaxMadra(data.getMaxMadra() + boost);

		// Madra purification: reset to 25% of new max
		data.setCurrentMadra(data.getMaxMadra() * 0.25f);

		// Notify the player
		player.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A7d\u00A7lBREAKTHROUGH! \u00A7fYou have advanced to \u00A7e" +
						nextStage.displayName() + "\u00A7f!"
		));
		player.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A7fYour Madra has been purified. Your power grows denser..."
		));

		CradleMod.LOGGER.info("Player {} broke through to {} at level {}",
				player.getName().getString(), nextStage.name(), data.getPlayerLevel());

		return true;
	}

	/**
	 * Checks both hands for an Iron Body crystal item.
	 * Returns the matching IronBody type, or NONE if no crystal is held.
	 */
	private static CradlePlayerData.IronBody detectIronBodyCrystal(ServerPlayer player) {
		ItemStack mainHand = player.getMainHandItem();
		ItemStack offHand = player.getOffhandItem();

		if (mainHand.is(CradleItems.BLOODFORGED_CRYSTAL) || offHand.is(CradleItems.BLOODFORGED_CRYSTAL)) {
			return CradlePlayerData.IronBody.BLOODFORGED;
		}
		if (mainHand.is(CradleItems.STEELBORN_CRYSTAL) || offHand.is(CradleItems.STEELBORN_CRYSTAL)) {
			return CradlePlayerData.IronBody.STEELBORN;
		}
		if (mainHand.is(CradleItems.RAINDROP_CRYSTAL) || offHand.is(CradleItems.RAINDROP_CRYSTAL)) {
			return CradlePlayerData.IronBody.RAINDROP;
		}
		return CradlePlayerData.IronBody.NONE;
	}

	/**
	 * Consumes one crystal item from whichever hand holds it.
	 */
	private static void consumeCrystalFromHands(ServerPlayer player, CradlePlayerData.IronBody bodyType) {
		Item crystal = switch (bodyType) {
			case BLOODFORGED -> CradleItems.BLOODFORGED_CRYSTAL;
			case STEELBORN -> CradleItems.STEELBORN_CRYSTAL;
			case RAINDROP -> CradleItems.RAINDROP_CRYSTAL;
			default -> null;
		};
		if (crystal == null) return;

		if (player.getMainHandItem().is(crystal)) {
			player.getMainHandItem().shrink(1);
		} else if (player.getOffhandItem().is(crystal)) {
			player.getOffhandItem().shrink(1);
		}
	}

	/**
	 * Returns the level needed for the next breakthrough, or -1 if at max stage.
	 * Used by the client-side info screen.
	 */
	public static int getNextBreakthroughLevel(CradlePlayerData.AdvancementStage currentStage) {
		if (!currentStage.hasNext()) {
			return -1;
		}
		return getLevelForStage(currentStage.next());
	}
}

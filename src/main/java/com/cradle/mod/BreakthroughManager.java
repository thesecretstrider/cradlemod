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
 * Progression: Foundation → Copper → Iron → Jade → Low Gold → High Gold → Truegold
 *              → Underlord → Overlord → Archlord → (Sage or Herald) → (the other) → Monarch
 *
 * After Archlord, the player must choose Sage or Herald first.
 * After achieving one, they can advance to the other.
 * After achieving both, they advance to Monarch.
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
			case LOW_GOLD -> 100;
			case HIGH_GOLD -> 130;
			case TRUEGOLD -> 165;
			case UNDERLORD -> 200;
			case OVERLORD -> 250;
			case ARCHLORD -> 300;
			case SAGE, HERALD -> 350;
			case MONARCH -> 400;
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
			case UNDERLORD -> CradleItems.UNDERLORD_REVELATION;
			case OVERLORD -> CradleItems.OVERLORD_REVELATION;
			case ARCHLORD -> CradleItems.ARCHLORD_REVELATION;
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
			case UNDERLORD -> 1; // 1 Underlord Revelation
			case OVERLORD -> 1;  // 1 Overlord Revelation
			case ARCHLORD -> 1;  // 1 Archlord Revelation
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
			case UNDERLORD -> "Underlord Revelation";
			case OVERLORD -> "Overlord Revelation";
			case ARCHLORD -> "Archlord Revelation";
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
			case LOW_GOLD -> 1200f;
			case HIGH_GOLD -> 1500f;
			case TRUEGOLD -> 2000f;
			case UNDERLORD -> 3000f;
			case OVERLORD -> 4000f;
			case ARCHLORD -> 5500f;
			case SAGE, HERALD -> 4000f;
			case MONARCH -> 8000f;
			default -> 0f;
		};
	}

	// ── Sage/Herald branching logic ──────────────────────────────────

	/**
	 * Determines the next advancement stage for the given player data,
	 * accounting for the Sage/Herald branching after Archlord.
	 *
	 * After Archlord:
	 *   - If neither Sage nor Herald chosen: returns null (player must choose via UI)
	 *   - If has Sage but not Herald: next is HERALD
	 *   - If has Herald but not Sage: next is SAGE
	 *   - If has both: next is MONARCH
	 * At Sage: returns Herald (or Monarch if hasHerald)
	 * At Herald: returns Sage (or Monarch if hasSage)
	 * At Monarch: returns null (max stage)
	 * For all other stages: linear progression via next()
	 */
	public static CradlePlayerData.AdvancementStage getNextStage(CradlePlayerData data) {
		CradlePlayerData.AdvancementStage current = data.getAdvancementStage();

		if (current == CradlePlayerData.AdvancementStage.MONARCH) {
			return null; // Already at max
		}

		if (current == CradlePlayerData.AdvancementStage.ARCHLORD) {
			// Branching: player must choose Sage or Herald
			if (!data.hasSage() && !data.hasHerald()) {
				return null; // Needs to choose via UI — handled by ChooseSageHeraldPayload
			}
			if (data.hasSage() && !data.hasHerald()) {
				return CradlePlayerData.AdvancementStage.HERALD;
			}
			if (data.hasHerald() && !data.hasSage()) {
				return CradlePlayerData.AdvancementStage.SAGE;
			}
			// Has both — next is Monarch
			return CradlePlayerData.AdvancementStage.MONARCH;
		}

		if (current == CradlePlayerData.AdvancementStage.SAGE) {
			if (data.hasHerald()) {
				return CradlePlayerData.AdvancementStage.MONARCH;
			}
			return CradlePlayerData.AdvancementStage.HERALD;
		}

		if (current == CradlePlayerData.AdvancementStage.HERALD) {
			if (data.hasSage()) {
				return CradlePlayerData.AdvancementStage.MONARCH;
			}
			return CradlePlayerData.AdvancementStage.SAGE;
		}

		// Linear progression for all other stages
		return current.next();
	}

	// ── Inventory helpers ────────────────────────────────────────────

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

	// ── Breakthrough checks ──────────────────────────────────────────

	/**
	 * Check if the player qualifies for a breakthrough after leveling up.
	 * Only NOTIFIES the player when they reach the required level — does NOT
	 * auto-advance. The player must use the "Advance" button to actually break through.
	 */
	public static void checkBreakthrough(ServerPlayer player, CradlePlayerData data) {
		CradlePlayerData.AdvancementStage nextStage = getNextStage(data);
		if (nextStage == null) {
			return; // At max or needs Sage/Herald choice
		}

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
	 * - For Sage/Herald branching: a choice has been made (getNextStage returns non-null)
	 */
	public static boolean canAdvance(ServerPlayer player, CradlePlayerData data) {
		CradlePlayerData.AdvancementStage nextStage = getNextStage(data);
		if (nextStage == null) {
			return false;
		}

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
	 * Attempt a breakthrough. For most stages this is instant. For Lord stages
	 * (Underlord/Overlord/Archlord), it consumes the item and starts a revelation
	 * trial — the player must kill spirits to complete the breakthrough.
	 *
	 * Returns true if the breakthrough started (item consumed) or completed instantly.
	 */
	public static boolean attemptBreakthrough(ServerPlayer player, CradlePlayerData data) {
		if (!canAdvance(player, data)) {
			return false;
		}

		// Block if already in a trial
		if (RevelationTrialManager.isInTrial(player.getUUID())) {
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cYou are already undergoing a revelation trial!"
			));
			return false;
		}

		CradlePlayerData.AdvancementStage nextStage = getNextStage(data);
		if (nextStage == null) return false;

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

		// Lord stages require a revelation trial instead of instant advancement
		if (RevelationTrialManager.requiresTrial(nextStage)) {
			RevelationTrialManager.startTrial(player, nextStage);
			return true; // Item consumed, trial started
		}

		// All other stages: instant breakthrough
		performBreakthrough(player, data, nextStage);
		return true;
	}

	/**
	 * Performs the actual stage advancement: Iron Body check, set stage,
	 * boost Madra, notify player. Called directly for instant breakthroughs
	 * and by RevelationTrialManager when a trial is completed.
	 */
	public static void performBreakthrough(ServerPlayer player, CradlePlayerData data,
										   CradlePlayerData.AdvancementStage nextStage) {
		// Check for Iron Body crystal when advancing to Iron stage
		if (nextStage == CradlePlayerData.AdvancementStage.IRON) {
			CradlePlayerData.IronBody bodyType = detectIronBodyCrystal(player);
			data.setIronBody(bodyType);
			if (bodyType != CradlePlayerData.IronBody.NONE) {
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

		// Track Sage/Herald achievement
		if (nextStage == CradlePlayerData.AdvancementStage.SAGE) {
			data.setHasSage(true);
		} else if (nextStage == CradlePlayerData.AdvancementStage.HERALD) {
			data.setHasHerald(true);
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
	 * Returns the level needed for the next breakthrough, or -1 if at max stage,
	 * or -2 if the player needs to make a Sage/Herald choice first.
	 * Now takes CradlePlayerData instead of AdvancementStage because it needs
	 * the hasSage/hasHerald flags for branching.
	 */
	public static int getNextBreakthroughLevel(CradlePlayerData data) {
		CradlePlayerData.AdvancementStage current = data.getAdvancementStage();

		// At Archlord with no choice made = needs Sage/Herald selection
		if (current == CradlePlayerData.AdvancementStage.ARCHLORD
				&& !data.hasSage() && !data.hasHerald()) {
			return -2; // Signal: needs choice
		}

		CradlePlayerData.AdvancementStage nextStage = getNextStage(data);
		if (nextStage == null) {
			return -1; // Max stage
		}
		return getLevelForStage(nextStage);
	}
}

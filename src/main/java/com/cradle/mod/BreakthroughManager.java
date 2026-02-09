package com.cradle.mod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles advancement stage breakthroughs. When a player reaches the
 * required level, they automatically break through to the next stage.
 *
 * Called after every level-up to check if the player qualifies.
 *
 * TODO (Phase 11): Add item requirements for Iron->Jade and Jade->Gold
 * when custom items are built.
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
	 * Check if the player qualifies for a breakthrough after leveling up.
	 * If they do, advance their stage, boost maxMadra, and purify their Madra.
	 */
	public static void checkBreakthrough(ServerPlayer player, CradlePlayerData data) {
		CradlePlayerData.AdvancementStage currentStage = data.getAdvancementStage();

		if (!currentStage.hasNext()) {
			return; // Already at max stage (Gold)
		}

		CradlePlayerData.AdvancementStage nextStage = currentStage.next();
		int requiredLevel = getLevelForStage(nextStage);

		if (data.getPlayerLevel() < requiredLevel) {
			return; // Not high enough level yet
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

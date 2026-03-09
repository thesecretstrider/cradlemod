package com.cradle.mod;

import com.cradle.mod.entity.CradleEntities;
import com.cradle.mod.entity.RemnantEntity;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;

import java.util.UUID;

/**
 * Handles spawning Remnant entities when sacred artists and mobs die.
 *
 * Three sources of Remnants:
 * 1. Players (Copper+) — Path and power based on player data
 * 2. Sacred Beasts (future) — Path-specific, power based on beast tier
 * 3. Normal Minecraft Mobs — Low chance, weak Remnants with path based on biome aura
 */
public final class RemnantManager {

	// Chance for a normal (non-sacred-artist) mob to leave a Remnant
	private static final float MOB_REMNANT_CHANCE = 0.15f; // 15%

	private RemnantManager() {} // Utility class

	/**
	 * Called when any living entity dies. Determines if a Remnant should spawn.
	 */
	public static void trySpawnRemnant(ServerLevel level, LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			trySpawnPlayerRemnant(level, player);
		} else if (!(entity instanceof RemnantEntity)) {
			// Normal mobs have a chance to leave a weak Remnant
			trySpawnMobRemnant(level, entity);
		}
	}

	/**
	 * Spawn a Remnant when a sacred artist (player) dies.
	 * Only spawns if the player is at Copper or above.
	 */
	private static void trySpawnPlayerRemnant(ServerLevel level, ServerPlayer player) {
		CradlePlayerData data = CradlePlayerData.get(player.getUUID());
		if (data == null) {
			CradleMod.LOGGER.info("[Remnant] Player {} died but has no CradlePlayerData", player.getName().getString());
			return;
		}

		CradleMod.LOGGER.info("[Remnant] Player {} died — Stage: {}, Path: {}, InDuel: {}",
				player.getName().getString(), data.getAdvancementStage(), data.getChosenPath(),
				DuelManager.isInDuel(player.getUUID()));

		// Foundation players are too weak to leave a Remnant
		if (data.getAdvancementStage() == CradlePlayerData.AdvancementStage.FOUNDATION) {
			CradleMod.LOGGER.info("[Remnant] Skipped — player is Foundation stage");
			return;
		}

		// Must have chosen a path
		if (!data.hasChosenPath() || data.getChosenPath() == CradlePlayerData.Path.UNSET) {
			CradleMod.LOGGER.info("[Remnant] Skipped — player has no path chosen");
			return;
		}

		// Don't spawn Remnants during duels
		if (DuelManager.isInDuel(player.getUUID())) {
			CradleMod.LOGGER.info("[Remnant] Skipped — player is in a duel");
			return;
		}

		// Power level = stage ordinal (Copper=1, Iron=2, Jade=3, ...)
		int powerLevel = data.getAdvancementStage().ordinal(); // FOUNDATION=0, COPPER=1, etc.

		RemnantEntity remnant = new RemnantEntity(CradleEntities.REMNANT, level);
		remnant.setPos(player.getX(), player.getY(), player.getZ());
		remnant.initRemnant(data.getChosenPath(), powerLevel, player.getUUID(), "cradlemod:player");

		// Copy player's ability loadout to remnant for ability AI
		remnant.setStoredLoadout(data.getLoadout());
		remnant.setMaxMadraPool(data.getMaxMadra());
		remnant.setMadraPool(data.getMaxMadra());

		level.addFreshEntity(remnant);

		// Increment death counter (scales Herald fight difficulty later)
		data.incrementRemnantDeathCount();

		CradleMod.LOGGER.debug("Spawned {} Remnant (power {}) for player {}",
				data.getChosenPath().displayName(), powerLevel, player.getName().getString());
	}

	/**
	 * Normal Minecraft mobs have a small chance to leave a weak Remnant.
	 * The Remnant's path is based on the biome's vital aura type.
	 */
	private static void trySpawnMobRemnant(ServerLevel level, LivingEntity entity) {
		// Random chance check
		float roll = level.random.nextFloat();
		if (roll > MOB_REMNANT_CHANCE) return;

		// Don't spawn Remnants from tiny/ambient mobs (bats, fish, etc.)
		if (entity.getMaxHealth() < 8.0f) {
			CradleMod.LOGGER.debug("[Remnant] Mob {} passed 15% roll but too small (HP: {})",
					entity.getType().getDescription().getString(), entity.getMaxHealth());
			return;
		}

		CradleMod.LOGGER.info("[Remnant] Mob {} died and passed 15% roll — spawning Remnant",
				entity.getType().getDescription().getString());

		// Determine path from biome aura
		Holder<Biome> biome = level.getBiome(entity.blockPosition());
		VitalAura aura = VitalAura.getAuraForBiome(biome);
		CradlePlayerData.Path remnantPath = auraToPath(aura);

		// Mob Remnants are always weak (power 1-2)
		int powerLevel = level.random.nextInt(2) + 1; // 1 or 2

		// Get the mob's registry name (e.g. "minecraft:spider")
		String mobType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
				.getKey(entity.getType()).toString();

		RemnantEntity remnant = new RemnantEntity(CradleEntities.REMNANT, level);
		remnant.setPos(entity.getX(), entity.getY(), entity.getZ());
		remnant.initRemnant(remnantPath, powerLevel, null, mobType);
		level.addFreshEntity(remnant);
	}

	/**
	 * Maps a biome's vital aura type to a Path for mob Remnants.
	 */
	private static CradlePlayerData.Path auraToPath(VitalAura aura) {
		return switch (aura) {
			case FIRE -> CradlePlayerData.Path.BLACK_FLAME;
			case WATER -> CradlePlayerData.Path.ENDLESS_SWORD;
			case WIND -> CradlePlayerData.Path.CLOUD_HAMMER;
			case LIFE -> CradlePlayerData.Path.STELLAR_SPEAR;
			case FORCE -> CradlePlayerData.Path.CLOUD_HAMMER;
			case BLOOD -> CradlePlayerData.Path.BLACK_FLAME;
			case EARTH -> CradlePlayerData.Path.HOLLOW_KING;
		};
	}
}

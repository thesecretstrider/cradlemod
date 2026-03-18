package com.cradle.mod.worldgen.structure;

import com.cradle.mod.story.GameModeManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages structure placement in Sacred Valley.
 * Structures are generated on first world load (after chunks exist)
 * rather than during chunk generation, to avoid chunk-loading complexity.
 * A flag in world data tracks whether structures have been placed.
 */
public class StructureGenerator {
	private static final Logger LOGGER = LoggerFactory.getLogger("cradlemod");
	private static boolean structuresPlaced = false;

	/**
	 * Generate all Sacred Valley structures if not already done.
	 * Called from SERVER_STARTED hook.
	 */
	public static void generateIfNeeded(ServerLevel level) {
		if (!GameModeManager.isCradleMode() || structuresPlaced) return;

		LOGGER.info("Generating Sacred Valley structures...");

		WeiClanStructure.generate(level);
		HeavensGloryStructure.generate(level);
		MountSamaraStructure.generate(level);
		HolyWindStructure.generate(level);
		FallenLeafStructure.generate(level);
		GoldenSwordStructure.generate(level);
		KazanClanStructure.generate(level);
		LiClanStructure.generate(level);

		structuresPlaced = true;
		LOGGER.info("Sacred Valley structures placed.");
	}

	public static CompoundTag writeNbt() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("structuresPlaced", structuresPlaced);
		return tag;
	}

	public static void readNbt(CompoundTag tag) {
		structuresPlaced = tag.getBooleanOr("structuresPlaced", false);
	}

	public static void reset() {
		structuresPlaced = false;
	}
}

package com.cradle.mod.worldgen.structure;

import com.cradle.mod.worldgen.ValleyHeightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Holy Wind School — located to the north of Sacred Valley.
 * White/quartz aesthetic with open-air training platforms,
 * a meditation garden, and lantern lighting throughout.
 */
public class HolyWindStructure {
	private static final int BASE_X = 0;
	private static final int BASE_Z = -500;

	public static void generate(ServerLevel level) {
		int groundY = ValleyHeightmap.getHeight(BASE_X, BASE_Z);

		generateMainDojo(level, groundY);
		generateTrainingPlatforms(level, groundY);
		generateMeditationGarden(level, groundY);
		generatePaths(level, groundY);
	}

	/**
	 * Main dojo: 20x15 room with quartz walls/floor and white concrete roof.
	 */
	private static void generateMainDojo(ServerLevel level, int groundY) {
		BlockPos origin = new BlockPos(BASE_X - 10, groundY, BASE_Z - 7);

		// Clear area
		BuildingPlacer.fill(level, origin, 20, 15, 8, Blocks.AIR);

		// Place the main room
		BuildingPlacer.placeRoom(level, origin, 20, 15, 6,
				Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.WHITE_CONCRETE);

		// Doorway on south side
		BuildingPlacer.placeDoor(level, new BlockPos(BASE_X, groundY + 1, BASE_Z + 7));

		// Doorway on north side
		BuildingPlacer.placeDoor(level, new BlockPos(BASE_X, groundY + 1, BASE_Z - 7));

		// Lantern lighting along walls inside
		for (int x = BASE_X - 8; x <= BASE_X + 8; x += 4) {
			BuildingPlacer.placeBlock(level, new BlockPos(x, groundY + 4, BASE_Z - 6), Blocks.LANTERN);
			BuildingPlacer.placeBlock(level, new BlockPos(x, groundY + 4, BASE_Z + 6), Blocks.LANTERN);
		}
	}

	/**
	 * Three open-air training platforms at increasing heights east of the dojo.
	 * Each is 5x5 quartz slab, at +3, +6, +9 above ground.
	 */
	private static void generateTrainingPlatforms(ServerLevel level, int groundY) {
		int platX = BASE_X + 15;

		for (int i = 0; i < 3; i++) {
			int platY = groundY + 3 + (i * 3);
			int platZ = BASE_Z - 5 + (i * 7);
			BlockPos platOrigin = new BlockPos(platX, platY, platZ);

			// Platform surface
			BuildingPlacer.fill(level, platOrigin, 5, 5, 1, Blocks.QUARTZ_SLAB);

			// Clear air above platform
			BuildingPlacer.fill(level, platOrigin.above(), 5, 5, 3, Blocks.AIR);

			// Support pillar from ground to platform
			BuildingPlacer.fill(level, new BlockPos(platX + 2, groundY, platZ + 2),
					1, 1, platY - groundY, Blocks.QUARTZ_PILLAR);

			// Lantern on each platform
			BuildingPlacer.placeBlock(level, new BlockPos(platX + 2, platY + 1, platZ + 2), Blocks.LANTERN);
		}
	}

	/**
	 * Meditation garden: 10x10 area west of the dojo with flowers
	 * and a central water feature.
	 */
	private static void generateMeditationGarden(ServerLevel level, int groundY) {
		int gardenX = BASE_X - 20;
		int gardenZ = BASE_Z - 5;
		BlockPos gardenOrigin = new BlockPos(gardenX, groundY, gardenZ);

		// Grass floor
		BuildingPlacer.fill(level, gardenOrigin, 10, 10, 1, Blocks.GRASS_BLOCK);

		// Clear above
		BuildingPlacer.fill(level, gardenOrigin.above(), 10, 10, 4, Blocks.AIR);

		// Central water feature
		BuildingPlacer.placeBlock(level, new BlockPos(gardenX + 5, groundY + 1, gardenZ + 5), Blocks.WATER);

		// Surround water with quartz slab border
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) continue;
				BuildingPlacer.placeBlock(level,
						new BlockPos(gardenX + 5 + dx, groundY, gardenZ + 5 + dz), Blocks.QUARTZ_SLAB);
			}
		}

		// Scatter flowers
		BuildingPlacer.placeBlock(level, new BlockPos(gardenX + 2, groundY + 1, gardenZ + 2), Blocks.AZURE_BLUET);
		BuildingPlacer.placeBlock(level, new BlockPos(gardenX + 8, groundY + 1, gardenZ + 2), Blocks.WHITE_TULIP);
		BuildingPlacer.placeBlock(level, new BlockPos(gardenX + 2, groundY + 1, gardenZ + 8), Blocks.OXEYE_DAISY);
		BuildingPlacer.placeBlock(level, new BlockPos(gardenX + 8, groundY + 1, gardenZ + 8), Blocks.ALLIUM);
		BuildingPlacer.placeBlock(level, new BlockPos(gardenX + 5, groundY + 1, gardenZ + 1), Blocks.AZURE_BLUET);
		BuildingPlacer.placeBlock(level, new BlockPos(gardenX + 5, groundY + 1, gardenZ + 9), Blocks.WHITE_TULIP);
		BuildingPlacer.placeBlock(level, new BlockPos(gardenX + 1, groundY + 1, gardenZ + 5), Blocks.OXEYE_DAISY);
		BuildingPlacer.placeBlock(level, new BlockPos(gardenX + 9, groundY + 1, gardenZ + 5), Blocks.ALLIUM);

		// Lanterns around garden perimeter
		BuildingPlacer.placeBlock(level, new BlockPos(gardenX, groundY + 1, gardenZ), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(gardenX + 9, groundY + 1, gardenZ), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(gardenX, groundY + 1, gardenZ + 9), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(gardenX + 9, groundY + 1, gardenZ + 9), Blocks.LANTERN);
	}

	/**
	 * Gravel paths connecting the dojo, training platforms, and garden.
	 */
	private static void generatePaths(ServerLevel level, int groundY) {
		// Path from dojo south exit toward the valley
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X, groundY, BASE_Z + 8),
				new BlockPos(BASE_X, groundY, BASE_Z + 25),
				2, Blocks.GRAVEL);

		// Path from dojo east side to training platforms
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X + 10, groundY, BASE_Z),
				new BlockPos(BASE_X + 15, groundY, BASE_Z),
				2, Blocks.GRAVEL);

		// Path from dojo west side to meditation garden
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X - 10, groundY, BASE_Z),
				new BlockPos(BASE_X - 20, groundY, BASE_Z),
				2, Blocks.GRAVEL);
	}
}

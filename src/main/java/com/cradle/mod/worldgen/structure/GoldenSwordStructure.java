package com.cradle.mod.worldgen.structure;

import com.cradle.mod.worldgen.ValleyHeightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Golden Sword School — located to the south of Sacred Valley.
 * Gold/yellow terracotta aesthetic. Features a sunken arena with
 * tiered seating, a main hall with gold accents, and a forge room.
 */
public class GoldenSwordStructure {
	private static final int BASE_X = 0;
	private static final int BASE_Z = 400;

	public static void generate(ServerLevel level) {
		int groundY = ValleyHeightmap.getHeight(BASE_X, BASE_Z);

		generateArena(level, groundY);
		generateMainHall(level, groundY);
		generateForgeRoom(level, groundY);
		generatePaths(level, groundY);
	}

	/**
	 * Arena: 20x20 sunken area (-3 blocks deep) with yellow terracotta walls
	 * and smooth stone floor. Surrounded by 3 tiers of stair seating.
	 */
	private static void generateArena(ServerLevel level, int groundY) {
		int arenaX = BASE_X - 14;
		int arenaZ = BASE_Z - 10;

		// Clear the larger area for arena + seating (26x26 to fit 3 tiers on each side)
		BuildingPlacer.fill(level, new BlockPos(arenaX - 3, groundY - 3, arenaZ - 3),
				26, 26, 8, Blocks.AIR);

		// Sunken arena floor: 20x20, 3 blocks below ground
		int arenaFloorY = groundY - 3;
		BuildingPlacer.fill(level, new BlockPos(arenaX, arenaFloorY, arenaZ),
				20, 20, 1, Blocks.SMOOTH_STONE);

		// Arena walls (yellow terracotta lining the sunken area)
		// North wall
		BuildingPlacer.fill(level, new BlockPos(arenaX, arenaFloorY + 1, arenaZ),
				20, 1, 3, Blocks.YELLOW_TERRACOTTA);
		// South wall
		BuildingPlacer.fill(level, new BlockPos(arenaX, arenaFloorY + 1, arenaZ + 19),
				20, 1, 3, Blocks.YELLOW_TERRACOTTA);
		// West wall
		BuildingPlacer.fill(level, new BlockPos(arenaX, arenaFloorY + 1, arenaZ),
				1, 20, 3, Blocks.YELLOW_TERRACOTTA);
		// East wall
		BuildingPlacer.fill(level, new BlockPos(arenaX + 19, arenaFloorY + 1, arenaZ),
				1, 20, 3, Blocks.YELLOW_TERRACOTTA);

		// Tiered seating around the arena — 3 tiers of stairs rising outward
		for (int tier = 0; tier < 3; tier++) {
			int tierY = groundY - 2 + tier;
			int offset = tier + 1;

			// North seating
			BuildingPlacer.fill(level, new BlockPos(arenaX - offset, tierY, arenaZ - offset),
					20 + (offset * 2), 1, 1, Blocks.QUARTZ_STAIRS);
			// South seating
			BuildingPlacer.fill(level, new BlockPos(arenaX - offset, tierY, arenaZ + 19 + offset),
					20 + (offset * 2), 1, 1, Blocks.QUARTZ_STAIRS);
			// West seating
			BuildingPlacer.fill(level, new BlockPos(arenaX - offset, tierY, arenaZ - offset),
					1, 20 + (offset * 2), 1, Blocks.QUARTZ_STAIRS);
			// East seating
			BuildingPlacer.fill(level, new BlockPos(arenaX + 19 + offset, tierY, arenaZ - offset),
					1, 20 + (offset * 2), 1, Blocks.QUARTZ_STAIRS);
		}

		// Arena entrance gap on south side
		BuildingPlacer.placeDoor(level, new BlockPos(arenaX + 10, arenaFloorY + 1, arenaZ + 19));

		// Torches around arena perimeter
		for (int i = 0; i < 20; i += 5) {
			BuildingPlacer.placeBlock(level, new BlockPos(arenaX + i, groundY + 1, arenaZ - 3), Blocks.TORCH);
			BuildingPlacer.placeBlock(level, new BlockPos(arenaX + i, groundY + 1, arenaZ + 22), Blocks.TORCH);
		}
	}

	/**
	 * Main hall: 15x12, yellow terracotta walls with gold block accents at corners.
	 */
	private static void generateMainHall(ServerLevel level, int groundY) {
		int hallX = BASE_X - 7;
		int hallZ = BASE_Z + 15;
		BlockPos origin = new BlockPos(hallX, groundY, hallZ);

		// Clear area
		BuildingPlacer.fill(level, origin, 15, 12, 8, Blocks.AIR);

		// Place the main hall
		BuildingPlacer.placeRoom(level, origin, 15, 12, 6,
				Blocks.YELLOW_TERRACOTTA, Blocks.SMOOTH_STONE, Blocks.YELLOW_TERRACOTTA);

		// Doorway on north side (facing arena)
		BuildingPlacer.placeDoor(level, new BlockPos(hallX + 7, groundY + 1, hallZ));

		// Doorway on south side
		BuildingPlacer.placeDoor(level, new BlockPos(hallX + 7, groundY + 1, hallZ + 11));

		// Gold block accents at the 4 interior corners
		BuildingPlacer.placeBlock(level, new BlockPos(hallX + 1, groundY + 1, hallZ + 1), Blocks.GOLD_BLOCK);
		BuildingPlacer.placeBlock(level, new BlockPos(hallX + 13, groundY + 1, hallZ + 1), Blocks.GOLD_BLOCK);
		BuildingPlacer.placeBlock(level, new BlockPos(hallX + 1, groundY + 1, hallZ + 10), Blocks.GOLD_BLOCK);
		BuildingPlacer.placeBlock(level, new BlockPos(hallX + 13, groundY + 1, hallZ + 10), Blocks.GOLD_BLOCK);

		// Torches
		BuildingPlacer.placeBlock(level, new BlockPos(hallX + 4, groundY + 3, hallZ + 1), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(hallX + 10, groundY + 3, hallZ + 1), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(hallX + 4, groundY + 3, hallZ + 10), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(hallX + 10, groundY + 3, hallZ + 10), Blocks.TORCH);
	}

	/**
	 * Forge room: 8x8 with furnaces, anvils, and a single lava block.
	 * Located east of the main hall.
	 */
	private static void generateForgeRoom(ServerLevel level, int groundY) {
		int forgeX = BASE_X + 12;
		int forgeZ = BASE_Z + 17;
		BlockPos origin = new BlockPos(forgeX, groundY, forgeZ);

		// Clear area
		BuildingPlacer.fill(level, origin, 8, 8, 7, Blocks.AIR);

		// Place forge room
		BuildingPlacer.placeRoom(level, origin, 8, 8, 5,
				Blocks.YELLOW_TERRACOTTA, Blocks.SMOOTH_STONE, Blocks.YELLOW_TERRACOTTA);

		// Doorway on west side connecting to main hall
		BuildingPlacer.placeDoor(level, new BlockPos(forgeX, groundY + 1, forgeZ + 4));

		// Furnaces along north wall
		BuildingPlacer.placeBlock(level, new BlockPos(forgeX + 2, groundY + 1, forgeZ + 1), Blocks.FURNACE);
		BuildingPlacer.placeBlock(level, new BlockPos(forgeX + 4, groundY + 1, forgeZ + 1), Blocks.FURNACE);
		BuildingPlacer.placeBlock(level, new BlockPos(forgeX + 6, groundY + 1, forgeZ + 1), Blocks.BLAST_FURNACE);

		// Anvils
		BuildingPlacer.placeBlock(level, new BlockPos(forgeX + 3, groundY + 1, forgeZ + 4), Blocks.ANVIL);
		BuildingPlacer.placeBlock(level, new BlockPos(forgeX + 5, groundY + 1, forgeZ + 4), Blocks.ANVIL);

		// Lava source (contained in the corner)
		BuildingPlacer.placeBlock(level, new BlockPos(forgeX + 6, groundY + 1, forgeZ + 6), Blocks.LAVA);

		// Torch
		BuildingPlacer.placeBlock(level, new BlockPos(forgeX + 4, groundY + 3, forgeZ + 6), Blocks.TORCH);
	}

	/**
	 * Gravel paths connecting the structures.
	 */
	private static void generatePaths(ServerLevel level, int groundY) {
		// Path from arena south to main hall north
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X, groundY, BASE_Z + 10),
				new BlockPos(BASE_X, groundY, BASE_Z + 15),
				2, Blocks.GRAVEL);

		// Path from main hall south exit
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X, groundY, BASE_Z + 27),
				new BlockPos(BASE_X, groundY, BASE_Z + 40),
				2, Blocks.GRAVEL);

		// Path from main hall east to forge
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X + 8, groundY, BASE_Z + 21),
				new BlockPos(BASE_X + 12, groundY, BASE_Z + 21),
				2, Blocks.GRAVEL);

		// Path from arena north toward valley center
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X, groundY, BASE_Z - 13),
				new BlockPos(BASE_X, groundY, BASE_Z - 30),
				2, Blocks.GRAVEL);
	}
}

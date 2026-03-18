package com.cradle.mod.worldgen.structure;

import com.cradle.mod.worldgen.ValleyHeightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Li Clan — located to the southeast of Sacred Valley.
 * Elegant birch/sandstone aesthetic. Features a two-floor main library,
 * a courtyard with water fountain, scholar quarters, and an observatory tower.
 */
public class LiClanStructure {
	private static final int BASE_X = 350;
	private static final int BASE_Z = 350;

	public static void generate(ServerLevel level) {
		int groundY = ValleyHeightmap.getHeight(BASE_X, BASE_Z);

		generateMainLibrary(level, groundY);
		generateCourtyard(level, groundY);
		generateScholarQuarters(level, groundY);
		generateObservatoryTower(level, groundY);
		generatePaths(level, groundY);
	}

	/**
	 * Main library: 15x12, two floors (height 8), sandstone walls, birch floor.
	 * Bookshelves lining walls, enchanting tables, and lecterns.
	 */
	private static void generateMainLibrary(ServerLevel level, int groundY) {
		BlockPos origin = new BlockPos(BASE_X - 7, groundY, BASE_Z - 6);

		// Clear area
		BuildingPlacer.fill(level, origin, 15, 12, 10, Blocks.AIR);

		// Place the library room — two floors tall (height 8)
		BuildingPlacer.placeRoom(level, origin, 15, 12, 8,
				Blocks.SANDSTONE, Blocks.BIRCH_PLANKS, Blocks.SANDSTONE);

		// Second floor platform at Y+4 (halfway up), leaving stairwell gap
		BuildingPlacer.fill(level, new BlockPos(BASE_X - 6, groundY + 4, BASE_Z - 5),
				13, 10, 1, Blocks.BIRCH_PLANKS);

		// Stairwell gap on east side of second floor
		BuildingPlacer.fill(level, new BlockPos(BASE_X + 5, groundY + 4, BASE_Z - 3),
				2, 3, 1, Blocks.AIR);

		// Ladder to second floor
		for (int y = groundY + 1; y < groundY + 4; y++) {
			BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + 6, y, BASE_Z - 2), Blocks.LADDER);
		}

		// Doorway on south side
		BuildingPlacer.placeDoor(level, new BlockPos(BASE_X, groundY + 1, BASE_Z + 5));

		// Doorway on west side
		BuildingPlacer.placeDoor(level, new BlockPos(BASE_X - 7, groundY + 1, BASE_Z));

		// First floor — bookshelves lining walls
		for (int x = BASE_X - 5; x <= BASE_X + 5; x += 2) {
			BuildingPlacer.placeBlock(level, new BlockPos(x, groundY + 1, BASE_Z - 5), Blocks.BOOKSHELF);
			BuildingPlacer.placeBlock(level, new BlockPos(x, groundY + 2, BASE_Z - 5), Blocks.BOOKSHELF);
			BuildingPlacer.placeBlock(level, new BlockPos(x, groundY + 3, BASE_Z - 5), Blocks.BOOKSHELF);
		}
		for (int z = BASE_Z - 4; z <= BASE_Z + 3; z += 2) {
			BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 6, groundY + 1, z), Blocks.BOOKSHELF);
			BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 6, groundY + 2, z), Blocks.BOOKSHELF);
		}

		// Second floor — more bookshelves
		for (int x = BASE_X - 5; x <= BASE_X + 3; x += 2) {
			BuildingPlacer.placeBlock(level, new BlockPos(x, groundY + 5, BASE_Z - 5), Blocks.BOOKSHELF);
			BuildingPlacer.placeBlock(level, new BlockPos(x, groundY + 6, BASE_Z - 5), Blocks.BOOKSHELF);
		}

		// Enchanting tables on first floor
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 2, groundY + 1, BASE_Z - 1), Blocks.ENCHANTING_TABLE);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + 2, groundY + 1, BASE_Z - 1), Blocks.ENCHANTING_TABLE);

		// Lecterns
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X, groundY + 1, BASE_Z + 2), Blocks.LECTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 3, groundY + 5, BASE_Z), Blocks.LECTERN);

		// Lantern lighting
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X, groundY + 3, BASE_Z), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 4, groundY + 3, BASE_Z - 3), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + 4, groundY + 3, BASE_Z + 3), Blocks.LANTERN);
		// Second floor lanterns
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X, groundY + 7, BASE_Z), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 4, groundY + 7, BASE_Z - 3), Blocks.LANTERN);
	}

	/**
	 * Courtyard: 12x12 open area south of the library with a water fountain in the center.
	 */
	private static void generateCourtyard(ServerLevel level, int groundY) {
		int courtX = BASE_X - 6;
		int courtZ = BASE_Z + 8;
		BlockPos origin = new BlockPos(courtX, groundY, courtZ);

		// Sandstone floor
		BuildingPlacer.fill(level, origin, 12, 12, 1, Blocks.SMOOTH_SANDSTONE);

		// Clear above
		BuildingPlacer.fill(level, origin.above(), 12, 12, 5, Blocks.AIR);

		// Low sandstone wall border (1 block high)
		BuildingPlacer.placeFence(level, origin.above(), 12, 12, Blocks.SANDSTONE_WALL);

		// Openings on north side (toward library) and south side
		BuildingPlacer.placeBlock(level, new BlockPos(courtX + 6, groundY + 1, courtZ), Blocks.AIR);
		BuildingPlacer.placeBlock(level, new BlockPos(courtX + 6, groundY + 1, courtZ + 11), Blocks.AIR);

		// Central water fountain
		int fountainX = courtX + 6;
		int fountainZ = courtZ + 6;

		// Fountain basin — quartz ring around water
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) {
					// Water in center
					BuildingPlacer.placeBlock(level,
							new BlockPos(fountainX, groundY + 1, fountainZ), Blocks.WATER);
				} else {
					// Quartz rim
					BuildingPlacer.placeBlock(level,
							new BlockPos(fountainX + dx, groundY + 1, fountainZ + dz), Blocks.QUARTZ_BLOCK);
				}
			}
		}
		// Fountain pillar rising from center
		BuildingPlacer.placeBlock(level, new BlockPos(fountainX, groundY + 2, fountainZ), Blocks.QUARTZ_PILLAR);
		BuildingPlacer.placeBlock(level, new BlockPos(fountainX, groundY + 3, fountainZ), Blocks.WATER);

		// Lanterns at courtyard corners
		BuildingPlacer.placeBlock(level, new BlockPos(courtX + 1, groundY + 2, courtZ + 1), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(courtX + 10, groundY + 2, courtZ + 1), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(courtX + 1, groundY + 2, courtZ + 10), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(courtX + 10, groundY + 2, courtZ + 10), Blocks.LANTERN);
	}

	/**
	 * Three scholar quarters (6x6 each) east of the library, each with bed and bookshelves.
	 */
	private static void generateScholarQuarters(ServerLevel level, int groundY) {
		int quarterBaseX = BASE_X + 12;
		int quarterBaseZ = BASE_Z - 4;

		for (int i = 0; i < 3; i++) {
			int qZ = quarterBaseZ + (i * 7);
			BlockPos origin = new BlockPos(quarterBaseX, groundY, qZ);

			// Clear area
			BuildingPlacer.fill(level, origin, 6, 6, 6, Blocks.AIR);

			// Place the room
			BuildingPlacer.placeRoom(level, origin, 6, 6, 4,
					Blocks.SANDSTONE, Blocks.BIRCH_PLANKS, Blocks.SANDSTONE);

			// Doorway on west side
			BuildingPlacer.placeDoor(level, new BlockPos(quarterBaseX, groundY + 1, qZ + 3));

			// Bed
			BuildingPlacer.placeBlock(level, new BlockPos(quarterBaseX + 4, groundY + 1, qZ + 4), Blocks.WHITE_BED);

			// Bookshelves
			BuildingPlacer.placeBlock(level, new BlockPos(quarterBaseX + 1, groundY + 1, qZ + 1), Blocks.BOOKSHELF);
			BuildingPlacer.placeBlock(level, new BlockPos(quarterBaseX + 2, groundY + 1, qZ + 1), Blocks.BOOKSHELF);
			BuildingPlacer.placeBlock(level, new BlockPos(quarterBaseX + 1, groundY + 2, qZ + 1), Blocks.BOOKSHELF);
			BuildingPlacer.placeBlock(level, new BlockPos(quarterBaseX + 2, groundY + 2, qZ + 1), Blocks.BOOKSHELF);

			// Lantern
			BuildingPlacer.placeBlock(level, new BlockPos(quarterBaseX + 3, groundY + 3, qZ + 3), Blocks.LANTERN);
		}
	}

	/**
	 * Observatory tower: sandstone, 3 radius, 12 blocks tall.
	 * Located west of the library.
	 */
	private static void generateObservatoryTower(ServerLevel level, int groundY) {
		BlockPos towerBase = new BlockPos(BASE_X - 15, groundY, BASE_Z);

		// Clear area above for the tower
		BuildingPlacer.fill(level, new BlockPos(BASE_X - 18, groundY, BASE_Z - 3),
				7, 7, 15, Blocks.AIR);

		// Place the tower — solid cylinder
		BuildingPlacer.placeTower(level, towerBase, 3, 12, Blocks.SANDSTONE);

		// Hollow out the interior (radius 2)
		for (int y = 1; y < 11; y++) {
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					BuildingPlacer.placeBlock(level,
							new BlockPos(BASE_X - 15 + x, groundY + y, BASE_Z + z), Blocks.AIR);
				}
			}
		}

		// Floor inside
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 15, groundY, BASE_Z), Blocks.BIRCH_PLANKS);
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				BuildingPlacer.placeBlock(level,
						new BlockPos(BASE_X - 15 + x, groundY, BASE_Z + z), Blocks.BIRCH_PLANKS);
			}
		}

		// Ladder inside the tower
		for (int y = groundY + 1; y < groundY + 11; y++) {
			BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 14, y, BASE_Z), Blocks.LADDER);
		}

		// Observation platform at top (slightly wider)
		BuildingPlacer.fill(level, new BlockPos(BASE_X - 18, groundY + 12, BASE_Z - 3),
				7, 7, 1, Blocks.SMOOTH_SANDSTONE);

		// Sandstone wall railing at top
		BuildingPlacer.placeFence(level, new BlockPos(BASE_X - 18, groundY + 13, BASE_Z - 3),
				7, 7, Blocks.SANDSTONE_WALL);

		// Doorway at base (east side, toward library)
		BuildingPlacer.placeDoor(level, new BlockPos(BASE_X - 13, groundY + 1, BASE_Z));

		// Lantern at the top
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 15, groundY + 13, BASE_Z), Blocks.LANTERN);
	}

	/**
	 * Paths connecting library, courtyard, scholar quarters, and observatory.
	 */
	private static void generatePaths(ServerLevel level, int groundY) {
		// Path from library south exit to courtyard
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X, groundY, BASE_Z + 6),
				new BlockPos(BASE_X, groundY, BASE_Z + 8),
				2, Blocks.GRAVEL);

		// Path from courtyard south exit outward
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X, groundY, BASE_Z + 20),
				new BlockPos(BASE_X, groundY, BASE_Z + 35),
				2, Blocks.GRAVEL);

		// Path from library east side to scholar quarters
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X + 8, groundY, BASE_Z),
				new BlockPos(BASE_X + 12, groundY, BASE_Z),
				2, Blocks.GRAVEL);

		// Path from library west side to observatory tower
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X - 7, groundY, BASE_Z),
				new BlockPos(BASE_X - 13, groundY, BASE_Z),
				2, Blocks.GRAVEL);
	}
}

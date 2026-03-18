package com.cradle.mod.worldgen.structure;

import com.cradle.mod.worldgen.ValleyHeightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Fallen Leaf School — located to the east of Sacred Valley.
 * Dark wood/stealth aesthetic. Features a main hall, underground
 * training hall accessed by ladder, library, and dormitories.
 */
public class FallenLeafStructure {
	private static final int BASE_X = 400;
	private static final int BASE_Z = 0;

	public static void generate(ServerLevel level) {
		int groundY = ValleyHeightmap.getHeight(BASE_X, BASE_Z);

		generateMainHall(level, groundY);
		generateUndergroundTrainingHall(level, groundY);
		generateLibrary(level, groundY);
		generateDormitories(level, groundY);
		generatePaths(level, groundY);
	}

	/**
	 * Main hall: 15x12 room with dark oak walls and spruce floor.
	 */
	private static void generateMainHall(ServerLevel level, int groundY) {
		BlockPos origin = new BlockPos(BASE_X - 7, groundY, BASE_Z - 6);

		// Clear area
		BuildingPlacer.fill(level, origin, 15, 12, 8, Blocks.AIR);

		// Place the main hall
		BuildingPlacer.placeRoom(level, origin, 15, 12, 6,
				Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS);

		// Doorway on south side
		BuildingPlacer.placeDoor(level, new BlockPos(BASE_X, groundY + 1, BASE_Z + 5));

		// Doorway on north side
		BuildingPlacer.placeDoor(level, new BlockPos(BASE_X, groundY + 1, BASE_Z - 6));

		// Torches for moody lighting
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 5, groundY + 3, BASE_Z - 4), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + 5, groundY + 3, BASE_Z - 4), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 5, groundY + 3, BASE_Z + 3), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + 5, groundY + 3, BASE_Z + 3), Blocks.TORCH);

		// Trapdoor entrance to underground training hall in the floor
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X, groundY, BASE_Z), Blocks.OAK_TRAPDOOR);
	}

	/**
	 * Underground training hall: 12x12 room below the main hall,
	 * accessed by a ladder from the trapdoor above.
	 */
	private static void generateUndergroundTrainingHall(ServerLevel level, int groundY) {
		int undergroundY = groundY - 7;
		BlockPos origin = new BlockPos(BASE_X - 6, undergroundY, BASE_Z - 6);

		// Carve out underground room
		BuildingPlacer.placeRoom(level, origin, 12, 12, 6,
				Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.DARK_OAK_PLANKS);

		// Ladder shaft from trapdoor down to underground room
		for (int y = undergroundY + 1; y < groundY; y++) {
			BuildingPlacer.placeBlock(level, new BlockPos(BASE_X, y, BASE_Z), Blocks.LADDER);
		}

		// Torches in the underground hall
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 4, undergroundY + 3, BASE_Z - 4), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + 4, undergroundY + 3, BASE_Z - 4), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 4, undergroundY + 3, BASE_Z + 4), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + 4, undergroundY + 3, BASE_Z + 4), Blocks.TORCH);

		// Armor stands (represented as dark oak fences for training dummies)
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 3, undergroundY + 1, BASE_Z), Blocks.DARK_OAK_FENCE);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + 3, undergroundY + 1, BASE_Z), Blocks.DARK_OAK_FENCE);
	}

	/**
	 * Library room: 10x8, filled with bookshelves on walls.
	 * Located to the north of the main hall.
	 */
	private static void generateLibrary(ServerLevel level, int groundY) {
		int libX = BASE_X - 5;
		int libZ = BASE_Z - 14;
		BlockPos origin = new BlockPos(libX, groundY, libZ);

		// Clear area
		BuildingPlacer.fill(level, origin, 10, 8, 7, Blocks.AIR);

		// Place library room
		BuildingPlacer.placeRoom(level, origin, 10, 8, 5,
				Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS);

		// Doorway on south side connecting to main hall area
		BuildingPlacer.placeDoor(level, new BlockPos(libX + 5, groundY + 1, libZ + 7));

		// Line walls with bookshelves (one block in from walls, at floor+1 and floor+2)
		for (int x = 1; x < 9; x++) {
			// North wall bookshelves
			BuildingPlacer.placeBlock(level, new BlockPos(libX + x, groundY + 1, libZ + 1), Blocks.BOOKSHELF);
			BuildingPlacer.placeBlock(level, new BlockPos(libX + x, groundY + 2, libZ + 1), Blocks.BOOKSHELF);
			// South wall bookshelves (leaving gap for door)
			if (x != 5) {
				BuildingPlacer.placeBlock(level, new BlockPos(libX + x, groundY + 1, libZ + 6), Blocks.BOOKSHELF);
				BuildingPlacer.placeBlock(level, new BlockPos(libX + x, groundY + 2, libZ + 6), Blocks.BOOKSHELF);
			}
		}
		// East and west wall bookshelves
		for (int z = 2; z < 6; z++) {
			BuildingPlacer.placeBlock(level, new BlockPos(libX + 1, groundY + 1, libZ + z), Blocks.BOOKSHELF);
			BuildingPlacer.placeBlock(level, new BlockPos(libX + 1, groundY + 2, libZ + z), Blocks.BOOKSHELF);
			BuildingPlacer.placeBlock(level, new BlockPos(libX + 8, groundY + 1, libZ + z), Blocks.BOOKSHELF);
			BuildingPlacer.placeBlock(level, new BlockPos(libX + 8, groundY + 2, libZ + z), Blocks.BOOKSHELF);
		}

		// Lanterns for reading light
		BuildingPlacer.placeBlock(level, new BlockPos(libX + 5, groundY + 3, libZ + 4), Blocks.LANTERN);
	}

	/**
	 * Four small dormitories (5x5 each) east of the main hall, each with a bed.
	 */
	private static void generateDormitories(ServerLevel level, int groundY) {
		int dormBaseX = BASE_X + 12;
		int dormBaseZ = BASE_Z - 6;

		for (int i = 0; i < 4; i++) {
			int dormZ = dormBaseZ + (i * 6);
			BlockPos origin = new BlockPos(dormBaseX, groundY, dormZ);

			// Clear area
			BuildingPlacer.fill(level, origin, 5, 5, 6, Blocks.AIR);

			// Place dormitory room
			BuildingPlacer.placeRoom(level, origin, 5, 5, 4,
					Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS);

			// Doorway on west side
			BuildingPlacer.placeDoor(level, new BlockPos(dormBaseX, groundY + 1, dormZ + 2));

			// Bed
			BuildingPlacer.placeBlock(level, new BlockPos(dormBaseX + 3, groundY + 1, dormZ + 3), Blocks.RED_BED);

			// Torch
			BuildingPlacer.placeBlock(level, new BlockPos(dormBaseX + 2, groundY + 2, dormZ + 1), Blocks.TORCH);
		}
	}

	/**
	 * Gravel paths connecting the structures.
	 */
	private static void generatePaths(ServerLevel level, int groundY) {
		// Path from main hall south exit
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X, groundY, BASE_Z + 6),
				new BlockPos(BASE_X, groundY, BASE_Z + 20),
				2, Blocks.GRAVEL);

		// Path from main hall to dormitories
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X + 8, groundY, BASE_Z),
				new BlockPos(BASE_X + 12, groundY, BASE_Z),
				2, Blocks.GRAVEL);

		// Path from main hall to library
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X, groundY, BASE_Z - 6),
				new BlockPos(BASE_X, groundY, BASE_Z - 14),
				2, Blocks.GRAVEL);
	}
}

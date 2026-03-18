package com.cradle.mod.worldgen.structure;

import com.cradle.mod.worldgen.ValleyHeightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Kazan Clan — located to the northeast of Sacred Valley.
 * Dark fortress aesthetic with deepslate and blackstone.
 * Features a fortress compound with thick double walls,
 * a sunken training pit, a war room, and lava accents.
 */
public class KazanClanStructure {
	private static final int BASE_X = 350;
	private static final int BASE_Z = -350;

	public static void generate(ServerLevel level) {
		int groundY = ValleyHeightmap.getHeight(BASE_X, BASE_Z);

		generateFortressCompound(level, groundY);
		generateTrainingPit(level, groundY);
		generateWarRoom(level, groundY);
		generatePaths(level, groundY);
	}

	/**
	 * Fortress compound: 20x20 room with deepslate brick walls, blackstone floor,
	 * and thick outer walls (double-width).
	 */
	private static void generateFortressCompound(ServerLevel level, int groundY) {
		BlockPos origin = new BlockPos(BASE_X - 12, groundY, BASE_Z - 12);

		// Clear area (extra size for double walls)
		BuildingPlacer.fill(level, origin, 24, 24, 10, Blocks.AIR);

		// Outer walls — first layer (24x24)
		BuildingPlacer.placeRoom(level, origin, 24, 24, 7,
				Blocks.DEEPSLATE_BRICKS, Blocks.BLACKSTONE, Blocks.DEEPSLATE_BRICKS);

		// Inner room — the actual compound interior (20x20, offset by 2 for double walls)
		BlockPos innerOrigin = new BlockPos(BASE_X - 10, groundY, BASE_Z - 10);
		BuildingPlacer.placeRoom(level, innerOrigin, 20, 20, 7,
				Blocks.DEEPSLATE_BRICKS, Blocks.BLACKSTONE, Blocks.DEEPSLATE_BRICKS);

		// Main entrance doorway on south side (through both wall layers)
		BuildingPlacer.placeDoor(level, new BlockPos(BASE_X, groundY + 1, BASE_Z + 11));
		BuildingPlacer.placeDoor(level, new BlockPos(BASE_X, groundY + 1, BASE_Z + 10));
		BuildingPlacer.placeDoor(level, new BlockPos(BASE_X, groundY + 1, BASE_Z + 9));

		// Lava accents — 2 lava blocks behind glass panes for dramatic lighting
		// West side lava accent
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 9, groundY + 2, BASE_Z - 9), Blocks.LAVA);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 9, groundY + 3, BASE_Z - 9), Blocks.GLASS_PANE);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 8, groundY + 2, BASE_Z - 9), Blocks.GLASS_PANE);

		// East side lava accent
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + 9, groundY + 2, BASE_Z - 9), Blocks.LAVA);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + 9, groundY + 3, BASE_Z - 9), Blocks.GLASS_PANE);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + 8, groundY + 2, BASE_Z - 9), Blocks.GLASS_PANE);

		// Torches along interior walls
		for (int i = -6; i <= 6; i += 4) {
			BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + i, groundY + 4, BASE_Z - 9), Blocks.TORCH);
			BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + i, groundY + 4, BASE_Z + 9), Blocks.TORCH);
			BuildingPlacer.placeBlock(level, new BlockPos(BASE_X - 9, groundY + 4, BASE_Z + i), Blocks.TORCH);
			BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + 9, groundY + 4, BASE_Z + i), Blocks.TORCH);
		}
	}

	/**
	 * Training pit: 12x12 sunken arena (-2 blocks) in the center of the compound.
	 */
	private static void generateTrainingPit(ServerLevel level, int groundY) {
		int pitX = BASE_X - 6;
		int pitZ = BASE_Z - 6;
		int pitFloorY = groundY - 2;

		// Dig out the pit
		BuildingPlacer.fill(level, new BlockPos(pitX, pitFloorY, pitZ), 12, 12, 3, Blocks.AIR);

		// Pit floor
		BuildingPlacer.fill(level, new BlockPos(pitX, pitFloorY, pitZ), 12, 12, 1, Blocks.BLACKSTONE);

		// Pit walls (2 blocks high around the edge)
		// North wall
		BuildingPlacer.fill(level, new BlockPos(pitX, pitFloorY + 1, pitZ), 12, 1, 2, Blocks.DEEPSLATE_BRICKS);
		// South wall
		BuildingPlacer.fill(level, new BlockPos(pitX, pitFloorY + 1, pitZ + 11), 12, 1, 2, Blocks.DEEPSLATE_BRICKS);
		// West wall
		BuildingPlacer.fill(level, new BlockPos(pitX, pitFloorY + 1, pitZ), 1, 12, 2, Blocks.DEEPSLATE_BRICKS);
		// East wall
		BuildingPlacer.fill(level, new BlockPos(pitX + 11, pitFloorY + 1, pitZ), 1, 12, 2, Blocks.DEEPSLATE_BRICKS);

		// Steps down on the south side
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X, pitFloorY + 1, pitZ + 11), Blocks.BLACKSTONE_STAIRS);
		BuildingPlacer.placeBlock(level, new BlockPos(BASE_X + 1, pitFloorY + 1, pitZ + 11), Blocks.BLACKSTONE_STAIRS);
	}

	/**
	 * War room: 10x10, dark oak table (platform), torches.
	 * Located north of the compound, attached to the fortress.
	 */
	private static void generateWarRoom(ServerLevel level, int groundY) {
		int warX = BASE_X - 5;
		int warZ = BASE_Z - 22;
		BlockPos origin = new BlockPos(warX, groundY, warZ);

		// Clear area
		BuildingPlacer.fill(level, origin, 10, 10, 7, Blocks.AIR);

		// Place war room
		BuildingPlacer.placeRoom(level, origin, 10, 10, 6,
				Blocks.DEEPSLATE_BRICKS, Blocks.BLACKSTONE, Blocks.DEEPSLATE_BRICKS);

		// Doorway on south side connecting to compound
		BuildingPlacer.placeDoor(level, new BlockPos(warX + 5, groundY + 1, warZ + 9));

		// Dark oak table (platform) in center — 4x2 surface
		BuildingPlacer.fill(level, new BlockPos(warX + 3, groundY + 1, warZ + 4),
				4, 2, 1, Blocks.DARK_OAK_PLANKS);

		// Table legs (fence posts)
		BuildingPlacer.placeBlock(level, new BlockPos(warX + 3, groundY + 1, warZ + 4), Blocks.DARK_OAK_FENCE);
		BuildingPlacer.placeBlock(level, new BlockPos(warX + 6, groundY + 1, warZ + 4), Blocks.DARK_OAK_FENCE);
		BuildingPlacer.placeBlock(level, new BlockPos(warX + 3, groundY + 1, warZ + 5), Blocks.DARK_OAK_FENCE);
		BuildingPlacer.placeBlock(level, new BlockPos(warX + 6, groundY + 1, warZ + 5), Blocks.DARK_OAK_FENCE);
		// Table top above legs
		BuildingPlacer.fill(level, new BlockPos(warX + 3, groundY + 2, warZ + 4),
				4, 2, 1, Blocks.DARK_OAK_PLANKS);

		// Torches on walls
		BuildingPlacer.placeBlock(level, new BlockPos(warX + 1, groundY + 3, warZ + 1), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(warX + 8, groundY + 3, warZ + 1), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(warX + 1, groundY + 3, warZ + 8), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(warX + 8, groundY + 3, warZ + 8), Blocks.TORCH);
	}

	/**
	 * Gravel paths connecting the fortress to the valley.
	 */
	private static void generatePaths(ServerLevel level, int groundY) {
		// Path from fortress south entrance toward the valley
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X, groundY, BASE_Z + 12),
				new BlockPos(BASE_X, groundY, BASE_Z + 30),
				2, Blocks.GRAVEL);

		// Path from compound north side to war room
		BuildingPlacer.placePath(level,
				new BlockPos(BASE_X, groundY, BASE_Z - 12),
				new BlockPos(BASE_X, groundY, BASE_Z - 22),
				2, Blocks.GRAVEL);
	}
}

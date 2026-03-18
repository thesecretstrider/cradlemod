package com.cradle.mod.worldgen.structure;

import com.cradle.mod.worldgen.ValleyHeightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Generates the Wei clan village in Sacred Valley.
 * The Wei are a small clan in a quiet corner of the valley — their compound
 * consists of a main hall, training grounds, transcription hall, residential
 * houses, and the Wei family home, all connected by gravel paths.
 *
 * Approximate center: (-300, surface, -300)
 */
public class WeiClanStructure {

	// Village center coordinates
	private static final int CENTER_X = -300;
	private static final int CENTER_Z = -300;

	public static void generate(ServerLevel level) {
		// === 1. Main Hall (15x20) — Elder Whisper's hall ===
		int mainHallX = CENTER_X - 7;
		int mainHallZ = CENTER_Z - 10;
		int mainHallY = ValleyHeightmap.getHeight(CENTER_X, CENTER_Z);
		BlockPos mainHallOrigin = new BlockPos(mainHallX, mainHallY, mainHallZ);

		// Foundation layer
		BuildingPlacer.fill(level, mainHallOrigin.below(), 15, 20, 1, Blocks.COBBLESTONE);
		// Room: 15 wide, 20 deep, 6 tall
		BuildingPlacer.placeRoom(level, mainHallOrigin, 15, 20, 6,
				Blocks.OAK_PLANKS, Blocks.OAK_PLANKS, Blocks.OAK_PLANKS);
		// Front door (centered on the south wall, z = mainHallZ)
		BuildingPlacer.placeDoor(level, new BlockPos(CENTER_X, mainHallY + 1, mainHallZ));
		// Back door
		BuildingPlacer.placeDoor(level, new BlockPos(CENTER_X, mainHallY + 1, mainHallZ + 19));
		// Raised platform at the far end (where Elder Whisper sits) — 11x6 stone platform
		BlockPos platformOrigin = new BlockPos(mainHallX + 2, mainHallY + 1, mainHallZ + 12);
		BuildingPlacer.fill(level, platformOrigin, 11, 6, 1, Blocks.SMOOTH_STONE);
		// Lectern on the platform
		BuildingPlacer.placeBlock(level, platformOrigin.offset(5, 1, 3), Blocks.LECTERN);
		// Torches inside the main hall — along walls
		for (int i = 3; i < 18; i += 4) {
			level.setBlock(new BlockPos(mainHallX + 1, mainHallY + 3, mainHallZ + i),
					Blocks.TORCH.defaultBlockState(), 2);
			level.setBlock(new BlockPos(mainHallX + 13, mainHallY + 3, mainHallZ + i),
					Blocks.TORCH.defaultBlockState(), 2);
		}
		// Lanterns flanking the platform
		BuildingPlacer.placeBlock(level, platformOrigin.offset(0, 1, 0), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, platformOrigin.offset(10, 1, 0), Blocks.LANTERN);

		// === 2. Training Grounds (20x20) — open area, east of main hall ===
		int trainingX = CENTER_X + 15;
		int trainingZ = CENTER_Z - 10;
		int trainingY = ValleyHeightmap.getHeight(trainingX + 10, trainingZ + 10);
		BlockPos trainingOrigin = new BlockPos(trainingX, trainingY, trainingZ);

		// Stone floor
		BuildingPlacer.fill(level, trainingOrigin, 20, 20, 1, Blocks.STONE);
		// Clear air above the training grounds
		BuildingPlacer.fill(level, trainingOrigin.above(), 20, 20, 4, Blocks.AIR);
		// Fence around the perimeter
		BuildingPlacer.placeFence(level, trainingOrigin.above(), 20, 20, Blocks.OAK_FENCE);
		// Hay bale targets in a row along the east side
		for (int i = 2; i < 18; i += 4) {
			BuildingPlacer.placeBlock(level, new BlockPos(trainingX + 17, trainingY + 1, trainingZ + i), Blocks.HAY_BLOCK);
			BuildingPlacer.placeBlock(level, new BlockPos(trainingX + 17, trainingY + 2, trainingZ + i), Blocks.HAY_BLOCK);
		}
		// Fence post targets (practice striking dummies) scattered in center
		BuildingPlacer.placeBlock(level, new BlockPos(trainingX + 6, trainingY + 1, trainingZ + 6), Blocks.OAK_FENCE);
		BuildingPlacer.placeBlock(level, new BlockPos(trainingX + 6, trainingY + 2, trainingZ + 6), Blocks.OAK_FENCE);
		BuildingPlacer.placeBlock(level, new BlockPos(trainingX + 12, trainingY + 1, trainingZ + 6), Blocks.OAK_FENCE);
		BuildingPlacer.placeBlock(level, new BlockPos(trainingX + 12, trainingY + 2, trainingZ + 6), Blocks.OAK_FENCE);
		BuildingPlacer.placeBlock(level, new BlockPos(trainingX + 9, trainingY + 1, trainingZ + 13), Blocks.OAK_FENCE);
		BuildingPlacer.placeBlock(level, new BlockPos(trainingX + 9, trainingY + 2, trainingZ + 13), Blocks.OAK_FENCE);
		// Torches at corners of the training grounds
		BuildingPlacer.placeBlock(level, new BlockPos(trainingX + 1, trainingY + 1, trainingZ + 1), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(trainingX + 18, trainingY + 1, trainingZ + 1), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(trainingX + 1, trainingY + 1, trainingZ + 18), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(trainingX + 18, trainingY + 1, trainingZ + 18), Blocks.TORCH);

		// === 3. Transcription Hall (10x12) — southwest of main hall ===
		int transX = CENTER_X - 20;
		int transZ = CENTER_Z + 5;
		int transY = ValleyHeightmap.getHeight(transX + 5, transZ + 6);
		BlockPos transOrigin = new BlockPos(transX, transY, transZ);

		BuildingPlacer.fill(level, transOrigin.below(), 10, 12, 1, Blocks.COBBLESTONE);
		BuildingPlacer.placeRoom(level, transOrigin, 10, 12, 5,
				Blocks.OAK_PLANKS, Blocks.OAK_PLANKS, Blocks.OAK_PLANKS);
		// Door on the east side (facing the village center)
		BuildingPlacer.placeDoor(level, new BlockPos(transX + 9, transY + 1, transZ + 5));
		// Bookshelves along the west wall
		for (int z = 1; z < 11; z++) {
			BuildingPlacer.placeBlock(level, new BlockPos(transX + 1, transY + 1, transZ + z), Blocks.BOOKSHELF);
			BuildingPlacer.placeBlock(level, new BlockPos(transX + 1, transY + 2, transZ + z), Blocks.BOOKSHELF);
		}
		// Bookshelves along the north wall
		for (int x = 2; x < 9; x++) {
			BuildingPlacer.placeBlock(level, new BlockPos(transX + x, transY + 1, transZ + 1), Blocks.BOOKSHELF);
		}
		// Lecterns for reading/transcription — two in the center
		BuildingPlacer.placeBlock(level, new BlockPos(transX + 5, transY + 1, transZ + 5), Blocks.LECTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(transX + 5, transY + 1, transZ + 8), Blocks.LECTERN);
		// Crafting tables as desks
		BuildingPlacer.placeBlock(level, new BlockPos(transX + 3, transY + 1, transZ + 5), Blocks.CRAFTING_TABLE);
		BuildingPlacer.placeBlock(level, new BlockPos(transX + 3, transY + 1, transZ + 8), Blocks.CRAFTING_TABLE);
		// Lanterns for reading light
		BuildingPlacer.placeBlock(level, new BlockPos(transX + 5, transY + 1, transZ + 3), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(transX + 5, transY + 1, transZ + 10), Blocks.LANTERN);

		// === 4. Residential Houses (6 small 7x7 houses) ===
		int[][] houseOffsets = {
				{-25, -20},  // northwest
				{-18, -25},  // north
				{ 12,  12},  // southeast
				{ 18,  15},  // south-southeast
				{-22,  18},  // southwest
				{ 15, -22},  // northeast
		};
		for (int[] offset : houseOffsets) {
			placeSmallHouse(level, CENTER_X + offset[0], CENTER_Z + offset[1]);
		}

		// === 5. Wei Family Home (10x10) — slightly north of center ===
		int weiFamilyX = CENTER_X - 5;
		int weiFamilyZ = CENTER_Z - 22;
		int weiFamilyY = ValleyHeightmap.getHeight(weiFamilyX + 5, weiFamilyZ + 5);
		BlockPos weiOrigin = new BlockPos(weiFamilyX, weiFamilyY, weiFamilyZ);

		BuildingPlacer.fill(level, weiOrigin.below(), 10, 10, 1, Blocks.COBBLESTONE);
		BuildingPlacer.placeRoom(level, weiOrigin, 10, 10, 5,
				Blocks.OAK_PLANKS, Blocks.OAK_PLANKS, Blocks.OAK_PLANKS);
		// Door on the south side (facing village)
		BuildingPlacer.placeDoor(level, new BlockPos(weiFamilyX + 5, weiFamilyY + 1, weiFamilyZ + 9));
		// Interior furnishings
		BuildingPlacer.placeBlock(level, new BlockPos(weiFamilyX + 1, weiFamilyY + 1, weiFamilyZ + 1), Blocks.FURNACE);
		BuildingPlacer.placeBlock(level, new BlockPos(weiFamilyX + 2, weiFamilyY + 1, weiFamilyZ + 1), Blocks.CRAFTING_TABLE);
		// Beds along east wall
		BuildingPlacer.placeBlock(level, new BlockPos(weiFamilyX + 8, weiFamilyY + 1, weiFamilyZ + 2), Blocks.RED_BED);
		BuildingPlacer.placeBlock(level, new BlockPos(weiFamilyX + 8, weiFamilyY + 1, weiFamilyZ + 5), Blocks.RED_BED);
		// Bookshelf along north wall
		for (int x = 3; x < 8; x++) {
			BuildingPlacer.placeBlock(level, new BlockPos(weiFamilyX + x, weiFamilyY + 1, weiFamilyZ + 1), Blocks.BOOKSHELF);
		}
		// Banner decorations (white banners to represent the Wei clan)
		BuildingPlacer.placeBlock(level, new BlockPos(weiFamilyX + 1, weiFamilyY + 3, weiFamilyZ + 5), Blocks.WHITE_BANNER);
		BuildingPlacer.placeBlock(level, new BlockPos(weiFamilyX + 8, weiFamilyY + 3, weiFamilyZ + 5), Blocks.WHITE_BANNER);
		// Lanterns
		BuildingPlacer.placeBlock(level, new BlockPos(weiFamilyX + 3, weiFamilyY + 1, weiFamilyZ + 5), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(weiFamilyX + 7, weiFamilyY + 1, weiFamilyZ + 5), Blocks.LANTERN);

		// === 6. Village Paths — gravel connections between buildings ===
		// Central hub point at the village center
		int pathY = ValleyHeightmap.getHeight(CENTER_X, CENTER_Z);
		BlockPos villageCenter = new BlockPos(CENTER_X, pathY, CENTER_Z);

		// Main hall front door to village center
		BuildingPlacer.placePath(level,
				new BlockPos(CENTER_X, mainHallY, mainHallZ),
				villageCenter, 2, Blocks.GRAVEL);

		// Village center to training grounds
		BuildingPlacer.placePath(level, villageCenter,
				new BlockPos(trainingX, trainingY, trainingZ + 10), 2, Blocks.GRAVEL);

		// Village center to transcription hall
		BuildingPlacer.placePath(level, villageCenter,
				new BlockPos(transX + 9, transY, transZ + 5), 2, Blocks.GRAVEL);

		// Village center to Wei family home
		BuildingPlacer.placePath(level, villageCenter,
				new BlockPos(weiFamilyX + 5, weiFamilyY, weiFamilyZ + 9), 2, Blocks.GRAVEL);

		// Paths from village center to each residential house
		for (int[] offset : houseOffsets) {
			int hx = CENTER_X + offset[0] + 3; // center of 7-wide house
			int hz = CENTER_Z + offset[1] + 3;
			int hy = ValleyHeightmap.getHeight(hx, hz);
			BuildingPlacer.placePath(level, villageCenter,
					new BlockPos(hx, hy, hz), 2, Blocks.GRAVEL);
		}

		// === 7. Exterior Lighting — torches along main paths ===
		// Torches around the village center
		BuildingPlacer.placeBlock(level, new BlockPos(CENTER_X + 3, pathY + 1, CENTER_Z), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(CENTER_X - 3, pathY + 1, CENTER_Z), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(CENTER_X, pathY + 1, CENTER_Z + 3), Blocks.TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(CENTER_X, pathY + 1, CENTER_Z - 3), Blocks.TORCH);
		// Lanterns at the main hall entrance
		BuildingPlacer.placeBlock(level, new BlockPos(CENTER_X - 2, mainHallY + 1, mainHallZ - 1), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(CENTER_X + 2, mainHallY + 1, mainHallZ - 1), Blocks.LANTERN);
		// Lanterns at Wei family home entrance
		BuildingPlacer.placeBlock(level, new BlockPos(weiFamilyX + 3, weiFamilyY + 1, weiFamilyZ + 10), Blocks.LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(weiFamilyX + 7, weiFamilyY + 1, weiFamilyZ + 10), Blocks.LANTERN);
	}

	/**
	 * Places a small 7x7 residential house at the given position.
	 */
	private static void placeSmallHouse(ServerLevel level, int x, int z) {
		int y = ValleyHeightmap.getHeight(x + 3, z + 3);
		BlockPos origin = new BlockPos(x, y, z);

		// Foundation
		BuildingPlacer.fill(level, origin.below(), 7, 7, 1, Blocks.COBBLESTONE);
		// Room: 7x7, 4 tall
		BuildingPlacer.placeRoom(level, origin, 7, 7, 4,
				Blocks.OAK_PLANKS, Blocks.OAK_PLANKS, Blocks.OAK_PLANKS);
		// Door on the south side
		BuildingPlacer.placeDoor(level, new BlockPos(x + 3, y + 1, z + 6));
		// Basic furnishings
		BuildingPlacer.placeBlock(level, new BlockPos(x + 1, y + 1, z + 1), Blocks.FURNACE);
		BuildingPlacer.placeBlock(level, new BlockPos(x + 2, y + 1, z + 1), Blocks.CRAFTING_TABLE);
		BuildingPlacer.placeBlock(level, new BlockPos(x + 5, y + 1, z + 1), Blocks.RED_BED);
		// Torch inside
		BuildingPlacer.placeBlock(level, new BlockPos(x + 3, y + 1, z + 3), Blocks.TORCH);
		// Lantern outside the door
		BuildingPlacer.placeBlock(level, new BlockPos(x + 3, y + 1, z + 7), Blocks.LANTERN);
	}
}

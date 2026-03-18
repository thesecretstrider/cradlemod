package com.cradle.mod.worldgen.structure;

import com.cradle.mod.worldgen.ValleyHeightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Heaven's Glory School — a stone temple complex built into a mountainside
 * in the southwest of Sacred Valley.
 *
 * Layout (south = +Z, east = +X):
 *   - Entrance gate with stairs leading up from the valley floor
 *   - Training courtyard (20x20 open area)
 *   - Main temple (25x30, high ceiling with pillars)
 *   - Elder quarters (10x8 rooms behind the temple)
 *   - Treasure vault (underground, below main temple)
 */
public class HeavensGloryStructure {

	// Southwest of the valley
	private static final int BASE_X = -400;
	private static final int BASE_Z = 300;

	public static void generate(ServerLevel level) {
		int groundY = ValleyHeightmap.getHeight(BASE_X, BASE_Z);

		// The temple sits slightly elevated from surrounding terrain
		int templeY = groundY + 1;

		generateTrainingCourtyard(level, templeY);
		generateMainTemple(level, templeY);
		generatePillars(level, templeY);
		generateElderQuarters(level, templeY);
		generateTreasureVault(level, templeY);
		generateEntranceGate(level, groundY, templeY);
		generateLighting(level, templeY);
	}

	/**
	 * Training courtyard — 20x20 open-air area in front (south) of the temple.
	 * Smooth stone floor with a low fence around it.
	 */
	private static void generateTrainingCourtyard(ServerLevel level, int templeY) {
		int courtX = BASE_X - 10; // centered on temple center
		int courtZ = BASE_Z + 30; // in front of the temple (south)

		// Level the ground — place a solid floor platform
		BuildingPlacer.fill(level, new BlockPos(courtX, templeY - 1, courtZ),
				20, 20, 1, Blocks.SMOOTH_STONE);

		// Clear above the courtyard so it's open air
		BuildingPlacer.fill(level, new BlockPos(courtX, templeY, courtZ),
				20, 20, 5, Blocks.AIR);

		// Low fence around the perimeter
		BuildingPlacer.placeFence(level, new BlockPos(courtX, templeY, courtZ),
				20, 20, Blocks.STONE_BRICK_WALL);
	}

	/**
	 * Main temple — 25x30 stone brick structure with a 6-block high ceiling.
	 * Grand entrance on the south side (facing the courtyard).
	 */
	private static void generateMainTemple(ServerLevel level, int templeY) {
		int templeX = BASE_X - 12; // centered, slightly wider than courtyard
		int templeZ = BASE_Z;      // temple starts at base position, extends north

		// Place the main room: 25 wide (X), 30 deep (Z), 8 tall (floor + 6 interior + roof)
		BuildingPlacer.placeRoom(level, new BlockPos(templeX, templeY, templeZ),
				25, 30, 8,
				Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);

		// Grand entrance — 3-wide doorway on south wall (z = templeZ + 29, centered on X)
		int doorCenterX = templeX + 12;
		int doorZ = templeZ + 29; // south wall (high Z end)
		for (int dx = -1; dx <= 1; dx++) {
			BuildingPlacer.placeDoor(level, new BlockPos(doorCenterX + dx, templeY + 1, doorZ));
			// Third block high for a grand entrance
			BuildingPlacer.placeBlock(level, new BlockPos(doorCenterX + dx, templeY + 3, doorZ), Blocks.AIR);
		}

		// Entrance stairs — 3 wide, stepping down from temple floor to courtyard level
		for (int step = 0; step < 3; step++) {
			for (int dx = -1; dx <= 1; dx++) {
				BlockPos stairPos = new BlockPos(doorCenterX + dx, templeY - step, doorZ + 1 + step);
				BuildingPlacer.placeBlock(level, stairPos, Blocks.STONE_BRICK_STAIRS);
				// Clear above stairs
				BuildingPlacer.placeBlock(level, stairPos.above(), Blocks.AIR);
				BuildingPlacer.placeBlock(level, stairPos.above().above(), Blocks.AIR);
			}
		}

		// Altar at the north end of the temple interior
		int altarX = templeX + 11;
		int altarZ = templeZ + 2;
		BuildingPlacer.placeBlock(level, new BlockPos(altarX, templeY + 1, altarZ), Blocks.CHISELED_STONE_BRICKS);
		BuildingPlacer.placeBlock(level, new BlockPos(altarX, templeY + 2, altarZ), Blocks.SOUL_LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(altarX - 1, templeY + 1, altarZ), Blocks.CHISELED_STONE_BRICKS);
		BuildingPlacer.placeBlock(level, new BlockPos(altarX + 1, templeY + 1, altarZ), Blocks.CHISELED_STONE_BRICKS);
	}

	/**
	 * Interior pillars — chiseled stone brick columns running down both sides of the temple.
	 */
	private static void generatePillars(ServerLevel level, int templeY) {
		int templeX = BASE_X - 12;
		int templeZ = BASE_Z;

		// Two rows of pillars, 4 blocks in from each wall, spaced every 6 blocks along Z
		int leftPillarX = templeX + 4;
		int rightPillarX = templeX + 20;

		for (int zi = 0; zi < 5; zi++) {
			int pillarZ = templeZ + 4 + zi * 6;
			// Each pillar is a single column of chiseled stone bricks, floor to ceiling
			for (int py = 1; py <= 6; py++) {
				BuildingPlacer.placeBlock(level, new BlockPos(leftPillarX, templeY + py, pillarZ),
						Blocks.CHISELED_STONE_BRICKS);
				BuildingPlacer.placeBlock(level, new BlockPos(rightPillarX, templeY + py, pillarZ),
						Blocks.CHISELED_STONE_BRICKS);
			}
		}
	}

	/**
	 * Elder quarters — two 10x8 stone rooms behind (north of) the main temple.
	 */
	private static void generateElderQuarters(ServerLevel level, int templeY) {
		int templeX = BASE_X - 12;
		int templeZ = BASE_Z;

		// Left elder room — northwest of temple
		int leftRoomX = templeX;
		int leftRoomZ = templeZ - 8;
		BuildingPlacer.placeRoom(level, new BlockPos(leftRoomX, templeY, leftRoomZ),
				10, 8, 5,
				Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);

		// Door connecting to the main temple (south wall of elder room)
		BuildingPlacer.placeDoor(level, new BlockPos(leftRoomX + 5, templeY + 1, leftRoomZ + 7));

		// Right elder room — northeast of temple
		int rightRoomX = templeX + 15;
		int rightRoomZ = templeZ - 8;
		BuildingPlacer.placeRoom(level, new BlockPos(rightRoomX, templeY, rightRoomZ),
				10, 8, 5,
				Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);

		// Door connecting to the main temple
		BuildingPlacer.placeDoor(level, new BlockPos(rightRoomX + 5, templeY + 1, rightRoomZ + 7));

		// Furnishings — a chest and redstone torch in each room
		BuildingPlacer.placeBlock(level, new BlockPos(leftRoomX + 2, templeY + 1, leftRoomZ + 2), Blocks.CHEST);
		BuildingPlacer.placeBlock(level, new BlockPos(leftRoomX + 7, templeY + 1, leftRoomZ + 2), Blocks.REDSTONE_TORCH);

		BuildingPlacer.placeBlock(level, new BlockPos(rightRoomX + 2, templeY + 1, rightRoomZ + 2), Blocks.CHEST);
		BuildingPlacer.placeBlock(level, new BlockPos(rightRoomX + 7, templeY + 1, rightRoomZ + 2), Blocks.REDSTONE_TORCH);
	}

	/**
	 * Treasure vault — underground room beneath the main temple.
	 * Accessed conceptually from the altar area. Iron block walls, chests, and armor stands.
	 */
	private static void generateTreasureVault(ServerLevel level, int templeY) {
		int templeX = BASE_X - 12;
		int templeZ = BASE_Z;

		// Vault sits underground, centered below the northern part of the temple
		int vaultX = templeX + 5;
		int vaultZ = templeZ + 3;
		int vaultY = templeY - 7; // well below the temple floor

		// Vault room: 15 wide, 12 deep, 6 tall
		BuildingPlacer.placeRoom(level, new BlockPos(vaultX, vaultY, vaultZ),
				15, 12, 6,
				Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);

		// Iron block accents along the walls (every other block on north and south walls)
		for (int ix = 1; ix < 14; ix += 2) {
			BuildingPlacer.placeBlock(level, new BlockPos(vaultX + ix, vaultY + 1, vaultZ), Blocks.IRON_BLOCK);
			BuildingPlacer.placeBlock(level, new BlockPos(vaultX + ix, vaultY + 1, vaultZ + 11), Blocks.IRON_BLOCK);
		}

		// Chests along the east wall
		for (int cz = 2; cz < 10; cz += 3) {
			BuildingPlacer.placeBlock(level, new BlockPos(vaultX + 13, vaultY + 1, vaultZ + cz), Blocks.CHEST);
		}

		// Armor stands represented as iron blocks on the west wall (placeholder)
		for (int az = 2; az < 10; az += 3) {
			BuildingPlacer.placeBlock(level, new BlockPos(vaultX + 1, vaultY + 1, vaultZ + az), Blocks.IRON_BLOCK);
			BuildingPlacer.placeBlock(level, new BlockPos(vaultX + 1, vaultY + 2, vaultZ + az), Blocks.IRON_BLOCK);
		}

		// Staircase from temple floor down to vault
		int stairX = templeX + 12; // centered in temple
		int stairZ = templeZ + 3;
		int stairDepth = templeY - vaultY - 1; // number of steps down
		for (int step = 0; step < stairDepth; step++) {
			BlockPos stepPos = new BlockPos(stairX, templeY - step, stairZ + step);
			BuildingPlacer.placeBlock(level, stepPos, Blocks.STONE_BRICK_STAIRS);
			// Clear headroom above each step
			BuildingPlacer.placeBlock(level, stepPos.above(), Blocks.AIR);
			BuildingPlacer.placeBlock(level, stepPos.above().above(), Blocks.AIR);
			BuildingPlacer.placeBlock(level, stepPos.above().above().above(), Blocks.AIR);
			// Widen the staircase
			BlockPos sidePos = new BlockPos(stairX + 1, templeY - step, stairZ + step);
			BuildingPlacer.placeBlock(level, sidePos, Blocks.STONE_BRICK_STAIRS);
			BuildingPlacer.placeBlock(level, sidePos.above(), Blocks.AIR);
			BuildingPlacer.placeBlock(level, sidePos.above().above(), Blocks.AIR);
			BuildingPlacer.placeBlock(level, sidePos.above().above().above(), Blocks.AIR);
		}

		// Lighting inside the vault
		BuildingPlacer.placeBlock(level, new BlockPos(vaultX + 4, vaultY + 4, vaultZ + 6), Blocks.SOUL_LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(vaultX + 10, vaultY + 4, vaultZ + 6), Blocks.SOUL_LANTERN);
	}

	/**
	 * Entrance gate — decorated archway with stairs from valley floor up to the temple.
	 * Located at the south side of the courtyard, stepping up from the valley floor.
	 */
	private static void generateEntranceGate(ServerLevel level, int groundY, int templeY) {
		int courtX = BASE_X - 10;
		int courtZ = BASE_Z + 30;

		// Gate position — centered at south edge of courtyard
		int gateX = courtX + 10;
		int gateZ = courtZ + 20;

		// Gate archway — two pillars with a crossbar
		int gateY = ValleyHeightmap.getHeight(gateX, gateZ);
		for (int py = 0; py < 6; py++) {
			BuildingPlacer.placeBlock(level, new BlockPos(gateX - 2, gateY + py, gateZ),
					Blocks.CHISELED_STONE_BRICKS);
			BuildingPlacer.placeBlock(level, new BlockPos(gateX + 2, gateY + py, gateZ),
					Blocks.CHISELED_STONE_BRICKS);
		}
		// Crossbar at the top
		for (int dx = -2; dx <= 2; dx++) {
			BuildingPlacer.placeBlock(level, new BlockPos(gateX + dx, gateY + 5, gateZ),
					Blocks.CHISELED_STONE_BRICKS);
			BuildingPlacer.placeBlock(level, new BlockPos(gateX + dx, gateY + 6, gateZ),
					Blocks.STONE_BRICKS);
		}

		// Lanterns on the gate pillars
		BuildingPlacer.placeBlock(level, new BlockPos(gateX - 2, gateY + 4, gateZ - 1), Blocks.SOUL_LANTERN);
		BuildingPlacer.placeBlock(level, new BlockPos(gateX + 2, gateY + 4, gateZ - 1), Blocks.SOUL_LANTERN);

		// Path from gate up to courtyard
		BuildingPlacer.placePath(level,
				new BlockPos(gateX, gateY, gateZ),
				new BlockPos(gateX, templeY - 1, courtZ + 19),
				3, Blocks.SMOOTH_STONE);
	}

	/**
	 * Lighting — soul lanterns and redstone torches throughout the temple.
	 */
	private static void generateLighting(ServerLevel level, int templeY) {
		int templeX = BASE_X - 12;
		int templeZ = BASE_Z;

		// Soul lanterns hanging from the ceiling inside the temple, between pillars
		int leftPillarX = templeX + 4;
		int rightPillarX = templeX + 20;
		int lanternY = templeY + 6; // just below the roof

		for (int zi = 0; zi < 5; zi++) {
			int lanternZ = templeZ + 4 + zi * 6;
			BuildingPlacer.placeBlock(level, new BlockPos(leftPillarX, lanternY, lanternZ), Blocks.SOUL_LANTERN);
			BuildingPlacer.placeBlock(level, new BlockPos(rightPillarX, lanternY, lanternZ), Blocks.SOUL_LANTERN);
		}

		// Central lantern row
		int centerX = templeX + 12;
		for (int zi = 0; zi < 4; zi++) {
			int lanternZ = templeZ + 7 + zi * 6;
			BuildingPlacer.placeBlock(level, new BlockPos(centerX, lanternY, lanternZ), Blocks.SOUL_LANTERN);
		}

		// Redstone torches at the courtyard corners
		int courtX = BASE_X - 10;
		int courtZ = BASE_Z + 30;
		BuildingPlacer.placeBlock(level, new BlockPos(courtX, templeY + 1, courtZ), Blocks.REDSTONE_TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(courtX + 19, templeY + 1, courtZ), Blocks.REDSTONE_TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(courtX, templeY + 1, courtZ + 19), Blocks.REDSTONE_TORCH);
		BuildingPlacer.placeBlock(level, new BlockPos(courtX + 19, templeY + 1, courtZ + 19), Blocks.REDSTONE_TORCH);
	}
}

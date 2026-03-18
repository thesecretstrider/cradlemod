package com.cradle.mod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Utility methods for procedurally placing structures in the world.
 * All methods clear the interior (fill with air) before placing walls.
 */
public class BuildingPlacer {

	/**
	 * Place a rectangular room with walls, floor, and roof.
	 * Origin is the bottom-northwest corner (min x, min y, min z).
	 */
	public static void placeRoom(ServerLevel level, BlockPos origin,
			int width, int depth, int height,
			Block wallBlock, Block floorBlock, Block roofBlock) {
		BlockState wall = wallBlock.defaultBlockState();
		BlockState floor = floorBlock.defaultBlockState();
		BlockState roof = roofBlock.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();

		for (int x = 0; x < width; x++) {
			for (int z = 0; z < depth; z++) {
				for (int y = 0; y < height; y++) {
					pos.set(ox + x, oy + y, oz + z);
					boolean isWall = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
					boolean isFloor = y == 0;
					boolean isRoof = y == height - 1;

					if (isFloor) {
						level.setBlock(pos, floor, 2);
					} else if (isRoof) {
						level.setBlock(pos, roof, 2);
					} else if (isWall) {
						level.setBlock(pos, wall, 2);
					} else {
						level.setBlock(pos, air, 2);
					}
				}
			}
		}
	}

	/**
	 * Place a path/road between two points at a fixed Y level.
	 * Uses Bresenham-style line for the center, then widens to the given width.
	 */
	public static void placePath(ServerLevel level, BlockPos from, BlockPos to,
			int width, Block pathBlock) {
		BlockState state = pathBlock.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		int dx = to.getX() - from.getX();
		int dz = to.getZ() - from.getZ();
		int steps = Math.max(Math.abs(dx), Math.abs(dz));
		if (steps == 0) return;

		int halfWidth = width / 2;
		for (int i = 0; i <= steps; i++) {
			int x = from.getX() + dx * i / steps;
			int z = from.getZ() + dz * i / steps;
			int y = from.getY() + (to.getY() - from.getY()) * i / steps;
			for (int wx = -halfWidth; wx <= halfWidth; wx++) {
				for (int wz = -halfWidth; wz <= halfWidth; wz++) {
					pos.set(x + wx, y, z + wz);
					level.setBlock(pos, state, 2);
					// Clear blocks above the path
					pos.set(x + wx, y + 1, z + wz);
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
					pos.set(x + wx, y + 2, z + wz);
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	/**
	 * Place a rectangular fence around an area. Origin is the bottom-northwest corner.
	 */
	public static void placeFence(ServerLevel level, BlockPos origin,
			int width, int depth, Block fenceBlock) {
		BlockState state = fenceBlock.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < depth; z++) {
				if (x == 0 || x == width - 1 || z == 0 || z == depth - 1) {
					pos.set(ox + x, oy, oz + z);
					level.setBlock(pos, state, 2);
				}
			}
		}
	}

	/**
	 * Place a solid cylinder (tower/pillar).
	 */
	public static void placeTower(ServerLevel level, BlockPos base,
			int radius, int height, Block block) {
		BlockState state = block.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		int bx = base.getX(), by = base.getY(), bz = base.getZ();
		int r2 = radius * radius;
		for (int y = 0; y < height; y++) {
			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					if (x * x + z * z <= r2) {
						pos.set(bx + x, by + y, bz + z);
						level.setBlock(pos, state, 2);
					}
				}
			}
		}
	}

	/**
	 * Fill a rectangular area with a single block (useful for clearing or platforms).
	 */
	public static void fill(ServerLevel level, BlockPos origin,
			int width, int depth, int height, Block block) {
		BlockState state = block.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < depth; z++) {
				for (int y = 0; y < height; y++) {
					pos.set(ox + x, oy + y, oz + z);
					level.setBlock(pos, state, 2);
				}
			}
		}
	}

	/**
	 * Place a doorway (2-high air gap) in a wall.
	 * Position is the bottom block of the door.
	 */
	public static void placeDoor(ServerLevel level, BlockPos doorPos) {
		level.setBlock(doorPos, Blocks.AIR.defaultBlockState(), 2);
		level.setBlock(doorPos.above(), Blocks.AIR.defaultBlockState(), 2);
	}

	/**
	 * Place a single block at a position.
	 */
	public static void placeBlock(ServerLevel level, BlockPos pos, Block block) {
		level.setBlock(pos, block.defaultBlockState(), 2);
	}
}

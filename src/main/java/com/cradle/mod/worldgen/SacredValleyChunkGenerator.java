package com.cradle.mod.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Custom chunk generator for Sacred Valley (Cradle mode).
 * Generates a bowl-shaped valley with Mount Samara, mountain ring,
 * rivers, forests, and surface decoration.
 */
public class SacredValleyChunkGenerator extends ChunkGenerator {

	public static final MapCodec<SacredValleyChunkGenerator> CODEC =
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource)
			).apply(instance, SacredValleyChunkGenerator::new));

	private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
	private static final BlockState STONE = Blocks.STONE.defaultBlockState();
	private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
	private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
	private static final BlockState SNOW = Blocks.SNOW_BLOCK.defaultBlockState();
	private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
	private static final BlockState BARRIER = Blocks.BARRIER.defaultBlockState();
	private static final BlockState WATER = Blocks.WATER.defaultBlockState();
	private static final BlockState SAND = Blocks.SAND.defaultBlockState();
	private static final BlockState OAK_LOG = Blocks.OAK_LOG.defaultBlockState();
	private static final BlockState OAK_LEAVES = Blocks.OAK_LEAVES.defaultBlockState();
	private static final BlockState DARK_OAK_LOG = Blocks.DARK_OAK_LOG.defaultBlockState();
	private static final BlockState DARK_OAK_LEAVES = Blocks.DARK_OAK_LEAVES.defaultBlockState();
	private static final BlockState SHORT_GRASS = Blocks.SHORT_GRASS.defaultBlockState();
	private static final BlockState POPPY = Blocks.POPPY.defaultBlockState();
	private static final BlockState DANDELION = Blocks.DANDELION.defaultBlockState();
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	private static final int RIVER_SURFACE_Y = 63;

	public SacredValleyChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(
			Blender blender, RandomState randomState,
			StructureManager structureManager, ChunkAccess chunk) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

		int chunkX = chunk.getPos().x;
		int chunkZ = chunk.getPos().z;
		int minY = chunk.getMinY();
		int maxY = minY + chunk.getHeight();

		for (int localX = 0; localX < 16; localX++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				int worldX = chunkX * 16 + localX;
				int worldZ = chunkZ * 16 + localZ;
				int surfaceY = ValleyHeightmap.getHeight(worldX, worldZ);
				ValleyBiomePainter.Zone zone = ValleyBiomePainter.getZone(worldX, worldZ);

				for (int y = minY; y < maxY; y++) {
					BlockState state = getBlockAt(y, surfaceY, worldX, worldZ, zone);
					if (state.isAir()) continue;

					chunk.setBlockState(pos.set(localX, y, localZ), state, 0);
					oceanFloor.update(localX, y, localZ, state);
					worldSurface.update(localX, y, localZ, state);
				}
			}
		}

		return CompletableFuture.completedFuture(chunk);
	}

	private BlockState getBlockAt(int y, int surfaceY, int worldX, int worldZ, ValleyBiomePainter.Zone zone) {
		if (y == -64) return BEDROCK;

		// River zones — water at river surface, sand at bottom
		if (zone == ValleyBiomePainter.Zone.RIVER) {
			if (y > RIVER_SURFACE_Y) return AIR;
			if (y > surfaceY - 4 && y <= RIVER_SURFACE_Y) return WATER;
			if (y == surfaceY - 4) return SAND;
			if (y < surfaceY - 4) return STONE;
		}

		if (y > surfaceY) {
			// Barrier wall at outer edge of mountain ring
			double dist = Math.sqrt(worldX * worldX + worldZ * worldZ);
			if (dist >= ValleyHeightmap.VALLEY_RADIUS && y <= ValleyHeightmap.MOUNTAIN_PEAK_Y + 5) {
				return BARRIER;
			}
			return AIR;
		}
		if (y == surfaceY) {
			if (surfaceY > 150) return SNOW;
			if (surfaceY > 120) return GRAVEL;
			return GRASS;
		}
		if (y >= surfaceY - 3) return DIRT;
		return STONE;
	}

	@Override
	public void buildSurface(WorldGenRegion region, StructureManager structureManager,
			RandomState randomState, ChunkAccess chunk) {
		ChunkPos chunkPos = chunk.getPos();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int localX = 0; localX < 16; localX++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				int worldX = chunkPos.x * 16 + localX;
				int worldZ = chunkPos.z * 16 + localZ;
				int surfaceY = ValleyHeightmap.getHeight(worldX, worldZ);
				ValleyBiomePainter.Zone zone = ValleyBiomePainter.getZone(worldX, worldZ);

				// Trees
				if (ValleyBiomePainter.shouldPlaceTree(worldX, worldZ, zone)) {
					placeTree(region, pos, worldX, surfaceY + 1, worldZ, zone);
				}
				// Grass and flowers (on non-tree positions)
				else if (ValleyBiomePainter.shouldPlaceGrass(worldX, worldZ, zone)) {
					pos.set(worldX, surfaceY + 1, worldZ);
					if (region.getBlockState(pos).isAir()) {
						if (ValleyBiomePainter.shouldBeFlower(worldX, worldZ)) {
							// Alternate between poppies and dandelions
							region.setBlock(pos, ((worldX + worldZ) % 2 == 0) ? POPPY : DANDELION, 2, 0);
						} else {
							region.setBlock(pos, SHORT_GRASS, 2, 0);
						}
					}
				}
			}
		}
	}

	/**
	 * Place a simple tree at the given position.
	 * Mount Samara gets large dark oak trees; other zones get normal oaks.
	 */
	private void placeTree(WorldGenRegion region, BlockPos.MutableBlockPos pos,
			int x, int baseY, int z, ValleyBiomePainter.Zone zone) {
		BlockState log = (zone == ValleyBiomePainter.Zone.MOUNT_SAMARA) ? DARK_OAK_LOG : OAK_LOG;
		BlockState leaves = (zone == ValleyBiomePainter.Zone.MOUNT_SAMARA) ? DARK_OAK_LEAVES : OAK_LEAVES;
		int trunkHeight = (zone == ValleyBiomePainter.Zone.MOUNT_SAMARA) ? 7 : 5;
		int leafRadius = (zone == ValleyBiomePainter.Zone.MOUNT_SAMARA) ? 3 : 2;

		// Place trunk
		for (int y = 0; y < trunkHeight; y++) {
			pos.set(x, baseY + y, z);
			if (region.getBlockState(pos).isAir()) {
				region.setBlock(pos, log, 2, 0);
			}
		}

		// Place leaf sphere at top of trunk
		int leafBase = baseY + trunkHeight - leafRadius;
		for (int dy = 0; dy <= leafRadius * 2; dy++) {
			int ry = dy - leafRadius;
			int layerRadius = leafRadius - Math.abs(ry);
			for (int dx = -layerRadius; dx <= layerRadius; dx++) {
				for (int dz = -layerRadius; dz <= layerRadius; dz++) {
					if (dx * dx + dz * dz > layerRadius * layerRadius + 1) continue;
					pos.set(x + dx, leafBase + dy, z + dz);
					if (region.getBlockState(pos).isAir()) {
						region.setBlock(pos, leaves, 2, 0);
					}
				}
			}
		}
	}

	@Override
	public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
			BiomeManager biomeManager, StructureManager structureManager,
			ChunkAccess chunk) {
		// No-op — no caves in Sacred Valley
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
		// No-op
	}

	@Override
	public int getGenDepth() {
		return 384;
	}

	@Override
	public int getSeaLevel() {
		return RIVER_SURFACE_Y;
	}

	@Override
	public int getMinY() {
		return -64;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types heightmapType,
			LevelHeightAccessor level, RandomState randomState) {
		return ValleyHeightmap.getHeight(x, z) + 1;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level,
			RandomState randomState) {
		int minY = level.getMinY();
		int height = level.getHeight();
		int surfaceY = ValleyHeightmap.getHeight(x, z);
		ValleyBiomePainter.Zone zone = ValleyBiomePainter.getZone(x, z);
		BlockState[] states = new BlockState[height];
		for (int i = 0; i < height; i++) {
			states[i] = getBlockAt(minY + i, surfaceY, x, z, zone);
		}
		return new NoiseColumn(minY, states);
	}

	@Override
	public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
		int x = pos.getX(), z = pos.getZ();
		info.add("Sacred Valley Generator");
		info.add("Surface Y: " + ValleyHeightmap.getHeight(x, z));
		info.add("Zone: " + ValleyBiomePainter.getZone(x, z));
	}
}

package com.cradle.mod.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
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
 * Generates a bowl-shaped valley with Mount Samara at center and mountain ring perimeter.
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
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

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

				for (int y = minY; y < maxY; y++) {
					BlockState state = getBlockAt(y, surfaceY, worldX, worldZ);
					if (state.isAir()) continue;

					chunk.setBlockState(pos.set(localX, y, localZ), state, 0);
					oceanFloor.update(localX, y, localZ, state);
					worldSurface.update(localX, y, localZ, state);
				}
			}
		}

		return CompletableFuture.completedFuture(chunk);
	}

	private BlockState getBlockAt(int y, int surfaceY, int worldX, int worldZ) {
		if (y == -64) return BEDROCK;
		if (y > surfaceY) {
			// Barrier wall at mountain peaks (outer edge of the ring)
			double dist = Math.sqrt(worldX * worldX + worldZ * worldZ);
			if (dist >= ValleyHeightmap.VALLEY_RADIUS && y <= ValleyHeightmap.MOUNTAIN_PEAK_Y + 5) {
				return BARRIER;
			}
			return AIR;
		}
		if (y == surfaceY) {
			// Snow on mountain peaks
			if (surfaceY > 150) return SNOW;
			// Gravel on steep mountain slopes
			if (surfaceY > 120) return GRAVEL;
			return GRASS;
		}
		if (y >= surfaceY - 3) return DIRT;
		return STONE;
	}

	@Override
	public void buildSurface(WorldGenRegion region, StructureManager structureManager,
			RandomState randomState, ChunkAccess chunk) {
		// No-op — surface is built in fillFromNoise
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
		return -63;
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
		BlockState[] states = new BlockState[height];
		for (int i = 0; i < height; i++) {
			states[i] = getBlockAt(minY + i, surfaceY, x, z);
		}
		return new NoiseColumn(minY, states);
	}

	@Override
	public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
		info.add("Sacred Valley Generator");
		info.add("Surface Y: " + ValleyHeightmap.getHeight(pos.getX(), pos.getZ()));
	}
}

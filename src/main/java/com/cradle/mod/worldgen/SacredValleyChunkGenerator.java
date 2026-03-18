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
 * Currently generates flat terrain at Y=64. Terrain shaping comes in Task 4.
 */
public class SacredValleyChunkGenerator extends ChunkGenerator {

	public static final MapCodec<SacredValleyChunkGenerator> CODEC =
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource)
			).apply(instance, SacredValleyChunkGenerator::new));

	private static final int SURFACE_Y = 64;

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

		int minY = chunk.getMinY();
		int maxY = minY + chunk.getHeight();

		for (int y = minY; y < maxY; y++) {
			BlockState state = getBlockAtHeight(y);
			if (state.isAir()) continue;

			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					chunk.setBlockState(pos.set(x, y, z), state, 0);
					oceanFloor.update(x, y, z, state);
					worldSurface.update(x, y, z, state);
				}
			}
		}

		return CompletableFuture.completedFuture(chunk);
	}

	private BlockState getBlockAtHeight(int y) {
		if (y == -64) return Blocks.BEDROCK.defaultBlockState();
		if (y < SURFACE_Y - 2) return Blocks.STONE.defaultBlockState();
		if (y < SURFACE_Y) return Blocks.DIRT.defaultBlockState();
		if (y == SURFACE_Y) return Blocks.GRASS_BLOCK.defaultBlockState();
		return Blocks.AIR.defaultBlockState();
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
		return -63; // Below the world — no oceans in Sacred Valley
	}

	@Override
	public int getMinY() {
		return -64;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types heightmapType,
			LevelHeightAccessor level, RandomState randomState) {
		return SURFACE_Y + 1;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level,
			RandomState randomState) {
		int minY = level.getMinY();
		int height = level.getHeight();
		BlockState[] states = new BlockState[height];
		for (int i = 0; i < height; i++) {
			states[i] = getBlockAtHeight(minY + i);
		}
		return new NoiseColumn(minY, states);
	}

	@Override
	public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
		info.add("Sacred Valley Generator");
	}
}

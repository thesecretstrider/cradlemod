package com.cradle.mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/**
 * A glowing bush block that drops a fruit item when right-clicked.
 * Cross-shaped like vanilla flowers, emits light level 7.
 * Will not generate within 200 blocks of world origin (0, 0) to prevent
 * players finding them too easily near spawn.
 */
public class FruitBushBlock extends BushBlock {

	/**
	 * Minimum horizontal distance (in blocks) from world origin (0, 0) where
	 * bushes are allowed to generate. This keeps them out of the spawn area
	 * and the immediately surrounding biomes.
	 */
	private static final int SPAWN_EXCLUSION_RADIUS = 200;

	private final Supplier<Item> fruitItem;

	/**
	 * Properties must have setId() called before being passed here.
	 */
	public FruitBushBlock(BlockBehaviour.Properties properties, Supplier<Item> fruitItem) {
		super(properties);
		this.fruitItem = fruitItem;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		// Check parent conditions first (must be on dirt/grass/etc.)
		if (!super.canSurvive(state, level, pos)) {
			return false;
		}

		// Block generation near world origin (spawn area)
		// Uses horizontal distance only (ignores Y)
		double distSq = (double) pos.getX() * pos.getX() + (double) pos.getZ() * pos.getZ();
		if (distSq < (double) SPAWN_EXCLUSION_RADIUS * SPAWN_EXCLUSION_RADIUS) {
			return false;
		}

		return true;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			// Drop the fruit item
			Block.popResource(level, pos, new ItemStack(fruitItem.get()));
			// Remove the bush
			level.destroyBlock(pos, false);
		}
		return InteractionResult.SUCCESS;
	}
}

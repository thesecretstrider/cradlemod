package com.cradle.mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
 */
public class FruitBushBlock extends BushBlock {

	private final Supplier<Item> fruitItem;

	/**
	 * Properties must have setId() called before being passed here.
	 */
	public FruitBushBlock(BlockBehaviour.Properties properties, Supplier<Item> fruitItem) {
		super(properties);
		this.fruitItem = fruitItem;
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

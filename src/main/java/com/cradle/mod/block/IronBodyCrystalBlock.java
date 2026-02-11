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
 * A floating crystal block that appears during special world events.
 * Cross-shaped (like flowers), emits bright light (level 12).
 * Right-click to collect the matching Iron Body crystal item.
 * The block disappears after collection.
 */
public class IronBodyCrystalBlock extends BushBlock {

	private final Supplier<Item> crystalItem;

	public IronBodyCrystalBlock(BlockBehaviour.Properties properties, Supplier<Item> crystalItem) {
		super(properties);
		this.crystalItem = crystalItem;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			Block.popResource(level, pos, new ItemStack(crystalItem.get()));
			level.destroyBlock(pos, false);
		}
		return InteractionResult.SUCCESS;
	}
}

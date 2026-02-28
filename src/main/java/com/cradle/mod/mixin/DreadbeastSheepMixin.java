package com.cradle.mod.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes all sheep behave as dreadbeasts — hostile to players,
 * unable to be fed, bred, or sheared. They will actively attack players on sight.
 *
 * Targets Sheep directly since all methods are defined on the class
 * (unlike cow which uses AbstractCow).
 */
@Mixin(Sheep.class)
public abstract class DreadbeastSheepMixin extends PathfinderMob {

	// Dummy constructor required by compiler — never actually called (mixin)
	protected DreadbeastSheepMixin(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	/**
	 * After vanilla goals are registered, add a player-targeting goal.
	 * Sheep don't have MeleeAttackGoal by default, so we add one too.
	 */
	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void cradleMod$addHostileGoals(CallbackInfo ci) {
		this.goalSelector.addGoal(1,
				new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.2, true));
		this.targetSelector.addGoal(1,
				new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/**
	 * Block all player interaction — no shearing, no feeding, no breeding.
	 */
	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	private void cradleMod$preventInteraction(Player player, InteractionHand hand,
											   CallbackInfoReturnable<InteractionResult> cir) {
		cir.setReturnValue(InteractionResult.PASS);
	}

	/**
	 * No item is food — prevents breeding and luring.
	 */
	@Inject(method = "isFood", at = @At("HEAD"), cancellable = true)
	private void cradleMod$preventFeeding(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}

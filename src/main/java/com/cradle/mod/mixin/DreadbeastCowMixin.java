package com.cradle.mod.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes all cows behave as dreadbeasts — hostile to players,
 * unable to be fed or bred. They will actively attack players on sight.
 *
 * Targets AbstractCow since that's where registerGoals, mobInteract,
 * and isFood are defined (Cow extends AbstractCow).
 */
@Mixin(AbstractCow.class)
public abstract class DreadbeastCowMixin extends PathfinderMob {

	// Dummy constructor required by compiler — never actually called (mixin)
	protected DreadbeastCowMixin(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	/**
	 * After vanilla goals are registered, add a player-targeting goal.
	 * AbstractCow inherits MeleeAttackGoal-compatible pathfinding from Animal/Mob.
	 * We also add a MeleeAttackGoal since cows don't have one by default.
	 */
	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void cradleMod$addHostileGoals(CallbackInfo ci) {
		this.goalSelector.addGoal(1,
				new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.2, true));
		this.targetSelector.addGoal(1,
				new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/**
	 * Block all player interaction — no milking, no feeding, no breeding.
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

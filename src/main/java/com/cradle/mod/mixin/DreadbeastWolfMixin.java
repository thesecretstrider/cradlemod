package com.cradle.mod.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes all wolves behave as dreadbeasts — permanently hostile to players,
 * untameable, and unbreedable. Existing tamed wolves are also untamed.
 *
 * Extends Mob so that we can access the protected {@code targetSelector} field
 * inherited from {@link Mob} without needing an access widener.
 */
@Mixin(Wolf.class)
public abstract class DreadbeastWolfMixin extends Mob {

	// Dummy constructor required by compiler — never actually called (mixin)
	protected DreadbeastWolfMixin(EntityType<? extends Mob> entityType, Level level) {
		super(entityType, level);
	}

	/**
	 * After vanilla goals are registered, add a player-targeting goal.
	 * Wolf already has MeleeAttackGoal from vanilla, so it will attack once it has a target.
	 */
	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void cradleMod$addHostileGoals(CallbackInfo ci) {
		// 'this' is the Wolf instance at runtime. targetSelector is inherited from Mob.
		this.targetSelector.addGoal(1,
				new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/**
	 * Block all player interaction — no taming with bones, no feeding, no sitting.
	 */
	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	private void cradleMod$preventInteraction(Player player, InteractionHand hand,
											   CallbackInfoReturnable<InteractionResult> cir) {
		cir.setReturnValue(InteractionResult.PASS);
	}

	/**
	 * No item is food — prevents breeding and luring with meat.
	 */
	@Inject(method = "isFood", at = @At("HEAD"), cancellable = true)
	private void cradleMod$preventFeeding(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}

	/**
	 * Every tick, if the wolf is somehow tamed (e.g. from before mod install),
	 * strip the tame status so it becomes hostile again.
	 */
	@Inject(method = "tick", at = @At("TAIL"))
	private void cradleMod$untameExisting(CallbackInfo ci) {
		Wolf self = (Wolf) (Object) this;
		if (self.isTame()) {
			self.setTame(false, false);
		}
	}
}

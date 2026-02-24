package com.cradle.mod.mixin.client;

import com.cradle.mod.ClientCradleData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side mixin that enables noclip (phase through blocks) during Herald Spirit Shift.
 *
 * Problem: Player.tick() resets {@code noPhysics = isSpectator()} every tick,
 * overriding the server's setting. Entity.move() reads noPhysics at its very top
 * to decide whether to skip all collision. By injecting at HEAD of move() and
 * setting noPhysics = true right before the check, we enable phasing.
 *
 * Only applies to the local player — other entities are unaffected.
 */
@Mixin(Entity.class)
public abstract class SpiritShiftMixin {

	@Shadow public boolean noPhysics;

	@Inject(method = "move", at = @At("HEAD"))
	private void cradleSpiritShiftNoclip(MoverType moverType, Vec3 movement, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (!self.level().isClientSide()) return;

		// Only affect the local player
		if (!(self instanceof LocalPlayer)) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || self != mc.player) return;

		if (ClientCradleData.spiritShiftActive) {
			this.noPhysics = true;
		}
	}
}

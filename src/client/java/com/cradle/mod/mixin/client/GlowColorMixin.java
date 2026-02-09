package com.cradle.mod.mixin.client;

import com.cradle.mod.ClientCradleData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides the glow outline color for the local player while cycling.
 * The color is based on the player's chosen Path.
 */
@Mixin(Entity.class)
public abstract class GlowColorMixin {

	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void cradleMod$customGlowColor(CallbackInfoReturnable<Integer> cir) {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null && (Object) this == client.player && ClientCradleData.cycling) {
			cir.setReturnValue(getPathColor(ClientCradleData.path));
		}
	}

	private static int getPathColor(String path) {
		return switch (path) {
			case "BLACK_FLAME" -> 0x8B0000;      // dark red
			case "ENDLESS_SWORD" -> 0xCCCCCC;    // silver
			case "STELLAR_SPEAR" -> 0xFFDD44;    // gold
			case "CLOUD_HAMMER" -> 0x444455;      // dark grey/storm
			case "HOLLOW_KING" -> 0xDDDDEE;       // pale white
			default -> 0xC0C0C0;                  // light grey (unset/unknown)
		};
	}
}

package com.cradle.mod.mixin.client;

import com.cradle.mod.ClientCradleData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla XP bar with a Madra bar.
 * The bar color changes based on the player's advancement stage.
 */
@Mixin(ExperienceBarRenderer.class)
public abstract class MadraBarMixin implements ContextualBarRenderer {

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void cradleMod$cancelBackground(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		// Don't render the vanilla XP bar background sprite
		ci.cancel();
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void cradleMod$renderMadraBar(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		// Cancel vanilla XP bar rendering
		ci.cancel();

		// Calculate bar position (same as vanilla XP bar)
		int barLeft = this.left(this.minecraft.getWindow());
		int barTop = this.top(this.minecraft.getWindow());

		// Bar dimensions from ContextualBarRenderer
		int barWidth = ContextualBarRenderer.WIDTH;
		int barHeight = ContextualBarRenderer.HEIGHT;

		// Draw dark background
		graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + barHeight, 0xFF222222);

		// Calculate fill amount based on Madra
		float ratio = ClientCradleData.maxMadra > 0
				? ClientCradleData.currentMadra / ClientCradleData.maxMadra
				: 0f;
		int fillWidth = (int) (barWidth * Math.min(1f, ratio));

		// Draw colored fill based on advancement stage
		if (fillWidth > 0) {
			int color = ClientCradleData.getStageColor();
			graphics.fill(barLeft, barTop, barLeft + fillWidth, barTop + barHeight, color);
		}

		// Draw a subtle 1px border on top for visual definition
		graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 1, 0x44FFFFFF);

		// Draw the player's level number above the bar (same as vanilla)
		Font font = this.minecraft.font;
		ContextualBarRenderer.renderExperienceLevel(graphics, font, ClientCradleData.level);
	}
}

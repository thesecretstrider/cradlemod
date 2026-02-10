package com.cradle.mod;

import com.cradle.mod.network.AttemptAdvancePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * GUI screen showing the player's Sacred Artist status.
 * Opened via /cycle info or the J keybind.
 * Press Escape to close.
 *
 * When the player meets all requirements to advance to the next stage,
 * an "Advance" button appears at the bottom. Clicking it sends a
 * request to the server to perform the breakthrough.
 */
public class CradleInfoScreen extends Screen {

	// Panel dimensions
	private static final int PANEL_WIDTH = 220;
	private static final int PANEL_HEIGHT = 220;

	// Bar dimensions (for XP and Madra bars inside the panel)
	private static final int BAR_WIDTH = 180;
	private static final int BAR_HEIGHT = 8;

	// Advance button dimensions
	private static final int BUTTON_WIDTH = 120;
	private static final int BUTTON_HEIGHT = 20;

	// Button position (calculated during render)
	private int advBtnX, advBtnY;
	private boolean advBtnHovered = false;

	public CradleInfoScreen() {
		super(Component.literal("Sacred Artist Status"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Dark semi-transparent background (use transparent instead of blurred to avoid "blur once per frame" crash)
		renderTransparentBackground(graphics);

		int centerX = this.width / 2;
		int centerY = this.height / 2;
		int panelLeft = centerX - PANEL_WIDTH / 2;
		int panelTop = centerY - PANEL_HEIGHT / 2;

		int stageColor = ClientCradleData.getStageColor();

		// Draw panel background (dark semi-transparent box)
		graphics.fill(panelLeft - 2, panelTop - 2, panelLeft + PANEL_WIDTH + 2, panelTop + PANEL_HEIGHT + 2, stageColor);
		graphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, 0xDD1A1A2E);

		int y = panelTop + 10;

		// Title
		graphics.drawCenteredString(this.font, "Sacred Artist Status", centerX, y, 0xFFFFD700);
		y += 16;

		// Divider line
		graphics.fill(panelLeft + 10, y, panelLeft + PANEL_WIDTH - 10, y + 1, 0x66FFFFFF);
		y += 8;

		// Path
		graphics.drawString(this.font, "Path:", panelLeft + 10, y, 0xFFAAAAAA);
		graphics.drawString(this.font, ClientCradleData.getPathDisplayName(), panelLeft + 60, y, stageColor);
		y += 14;

		// Stage
		graphics.drawString(this.font, "Stage:", panelLeft + 10, y, 0xFFAAAAAA);
		graphics.drawString(this.font, ClientCradleData.getStageDisplayName(), panelLeft + 60, y, stageColor);
		y += 14;

		// Level
		graphics.drawString(this.font, "Level:", panelLeft + 10, y, 0xFFAAAAAA);
		graphics.drawString(this.font, String.valueOf(ClientCradleData.level), panelLeft + 60, y, 0xFFFFFFFF);
		y += 14;

		// Next breakthrough
		int nextBreakthrough = ClientCradleData.getNextBreakthroughLevel();
		graphics.drawString(this.font, "Next:", panelLeft + 10, y, 0xFFAAAAAA);
		if (nextBreakthrough > 0) {
			graphics.drawString(this.font, "Level " + nextBreakthrough, panelLeft + 60, y, 0xFFDD99FF);
		} else {
			graphics.drawString(this.font, "Max stage reached", panelLeft + 60, y, 0xFFFFD700);
		}
		y += 14;

		// Cycling status
		graphics.drawString(this.font, "Cycling:", panelLeft + 10, y, 0xFFAAAAAA);
		if (ClientCradleData.cycling) {
			graphics.drawString(this.font, "Active", panelLeft + 70, y, 0xFF55FF55);
		} else {
			graphics.drawString(this.font, "Inactive", panelLeft + 70, y, 0xFF999999);
		}
		y += 18;

		// Cycling XP bar
		int barLeft = centerX - BAR_WIDTH / 2;
		graphics.drawString(this.font, "Cycling XP:", panelLeft + 10, y, 0xFFAAAAAA);
		String xpText = ClientCradleData.cyclingXp + " / " + ClientCradleData.xpToNext;
		int xpTextWidth = this.font.width(xpText);
		graphics.drawString(this.font, xpText, panelLeft + PANEL_WIDTH - 10 - xpTextWidth, y, 0xFFFFFFFF);
		y += 12;

		// XP bar background
		graphics.fill(barLeft, y, barLeft + BAR_WIDTH, y + BAR_HEIGHT, 0xFF333333);
		// XP bar fill (white/light)
		float xpRatio = ClientCradleData.xpToNext > 0
				? (float) ClientCradleData.cyclingXp / ClientCradleData.xpToNext
				: 0f;
		int xpFillWidth = (int) (BAR_WIDTH * Math.min(1f, xpRatio));
		if (xpFillWidth > 0) {
			graphics.fill(barLeft, y, barLeft + xpFillWidth, y + BAR_HEIGHT, 0xFF55FFFF);
		}
		y += BAR_HEIGHT + 10;

		// Madra bar
		graphics.drawString(this.font, "Madra:", panelLeft + 10, y, 0xFFAAAAAA);
		String madraText = String.format("%.0f / %.0f", ClientCradleData.currentMadra, ClientCradleData.maxMadra);
		int madraTextWidth = this.font.width(madraText);
		graphics.drawString(this.font, madraText, panelLeft + PANEL_WIDTH - 10 - madraTextWidth, y, 0xFFFFFFFF);
		y += 12;

		// Madra bar background
		graphics.fill(barLeft, y, barLeft + BAR_WIDTH, y + BAR_HEIGHT, 0xFF333333);
		// Madra bar fill (colored by stage)
		float madraRatio = ClientCradleData.maxMadra > 0
				? ClientCradleData.currentMadra / ClientCradleData.maxMadra
				: 0f;
		int madraFillWidth = (int) (BAR_WIDTH * Math.min(1f, madraRatio));
		if (madraFillWidth > 0) {
			graphics.fill(barLeft, y, barLeft + madraFillWidth, y + BAR_HEIGHT, stageColor);
		}
		y += BAR_HEIGHT + 10;

		// ── Advance button (only shown when player can advance) ──────────
		if (ClientCradleData.canAdvance) {
			advBtnX = centerX - BUTTON_WIDTH / 2;
			advBtnY = y;

			// Check hover
			advBtnHovered = mouseX >= advBtnX && mouseX <= advBtnX + BUTTON_WIDTH
					&& mouseY >= advBtnY && mouseY <= advBtnY + BUTTON_HEIGHT;

			// Button border (gold)
			graphics.fill(advBtnX - 1, advBtnY - 1, advBtnX + BUTTON_WIDTH + 1, advBtnY + BUTTON_HEIGHT + 1, 0xFFFFD700);

			// Button background (brighter on hover)
			int btnBg = advBtnHovered ? 0xFF3A2A1E : 0xFF2A1A0E;
			graphics.fill(advBtnX, advBtnY, advBtnX + BUTTON_WIDTH, advBtnY + BUTTON_HEIGHT, btnBg);

			// Button text
			int textColor = advBtnHovered ? 0xFFFFFF55 : 0xFFFFD700;
			graphics.drawCenteredString(this.font, "\u2B06 Advance \u2B06", centerX, advBtnY + 6, textColor);
		} else {
			advBtnHovered = false;
		}

		// Hint at the bottom
		graphics.drawCenteredString(this.font, "Press ESC to close | J to toggle",
				centerX, panelTop + PANEL_HEIGHT - 12, 0x66FFFFFF);
	}

	// ── Click handling ────────────────────────────────────────────────

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		if (event.button() == 0 && ClientCradleData.canAdvance && advBtnHovered) {
			// Send advancement request to server
			ClientPlayNetworking.send(new AttemptAdvancePayload());
			// Close the screen so the player sees the breakthrough messages in chat
			this.onClose();
			return true;
		}
		return super.mouseClicked(event, bl);
	}
}

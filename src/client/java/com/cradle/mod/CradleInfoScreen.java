package com.cradle.mod;

import com.cradle.mod.network.AttemptAdvancePayload;
import com.cradle.mod.network.ChooseSageHeraldPayload;
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

	// Panel dimensions (base, before scaling)
	private static final int BASE_PANEL_WIDTH = 220;
	private static final int BASE_PANEL_HEIGHT = 302;

	// Bar dimensions (for XP and Madra bars inside the panel)
	private static final int BAR_WIDTH = 180;
	private static final int BAR_HEIGHT = 8;

	// Advance button dimensions
	private static final int BUTTON_WIDTH = 120;
	private static final int BUTTON_HEIGHT = 20;

	// Button position (calculated during render)
	private int advBtnX, advBtnY;
	private boolean advBtnHovered = false;

	// Sage/Herald choice button positions
	private int sageBtnX, sageBtnY;
	private int heraldBtnX, heraldBtnY;
	private boolean sageBtnHovered = false;
	private boolean heraldBtnHovered = false;
	private boolean showingChoice = false;

	// Skill Tree button position
	private int skillTreeBtnX, skillTreeBtnY;
	private boolean skillTreeBtnHovered = false;

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

		// Scale panel to fit screen with some margin
		int panelWidth = BASE_PANEL_WIDTH;
		int panelHeight = BASE_PANEL_HEIGHT;
		if (panelWidth > this.width - 10) {
			panelWidth = this.width - 10;
		}
		if (panelHeight > this.height - 10) {
			panelHeight = this.height - 10;
		}

		int panelLeft = centerX - panelWidth / 2;
		int panelTop = centerY - panelHeight / 2;

		int stageColor = ClientCradleData.getStageColor();

		// Bar widths scale with panel
		int barWidth = panelWidth - 40;

		// Draw panel background (dark semi-transparent box)
		graphics.fill(panelLeft - 2, panelTop - 2, panelLeft + panelWidth + 2, panelTop + panelHeight + 2, stageColor);
		graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xDD1A1A2E);

		// Compute line spacing — shrink if panel is tight
		int lineH = panelHeight >= BASE_PANEL_HEIGHT ? 14 : Math.max(10, (panelHeight - 90) / 16);
		int barH = panelHeight >= BASE_PANEL_HEIGHT ? BAR_HEIGHT : Math.max(4, BAR_HEIGHT - 2);

		int y = panelTop + 6;

		// Title
		graphics.drawCenteredString(this.font, "Sacred Artist Status", centerX, y, 0xFFFFD700);
		y += lineH + 2;

		// Divider line
		graphics.fill(panelLeft + 10, y, panelLeft + panelWidth - 10, y + 1, 0x66FFFFFF);
		y += lineH / 2;

		// Value column offset (scales with panel width)
		int valCol = panelLeft + Math.min(60, panelWidth / 4);
		int valColWide = panelLeft + Math.min(80, panelWidth / 3);

		// Path
		graphics.drawString(this.font, "Path:", panelLeft + 10, y, 0xFFAAAAAA);
		graphics.drawString(this.font, ClientCradleData.getPathDisplayName(), valCol, y, stageColor);
		y += lineH;

		// Stage
		graphics.drawString(this.font, "Stage:", panelLeft + 10, y, 0xFFAAAAAA);
		graphics.drawString(this.font, ClientCradleData.getStageDisplayName(), valCol, y, stageColor);
		y += lineH;

		// Level
		graphics.drawString(this.font, "Level:", panelLeft + 10, y, 0xFFAAAAAA);
		graphics.drawString(this.font, String.valueOf(ClientCradleData.level), valCol, y, 0xFFFFFFFF);
		y += lineH;

		// Next breakthrough
		int nextBreakthrough = ClientCradleData.getNextBreakthroughLevel();
		graphics.drawString(this.font, "Next:", panelLeft + 10, y, 0xFFAAAAAA);
		if (nextBreakthrough == -2) {
			graphics.drawString(this.font, "Choose: Sage or Herald", valCol, y, 0xFFDD99FF);
		} else if (nextBreakthrough > 0) {
			graphics.drawString(this.font, "Level " + nextBreakthrough, valCol, y, 0xFFDD99FF);
		} else {
			graphics.drawString(this.font, "Max stage reached", valCol, y, 0xFFFFD700);
		}
		y += lineH;

		// Cycling status
		graphics.drawString(this.font, "Cycling:", panelLeft + 10, y, 0xFFAAAAAA);
		if (ClientCradleData.cycling) {
			graphics.drawString(this.font, "Active", valCol + 10, y, 0xFF55FF55);
		} else {
			graphics.drawString(this.font, "Inactive", valCol + 10, y, 0xFF999999);
		}
		y += lineH;

		// Iron Body
		graphics.drawString(this.font, "Iron Body:", panelLeft + 10, y, 0xFFAAAAAA);
		if (!"NONE".equals(ClientCradleData.ironBody)) {
			String bodyText = ClientCradleData.getIronBodyDisplayName();
			if (ClientCradleData.ironBodyActive) {
				bodyText += " (ON)";
			}
			graphics.drawString(this.font, bodyText, valColWide, y, ClientCradleData.getIronBodyColor());
		} else {
			graphics.drawString(this.font, "None", valColWide, y, 0xFF999999);
		}
		y += lineH;

		// Willpower (Archlord+ only)
		if (ClientCradleData.hasWillpower()) {
			graphics.drawString(this.font, "Willpower:", panelLeft + 10, y, 0xFFAAAAAA);
			String wpText = String.format("%.0f / %.0f", ClientCradleData.currentWillpower, ClientCradleData.maxWillpower);
			graphics.drawString(this.font, wpText, valColWide, y, 0xFF6699FF);
			y += lineH;
		}

		// Icon (Sage+ only)
		if (ClientCradleData.hasIcon()) {
			graphics.drawString(this.font, "Icon:", panelLeft + 10, y, 0xFFAAAAAA);
			graphics.drawString(this.font, ClientCradleData.getIconDisplayName(),
					valColWide, y, ClientCradleData.getIconColor());
			y += lineH;
		}

		// Enforcer technique
		graphics.drawString(this.font, "Enforcer:", panelLeft + 10, y, 0xFFAAAAAA);
		if (!"UNSET".equals(ClientCradleData.path)) {
			String enforcerName = ClientCradleData.getEnforcerTechniqueName();
			if (ClientCradleData.enforcerActive) {
				graphics.drawString(this.font, enforcerName + " (ON)", valCol + 15, y, 0xFF55FF55);
			} else {
				graphics.drawString(this.font, enforcerName, valCol + 15, y, 0xFF999999);
			}
		} else {
			graphics.drawString(this.font, "None", valCol + 15, y, 0xFF999999);
		}
		y += lineH;

		// Ruler technique
		graphics.drawString(this.font, "Ruler:", panelLeft + 10, y, 0xFFAAAAAA);
		if (!"UNSET".equals(ClientCradleData.path)) {
			String rulerName = ClientCradleData.getRulerTechniqueName();
			if (ClientCradleData.rulerActive) {
				graphics.drawString(this.font, rulerName + " (ON)", valCol + 15, y, 0xFF55FF55);
			} else {
				graphics.drawString(this.font, rulerName, valCol + 15, y, 0xFF999999);
			}
		} else {
			graphics.drawString(this.font, "None", valCol + 15, y, 0xFF999999);
		}
		y += lineH + 4;

		// Cycling XP bar
		int barLeft = centerX - barWidth / 2;
		graphics.drawString(this.font, "Cycling XP:", panelLeft + 10, y, 0xFFAAAAAA);
		String xpText = ClientCradleData.cyclingXp + " / " + ClientCradleData.xpToNext;
		int xpTextWidth = this.font.width(xpText);
		graphics.drawString(this.font, xpText, panelLeft + panelWidth - 10 - xpTextWidth, y, 0xFFFFFFFF);
		y += lineH - 2;

		// XP bar background
		graphics.fill(barLeft, y, barLeft + barWidth, y + barH, 0xFF333333);
		// XP bar fill (white/light)
		float xpRatio = ClientCradleData.xpToNext > 0
				? (float) ClientCradleData.cyclingXp / ClientCradleData.xpToNext
				: 0f;
		int xpFillWidth = (int) (barWidth * Math.min(1f, xpRatio));
		if (xpFillWidth > 0) {
			graphics.fill(barLeft, y, barLeft + xpFillWidth, y + barH, 0xFF55FFFF);
		}
		y += barH + lineH / 2 + 2;

		// Madra bar
		graphics.drawString(this.font, "Madra:", panelLeft + 10, y, 0xFFAAAAAA);
		String madraText = String.format("%.0f / %.0f", ClientCradleData.currentMadra, ClientCradleData.maxMadra);
		int madraTextWidth = this.font.width(madraText);
		graphics.drawString(this.font, madraText, panelLeft + panelWidth - 10 - madraTextWidth, y, 0xFFFFFFFF);
		y += lineH - 2;

		// Madra bar background
		graphics.fill(barLeft, y, barLeft + barWidth, y + barH, 0xFF333333);
		// Madra bar fill (colored by stage)
		float madraRatio = ClientCradleData.maxMadra > 0
				? ClientCradleData.currentMadra / ClientCradleData.maxMadra
				: 0f;
		int madraFillWidth = (int) (barWidth * Math.min(1f, madraRatio));
		if (madraFillWidth > 0) {
			graphics.fill(barLeft, y, barLeft + madraFillWidth, y + barH, stageColor);
		}
		y += barH + lineH / 2 + 2;

		// ── Sage/Herald choice or Advance button ──────────────────────
		showingChoice = false;

		// Show Sage/Herald choice when at Archlord, level met, no choice yet
		if (nextBreakthrough == -2 && ClientCradleData.level >= 350) {
			showingChoice = true;

			// "Choose your path beyond Archlord:" label
			graphics.drawCenteredString(this.font, "Choose your path beyond Archlord:",
					centerX, y, 0xFFDD99FF);
			y += 14;

			int choiceBtnWidth = 95;

			// Sage button (left)
			sageBtnX = centerX - choiceBtnWidth - 4;
			sageBtnY = y;
			sageBtnHovered = mouseX >= sageBtnX && mouseX <= sageBtnX + choiceBtnWidth
					&& mouseY >= sageBtnY && mouseY <= sageBtnY + BUTTON_HEIGHT;

			graphics.fill(sageBtnX - 1, sageBtnY - 1, sageBtnX + choiceBtnWidth + 1, sageBtnY + BUTTON_HEIGHT + 1, 0xFF00B3B3);
			int sageBg = sageBtnHovered ? 0xFF1A3A3A : 0xFF0A2A2A;
			graphics.fill(sageBtnX, sageBtnY, sageBtnX + choiceBtnWidth, sageBtnY + BUTTON_HEIGHT, sageBg);
			int sageText = sageBtnHovered ? 0xFF55FFFF : 0xFF00B3B3;
			graphics.drawCenteredString(this.font, "\u2728 Become Sage", sageBtnX + choiceBtnWidth / 2, sageBtnY + 6, sageText);

			// Herald button (right)
			heraldBtnX = centerX + 4;
			heraldBtnY = y;
			heraldBtnHovered = mouseX >= heraldBtnX && mouseX <= heraldBtnX + choiceBtnWidth
					&& mouseY >= heraldBtnY && mouseY <= heraldBtnY + BUTTON_HEIGHT;

			graphics.fill(heraldBtnX - 1, heraldBtnY - 1, heraldBtnX + choiceBtnWidth + 1, heraldBtnY + BUTTON_HEIGHT + 1, 0xFFCC1166);
			int heraldBg = heraldBtnHovered ? 0xFF3A1A2A : 0xFF2A0A1A;
			graphics.fill(heraldBtnX, heraldBtnY, heraldBtnX + choiceBtnWidth, heraldBtnY + BUTTON_HEIGHT, heraldBg);
			int heraldText = heraldBtnHovered ? 0xFFFF66AA : 0xFFCC1166;
			graphics.drawCenteredString(this.font, "\u2728 Become Herald", heraldBtnX + choiceBtnWidth / 2, heraldBtnY + 6, heraldText);

			advBtnHovered = false;
		} else if (ClientCradleData.canAdvance) {
			// Standard advance button
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
			sageBtnHovered = false;
			heraldBtnHovered = false;
		}

		// Skill Tree button (bottom of panel, before hint)
		if (ClientCradleData.hasChosenPath()) {
			int stBtnW = 100;
			int stBtnH = 16;
			skillTreeBtnX = centerX - stBtnW / 2;
			skillTreeBtnY = panelTop + panelHeight - 32;
			skillTreeBtnHovered = mouseX >= skillTreeBtnX && mouseX <= skillTreeBtnX + stBtnW
					&& mouseY >= skillTreeBtnY && mouseY <= skillTreeBtnY + stBtnH;
			int stColor = skillTreeBtnHovered ? 0xFF444466 : 0xFF333355;
			graphics.fill(skillTreeBtnX - 1, skillTreeBtnY - 1,
					skillTreeBtnX + stBtnW + 1, skillTreeBtnY + stBtnH + 1, 0xFF6666AA);
			graphics.fill(skillTreeBtnX, skillTreeBtnY,
					skillTreeBtnX + stBtnW, skillTreeBtnY + stBtnH, stColor);
			int stTextColor = skillTreeBtnHovered ? 0xFFAAAAFF : 0xFF8888CC;
			graphics.drawCenteredString(this.font, "\u2728 Skill Tree (K)",
					centerX, skillTreeBtnY + 4, stTextColor);
		}

		// Hint at the bottom
		graphics.drawCenteredString(this.font, "Press ESC to close | J to toggle",
				centerX, panelTop + panelHeight - 12, 0x66FFFFFF);
	}

	// ── Click handling ────────────────────────────────────────────────

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		if (event.button() == 0) {
			// Skill Tree button
			if (skillTreeBtnHovered && ClientCradleData.hasChosenPath()) {
				this.minecraft.setScreen(new SkillTreeScreen());
				return true;
			}
			// Sage/Herald choice buttons
			if (showingChoice && sageBtnHovered) {
				ClientPlayNetworking.send(new ChooseSageHeraldPayload("SAGE"));
				this.onClose();
				return true;
			}
			if (showingChoice && heraldBtnHovered) {
				ClientPlayNetworking.send(new ChooseSageHeraldPayload("HERALD"));
				this.onClose();
				return true;
			}
			// Standard advance button
			if (ClientCradleData.canAdvance && advBtnHovered) {
				ClientPlayNetworking.send(new AttemptAdvancePayload());
				this.onClose();
				return true;
			}
		}
		return super.mouseClicked(event, bl);
	}
}

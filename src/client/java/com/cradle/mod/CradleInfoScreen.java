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
 * Now supports vertical scrolling when the content exceeds the panel height.
 * Panel automatically fits within the screen at any GUI scale.
 *
 * When the player meets all requirements to advance to the next stage,
 * an "Advance" button appears at the bottom. Clicking it sends a
 * request to the server to perform the breakthrough.
 */
public class CradleInfoScreen extends Screen {

	// Panel dimensions (base, before scaling)
	private static final int BASE_PANEL_WIDTH = 220;

	// Bar dimensions (for XP and Madra bars inside the panel)
	private static final int BAR_HEIGHT = 8;

	// Advance button dimensions
	private static final int BUTTON_WIDTH = 120;
	private static final int BUTTON_HEIGHT = 20;

	// Scrolling
	private static final int SCROLL_SPEED = 10;
	private float scrollOffset = 0;
	private int contentHeight = 0; // total rendered content height
	private int viewportHeight = 0; // visible area height inside panel

	// Fixed areas (title at top, hint at bottom) not scrolled
	private static final int HEADER_HEIGHT = 24; // title + divider
	private static final int FOOTER_HEIGHT = 18; // hint line

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

	// Cached panel bounds for scroll clipping and mouse checks
	private int panelLeft, panelTop, panelWidth, panelHeight;

	public CradleInfoScreen() {
		super(Component.literal("Sacred Artist Status"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Dark semi-transparent background
		renderTransparentBackground(graphics);

		int centerX = this.width / 2;
		int centerY = this.height / 2;

		// Panel fills available screen with margin
		panelWidth = Math.min(BASE_PANEL_WIDTH, this.width - 10);
		panelHeight = this.height - 10;

		panelLeft = centerX - panelWidth / 2;
		panelTop = centerY - panelHeight / 2;

		int stageColor = ClientCradleData.getStageColor();

		// Bar widths scale with panel
		int barWidth = panelWidth - 40;
		int barH = BAR_HEIGHT;
		int lineH = 14;

		// ── Draw panel background ──
		graphics.fill(panelLeft - 2, panelTop - 2, panelLeft + panelWidth + 2, panelTop + panelHeight + 2, stageColor);
		graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xDD1A1A2E);

		// ── Header (not scrolled) ──
		int y = panelTop + 6;
		graphics.drawCenteredString(this.font, "Sacred Artist Status", centerX, y, 0xFFFFD700);
		y += lineH + 2;
		graphics.fill(panelLeft + 10, y, panelLeft + panelWidth - 10, y + 1, 0x66FFFFFF);

		// ── Footer (not scrolled) ──
		graphics.drawCenteredString(this.font, "Press ESC to close | J to toggle",
				centerX, panelTop + panelHeight - 12, 0x66FFFFFF);

		// ── Scrollable content area ──
		int scrollTop = panelTop + HEADER_HEIGHT;
		int scrollBottom = panelTop + panelHeight - FOOTER_HEIGHT;
		viewportHeight = scrollBottom - scrollTop;

		// Enable scissor clipping for the scrollable area
		graphics.enableScissor(panelLeft, scrollTop, panelLeft + panelWidth, scrollBottom);

		// Apply scroll offset
		int scrollY = scrollTop + 4 - (int) scrollOffset;

		// Value column offsets
		int valCol = panelLeft + Math.min(60, panelWidth / 4);
		int valColWide = panelLeft + Math.min(80, panelWidth / 3);

		// Path
		graphics.drawString(this.font, "Path:", panelLeft + 10, scrollY, 0xFFAAAAAA);
		graphics.drawString(this.font, ClientCradleData.getPathDisplayName(), valCol, scrollY, stageColor);
		scrollY += lineH;

		// Stage
		graphics.drawString(this.font, "Stage:", panelLeft + 10, scrollY, 0xFFAAAAAA);
		graphics.drawString(this.font, ClientCradleData.getStageDisplayName(), valCol, scrollY, stageColor);
		scrollY += lineH;

		// Level
		graphics.drawString(this.font, "Level:", panelLeft + 10, scrollY, 0xFFAAAAAA);
		graphics.drawString(this.font, String.valueOf(ClientCradleData.level), valCol, scrollY, 0xFFFFFFFF);
		scrollY += lineH;

		// Next breakthrough
		int nextBreakthrough = ClientCradleData.getNextBreakthroughLevel();
		graphics.drawString(this.font, "Next:", panelLeft + 10, scrollY, 0xFFAAAAAA);
		if (nextBreakthrough == -2) {
			graphics.drawString(this.font, "Choose: Sage or Herald", valCol, scrollY, 0xFFDD99FF);
		} else if (nextBreakthrough > 0) {
			graphics.drawString(this.font, "Level " + nextBreakthrough, valCol, scrollY, 0xFFDD99FF);
		} else {
			graphics.drawString(this.font, "Max stage reached", valCol, scrollY, 0xFFFFD700);
		}
		scrollY += lineH;

		// Cycling status
		graphics.drawString(this.font, "Cycling:", panelLeft + 10, scrollY, 0xFFAAAAAA);
		if (ClientCradleData.cycling) {
			graphics.drawString(this.font, "Active", valCol + 10, scrollY, 0xFF55FF55);
		} else {
			graphics.drawString(this.font, "Inactive", valCol + 10, scrollY, 0xFF999999);
		}
		scrollY += lineH;

		// Vital Aura (Copper+ only, when Copper Sight is active)
		if (ClientCradleData.isCopper()) {
			graphics.drawString(this.font, "Aura:", panelLeft + 10, scrollY, 0xFFAAAAAA);
			if (ClientCradleData.copperSightActive) {
				String auraName = AuraParticleRenderer.getCachedAuraName();
				int auraColor = AuraParticleRenderer.getCachedAuraColor();
				String biomeName = AuraParticleRenderer.getCachedBiomeName();
				String auraDisplay = auraName + " (" + biomeName + ")";
				graphics.drawString(this.font, auraDisplay, valCol, scrollY, auraColor);
			} else {
				graphics.drawString(this.font, "Sight off (H)", valCol, scrollY, 0xFF666666);
			}
			scrollY += lineH;
		}

		// Iron Body
		graphics.drawString(this.font, "Iron Body:", panelLeft + 10, scrollY, 0xFFAAAAAA);
		if (!"NONE".equals(ClientCradleData.ironBody)) {
			String bodyText = ClientCradleData.getIronBodyDisplayName();
			if (ClientCradleData.ironBodyActive) {
				bodyText += " (ON)";
			}
			graphics.drawString(this.font, bodyText, valColWide, scrollY, ClientCradleData.getIronBodyColor());
		} else {
			graphics.drawString(this.font, "None", valColWide, scrollY, 0xFF999999);
		}
		scrollY += lineH;

		// Goldsign (only show if player has one or is Gold+)
		if (ClientCradleData.hasGoldsign()) {
			graphics.drawString(this.font, "Goldsign:", panelLeft + 10, scrollY, 0xFFAAAAAA);
			graphics.drawString(this.font, ClientCradleData.getGoldsignDisplayName(),
					valColWide, scrollY, ClientCradleData.getGoldsignColor());
			scrollY += lineH;
		}

		// Willpower (Archlord+ only)
		if (ClientCradleData.hasWillpower()) {
			graphics.drawString(this.font, "Willpower:", panelLeft + 10, scrollY, 0xFFAAAAAA);
			String wpText = String.format("%.0f / %.0f", ClientCradleData.currentWillpower, ClientCradleData.maxWillpower);
			graphics.drawString(this.font, wpText, valColWide, scrollY, 0xFF6699FF);
			scrollY += lineH;
		}

		// Icon (Sage+ only)
		if (ClientCradleData.hasIcon()) {
			graphics.drawString(this.font, "Icon:", panelLeft + 10, scrollY, 0xFFAAAAAA);
			graphics.drawString(this.font, ClientCradleData.getIconDisplayName(),
					valColWide, scrollY, ClientCradleData.getIconColor());
			scrollY += lineH;
		}

		// Enforcer technique
		graphics.drawString(this.font, "Enforcer:", panelLeft + 10, scrollY, 0xFFAAAAAA);
		if (!"UNSET".equals(ClientCradleData.path)) {
			String enforcerName = ClientCradleData.getEnforcerTechniqueName();
			if (ClientCradleData.enforcerActive) {
				graphics.drawString(this.font, enforcerName + " (ON)", valCol + 15, scrollY, 0xFF55FF55);
			} else {
				graphics.drawString(this.font, enforcerName, valCol + 15, scrollY, 0xFF999999);
			}
		} else {
			graphics.drawString(this.font, "None", valCol + 15, scrollY, 0xFF999999);
		}
		scrollY += lineH;

		// Ruler technique
		graphics.drawString(this.font, "Ruler:", panelLeft + 10, scrollY, 0xFFAAAAAA);
		if (!"UNSET".equals(ClientCradleData.path)) {
			String rulerName = ClientCradleData.getRulerTechniqueName();
			if (ClientCradleData.rulerActive) {
				graphics.drawString(this.font, rulerName + " (ON)", valCol + 15, scrollY, 0xFF55FF55);
			} else {
				graphics.drawString(this.font, rulerName, valCol + 15, scrollY, 0xFF999999);
			}
		} else {
			graphics.drawString(this.font, "None", valCol + 15, scrollY, 0xFF999999);
		}
		scrollY += lineH + 4;

		// Cycling XP bar
		int barLeft = centerX - barWidth / 2;
		graphics.drawString(this.font, "Cycling XP:", panelLeft + 10, scrollY, 0xFFAAAAAA);
		String xpText = ClientCradleData.cyclingXp + " / " + ClientCradleData.xpToNext;
		int xpTextWidth = this.font.width(xpText);
		graphics.drawString(this.font, xpText, panelLeft + panelWidth - 10 - xpTextWidth, scrollY, 0xFFFFFFFF);
		scrollY += lineH - 2;

		// XP bar background
		graphics.fill(barLeft, scrollY, barLeft + barWidth, scrollY + barH, 0xFF333333);
		// XP bar fill
		float xpRatio = ClientCradleData.xpToNext > 0
				? (float) ClientCradleData.cyclingXp / ClientCradleData.xpToNext
				: 0f;
		int xpFillWidth = (int) (barWidth * Math.min(1f, xpRatio));
		if (xpFillWidth > 0) {
			graphics.fill(barLeft, scrollY, barLeft + xpFillWidth, scrollY + barH, 0xFF55FFFF);
		}
		scrollY += barH + lineH / 2 + 2;

		// Madra bar
		graphics.drawString(this.font, "Madra:", panelLeft + 10, scrollY, 0xFFAAAAAA);
		String madraText = String.format("%.0f / %.0f", ClientCradleData.currentMadra, ClientCradleData.maxMadra);
		int madraTextWidth = this.font.width(madraText);
		graphics.drawString(this.font, madraText, panelLeft + panelWidth - 10 - madraTextWidth, scrollY, 0xFFFFFFFF);
		scrollY += lineH - 2;

		// Madra bar background
		graphics.fill(barLeft, scrollY, barLeft + barWidth, scrollY + barH, 0xFF333333);
		// Madra bar fill (colored by stage)
		float madraRatio = ClientCradleData.maxMadra > 0
				? ClientCradleData.currentMadra / ClientCradleData.maxMadra
				: 0f;
		int madraFillWidth = (int) (barWidth * Math.min(1f, madraRatio));
		if (madraFillWidth > 0) {
			graphics.fill(barLeft, scrollY, barLeft + madraFillWidth, scrollY + barH, stageColor);
		}
		scrollY += barH + lineH / 2 + 2;

		// ── Sage/Herald choice or Advance button ──
		showingChoice = false;

		if (nextBreakthrough == -2 && ClientCradleData.level >= 350) {
			showingChoice = true;

			graphics.drawCenteredString(this.font, "Choose your path beyond Archlord:",
					centerX, scrollY, 0xFFDD99FF);
			scrollY += 14;

			int choiceBtnWidth = 95;

			// Sage button (left)
			sageBtnX = centerX - choiceBtnWidth - 4;
			sageBtnY = scrollY;
			sageBtnHovered = isInScrollArea(mouseX, mouseY)
					&& mouseX >= sageBtnX && mouseX <= sageBtnX + choiceBtnWidth
					&& mouseY >= sageBtnY && mouseY <= sageBtnY + BUTTON_HEIGHT;

			graphics.fill(sageBtnX - 1, sageBtnY - 1, sageBtnX + choiceBtnWidth + 1, sageBtnY + BUTTON_HEIGHT + 1, 0xFF00B3B3);
			int sageBg = sageBtnHovered ? 0xFF1A3A3A : 0xFF0A2A2A;
			graphics.fill(sageBtnX, sageBtnY, sageBtnX + choiceBtnWidth, sageBtnY + BUTTON_HEIGHT, sageBg);
			int sageText = sageBtnHovered ? 0xFF55FFFF : 0xFF00B3B3;
			graphics.drawCenteredString(this.font, "\u2728 Become Sage", sageBtnX + choiceBtnWidth / 2, sageBtnY + 6, sageText);

			// Herald button (right)
			heraldBtnX = centerX + 4;
			heraldBtnY = scrollY;
			heraldBtnHovered = isInScrollArea(mouseX, mouseY)
					&& mouseX >= heraldBtnX && mouseX <= heraldBtnX + choiceBtnWidth
					&& mouseY >= heraldBtnY && mouseY <= heraldBtnY + BUTTON_HEIGHT;

			graphics.fill(heraldBtnX - 1, heraldBtnY - 1, heraldBtnX + choiceBtnWidth + 1, heraldBtnY + BUTTON_HEIGHT + 1, 0xFFCC1166);
			int heraldBg = heraldBtnHovered ? 0xFF3A1A2A : 0xFF2A0A1A;
			graphics.fill(heraldBtnX, heraldBtnY, heraldBtnX + choiceBtnWidth, heraldBtnY + BUTTON_HEIGHT, heraldBg);
			int heraldText = heraldBtnHovered ? 0xFFFF66AA : 0xFFCC1166;
			graphics.drawCenteredString(this.font, "\u2728 Become Herald", heraldBtnX + choiceBtnWidth / 2, heraldBtnY + 6, heraldText);

			scrollY += BUTTON_HEIGHT + 4;
			advBtnHovered = false;
		} else if (ClientCradleData.canAdvance) {
			advBtnX = centerX - BUTTON_WIDTH / 2;
			advBtnY = scrollY;

			advBtnHovered = isInScrollArea(mouseX, mouseY)
					&& mouseX >= advBtnX && mouseX <= advBtnX + BUTTON_WIDTH
					&& mouseY >= advBtnY && mouseY <= advBtnY + BUTTON_HEIGHT;

			graphics.fill(advBtnX - 1, advBtnY - 1, advBtnX + BUTTON_WIDTH + 1, advBtnY + BUTTON_HEIGHT + 1, 0xFFFFD700);
			int btnBg = advBtnHovered ? 0xFF3A2A1E : 0xFF2A1A0E;
			graphics.fill(advBtnX, advBtnY, advBtnX + BUTTON_WIDTH, advBtnY + BUTTON_HEIGHT, btnBg);
			int textColor = advBtnHovered ? 0xFFFFFF55 : 0xFFFFD700;
			graphics.drawCenteredString(this.font, "\u2B06 Advance \u2B06", centerX, advBtnY + 6, textColor);

			scrollY += BUTTON_HEIGHT + 4;
		} else {
			advBtnHovered = false;
			sageBtnHovered = false;
			heraldBtnHovered = false;
		}

		// Skill Tree button
		if (ClientCradleData.hasChosenPath()) {
			int stBtnW = 100;
			int stBtnH = 16;
			skillTreeBtnX = centerX - stBtnW / 2;
			skillTreeBtnY = scrollY;
			skillTreeBtnHovered = isInScrollArea(mouseX, mouseY)
					&& mouseX >= skillTreeBtnX && mouseX <= skillTreeBtnX + stBtnW
					&& mouseY >= skillTreeBtnY && mouseY <= skillTreeBtnY + stBtnH;
			int stColor = skillTreeBtnHovered ? 0xFF444466 : 0xFF333355;
			graphics.fill(skillTreeBtnX - 1, skillTreeBtnY - 1,
					skillTreeBtnX + stBtnW + 1, skillTreeBtnY + stBtnH + 1, 0xFF6666AA);
			graphics.fill(skillTreeBtnX, skillTreeBtnY,
					skillTreeBtnX + stBtnW, skillTreeBtnY + stBtnH, stColor);
			int stTextColor = skillTreeBtnHovered ? 0xFFAAAAFF : 0xFF8888CC;
			graphics.drawCenteredString(this.font, "\u2728 Skill Tree (K)",
					centerX, skillTreeBtnY + 4, stTextColor);
			scrollY += stBtnH + 8;
		}

		// Calculate total content height (from top of scroll area to here)
		contentHeight = scrollY - (scrollTop + 4 - (int) scrollOffset);

		// Disable scissor
		graphics.disableScissor();

		// Draw scroll indicators if content overflows
		int maxScroll = Math.max(0, contentHeight - viewportHeight);
		if (maxScroll > 0) {
			// Scroll bar track
			int trackX = panelLeft + panelWidth - 5;
			int trackTop = scrollTop + 2;
			int trackHeight = viewportHeight - 4;
			graphics.fill(trackX, trackTop, trackX + 3, trackTop + trackHeight, 0x33FFFFFF);

			// Scroll bar thumb
			float scrollRatio = scrollOffset / maxScroll;
			int thumbHeight = Math.max(10, (int) ((float) viewportHeight / contentHeight * trackHeight));
			int thumbY = trackTop + (int) (scrollRatio * (trackHeight - thumbHeight));
			graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xAAFFFFFF);
		}

		// Clamp scroll offset
		scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
	}

	// ── Scroll handling ──────────────────────────────────────────────

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int maxScroll = Math.max(0, contentHeight - viewportHeight);
		scrollOffset -= (float) (scrollY * SCROLL_SPEED);
		scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
		return true;
	}

	/** Check if mouse is within the scrollable viewport area. */
	private boolean isInScrollArea(int mouseX, int mouseY) {
		int scrollTop = panelTop + HEADER_HEIGHT;
		int scrollBottom = panelTop + panelHeight - FOOTER_HEIGHT;
		return mouseX >= panelLeft && mouseX <= panelLeft + panelWidth
				&& mouseY >= scrollTop && mouseY <= scrollBottom;
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

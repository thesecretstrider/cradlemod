package com.cradle.mod;

import com.cradle.mod.network.ChooseIconPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Full-screen Icon selection GUI.
 * Shown when a player is about to become a Sage (or Monarch from Herald).
 * Cannot be closed with Escape — must pick an Icon.
 */
public class IconSelectionScreen extends Screen {

	// ── Layout constants ──────────────────────────────────────────────

	private static final int CARD_WIDTH = 150;
	private static final int CARD_HEIGHT = 80;
	private static final int GAP = 8;

	// ── State ─────────────────────────────────────────────────────────

	private final List<CradlePlayerData.Icon> availableIcons;
	private final boolean forMonarch;
	private int hoveredIndex = -1;

	// Card positions, calculated on render
	private int[] cardX;
	private int[] cardY;

	public IconSelectionScreen(String pathName, boolean forMonarch) {
		super(Component.literal("Manifest Your Icon"));
		this.forMonarch = forMonarch;

		// Determine available icons based on path
		CradlePlayerData.Path path;
		try {
			path = CradlePlayerData.Path.valueOf(pathName);
		} catch (IllegalArgumentException e) {
			path = CradlePlayerData.Path.UNSET;
		}
		this.availableIcons = CradlePlayerData.getAvailableIcons(path);
		this.cardX = new int[availableIcons.size()];
		this.cardY = new int[availableIcons.size()];
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false; // Cannot close until an Icon is chosen
	}

	// ── Rendering ─────────────────────────────────────────────────────

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Dark semi-transparent background
		renderTransparentBackground(graphics);

		int centerX = this.width / 2;
		int centerY = this.height / 2;

		int count = availableIcons.size();
		if (count == 0) {
			graphics.drawCenteredString(this.font, "No Icons available", centerX, centerY, 0xFFFF4444);
			return;
		}

		// Calculate layout — single row centered
		int totalWidth = count * CARD_WIDTH + (count - 1) * GAP;
		int startX = centerX - totalWidth / 2;

		int titleHeight = 36; // title + subtitle + spacing
		int startY = centerY - (titleHeight + CARD_HEIGHT) / 2;

		// Title
		String titleText = forMonarch
				? "Manifest Your Icon to Ascend"
				: "Manifest Your Icon";
		graphics.drawCenteredString(this.font, titleText, centerX, startY, 0xFF00CCCC);
		graphics.drawCenteredString(this.font,
				"The Way watches. Choose the concept that defines your soul.",
				centerX, startY + 14, 0xFFAAAAAA);

		int cardRowY = startY + titleHeight;

		// ── Calculate card positions ──────────────────────────────────

		for (int i = 0; i < count; i++) {
			cardX[i] = startX + i * (CARD_WIDTH + GAP);
			cardY[i] = cardRowY;
		}

		// ── Detect hover ──────────────────────────────────────────────

		hoveredIndex = -1;
		for (int i = 0; i < count; i++) {
			if (mouseX >= cardX[i] && mouseX <= cardX[i] + CARD_WIDTH
					&& mouseY >= cardY[i] && mouseY <= cardY[i] + CARD_HEIGHT) {
				hoveredIndex = i;
				break;
			}
		}

		// ── Draw cards ────────────────────────────────────────────────

		for (int i = 0; i < count; i++) {
			drawIconCard(graphics, i, i == hoveredIndex);
		}

		// ── Hint at bottom ────────────────────────────────────────────

		graphics.drawCenteredString(this.font, "Click an Icon to manifest it",
				centerX, this.height - 14, 0x66FFFFFF);
	}

	private void drawIconCard(GuiGraphics graphics, int index, boolean hovered) {
		int x = cardX[index];
		int y = cardY[index];
		CradlePlayerData.Icon icon = availableIcons.get(index);
		int color = icon.color();

		// Border color — brighter on hover
		int borderColor = hovered ? brighten(color) : color;
		// Background — slightly lighter on hover
		int bgColor = hovered ? 0xDD2A2A3E : 0xDD1A1A2E;

		// Draw border (2px)
		graphics.fill(x - 2, y - 2, x + CARD_WIDTH + 2, y + CARD_HEIGHT + 2, borderColor);
		// Draw background
		graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, bgColor);

		// Icon name (centered, colored)
		int textCenterX = x + CARD_WIDTH / 2;
		String displayName = icon.displayName() + " Icon";
		graphics.drawCenteredString(this.font, displayName, textCenterX, y + 6, color);

		// Divider line
		int divY = y + 20;
		graphics.fill(x + 10, divY, x + CARD_WIDTH - 10, divY + 1, 0x44FFFFFF);

		// Lore description — word-wrapped, clipped to card bounds
		int descY = divY + 4;
		int maxDescWidth = CARD_WIDTH - 16;
		int cardBottom = y + CARD_HEIGHT - 4;
		Component descText = Component.literal(icon.loreDescription());
		List<FormattedCharSequence> lines = this.font.split(descText, maxDescWidth);

		for (FormattedCharSequence line : lines) {
			if (descY + this.font.lineHeight > cardBottom) {
				break; // Don't draw lines that would overflow the card
			}
			int lineWidth = this.font.width(line);
			float lineX = x + (CARD_WIDTH - lineWidth) / 2.0f;
			graphics.drawString(this.font, line, (int) lineX, descY, 0xFFBBBBBB);
			descY += this.font.lineHeight + 1;
		}
	}

	/**
	 * Brightens a color for the hover effect.
	 */
	private static int brighten(int argb) {
		int a = (argb >> 24) & 0xFF;
		int r = Math.min(255, ((argb >> 16) & 0xFF) + 60);
		int g = Math.min(255, ((argb >> 8) & 0xFF) + 60);
		int b = Math.min(255, (argb & 0xFF) + 60);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// ── Click handling ────────────────────────────────────────────────

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		if (event.button() == 0 && hoveredIndex >= 0 && hoveredIndex < availableIcons.size()) {
			CradlePlayerData.Icon chosen = availableIcons.get(hoveredIndex);
			// Send the chosen icon to the server
			ClientPlayNetworking.send(new ChooseIconPayload(chosen.name()));
			// Close the screen
			this.onClose();
			return true;
		}
		return super.mouseClicked(event, bl);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(null);
	}
}

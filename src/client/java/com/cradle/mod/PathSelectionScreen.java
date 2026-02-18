package com.cradle.mod;

import com.cradle.mod.network.ChoosePathPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Full-screen path selection GUI.
 * Shown on first join when the player hasn't chosen a Path yet.
 * Cannot be closed with Escape — must pick a path.
 */
public class PathSelectionScreen extends Screen {

	// ── Path card data ────────────────────────────────────────────────

	private static final String[] PATH_NAMES = {
			"BLACK_FLAME",
			"ENDLESS_SWORD",
			"STELLAR_SPEAR",
			"CLOUD_HAMMER",
			"HOLLOW_KING"
	};

	private static final String[] DISPLAY_NAMES = {
			"Path of Black Flame",
			"Path of the Endless Sword",
			"Path of the Stellar Spear",
			"Path of the Cloud Hammer",
			"Path of the Hollow King"
	};

	private static final String[] DESCRIPTIONS = {
			"The fire that devours all. Blackflame destroys everything in its path — including you.",
			"A thousand invisible blades answer your will. Cut through anything that stands before you.",
			"Starlight given form. Pierce the heavens and let nothing stand between you and your target.",
			"Thunder and weight made manifest. Crush your enemies with the force of a storm.",
			"Pure madra, unaspected and absolute. The power to negate, absorb, and command all aura."
	};

	private static final int[] PATH_COLORS = {
			0xFF8B0000, // Black Flame — dark red
			0xFFCCCCCC, // Endless Sword — silver
			0xFFFFDD44, // Stellar Spear — gold
			0xFF444455, // Cloud Hammer — dark grey
			0xFFDDDDEE  // Hollow King — pale white
	};

	// ── Layout constants ──────────────────────────────────────────────

	private static final int CARD_WIDTH = 140;
	private static final int CARD_HEIGHT = 64;
	private static final int GAP = 6;

	// ── State ─────────────────────────────────────────────────────────

	private int hoveredIndex = -1;

	// Card positions, calculated on render
	private final int[] cardX = new int[5];
	private final int[] cardY = new int[5];

	public PathSelectionScreen() {
		super(Component.literal("Choose Your Path"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false; // Cannot close until a path is chosen
	}

	// ── Rendering ─────────────────────────────────────────────────────

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Dark semi-transparent background (safe — no blur crash)
		renderTransparentBackground(graphics);

		int centerX = this.width / 2;
		int centerY = this.height / 2;

		// Calculate total grid height to center everything vertically
		// Title (10px) + gap (4) + subtitle (10px) + gap (8) + 2 rows of cards + gap between rows
		int gridHeight = CARD_HEIGHT + GAP + CARD_HEIGHT; // two rows
		int titleHeight = 28; // title + subtitle + spacing
		int totalHeight = titleHeight + gridHeight;
		int startY = centerY - totalHeight / 2;

		// Title
		graphics.drawCenteredString(this.font, "Choose Your Path", centerX, startY, 0xFFFFD700);
		graphics.drawCenteredString(this.font, "Select a sacred arts discipline to begin your journey",
				centerX, startY + 14, 0xFFAAAAAA);

		// ── Calculate card positions (3-2 grid) ──────────────────────

		// Top row: 3 cards centered
		int topRowWidth = 3 * CARD_WIDTH + 2 * GAP;
		int topRowLeft = centerX - topRowWidth / 2;
		int topRowY = startY + titleHeight;

		for (int i = 0; i < 3; i++) {
			cardX[i] = topRowLeft + i * (CARD_WIDTH + GAP);
			cardY[i] = topRowY;
		}

		// Bottom row: 2 cards centered
		int bottomRowWidth = 2 * CARD_WIDTH + GAP;
		int bottomRowLeft = centerX - bottomRowWidth / 2;
		int bottomRowY = topRowY + CARD_HEIGHT + GAP;

		for (int i = 0; i < 2; i++) {
			cardX[3 + i] = bottomRowLeft + i * (CARD_WIDTH + GAP);
			cardY[3 + i] = bottomRowY;
		}

		// ── Detect hover ──────────────────────────────────────────────

		hoveredIndex = -1;
		for (int i = 0; i < 5; i++) {
			if (mouseX >= cardX[i] && mouseX <= cardX[i] + CARD_WIDTH
					&& mouseY >= cardY[i] && mouseY <= cardY[i] + CARD_HEIGHT) {
				hoveredIndex = i;
				break;
			}
		}

		// ── Draw cards ────────────────────────────────────────────────

		for (int i = 0; i < 5; i++) {
			drawPathCard(graphics, i, i == hoveredIndex);
		}

		// ── Hint at bottom ────────────────────────────────────────────

		graphics.drawCenteredString(this.font, "Click a path to make your choice",
				centerX, this.height - 14, 0x66FFFFFF);
	}

	private void drawPathCard(GuiGraphics graphics, int index, boolean hovered) {
		int x = cardX[index];
		int y = cardY[index];
		int color = PATH_COLORS[index];

		// Border color — brighter on hover
		int borderColor = hovered ? brighten(color) : color;
		// Background — slightly lighter on hover
		int bgColor = hovered ? 0xDD2A2A3E : 0xDD1A1A2E;

		// Draw border (2px)
		graphics.fill(x - 2, y - 2, x + CARD_WIDTH + 2, y + CARD_HEIGHT + 2, borderColor);
		// Draw background
		graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, bgColor);

		// Path name (centered, colored)
		int textCenterX = x + CARD_WIDTH / 2;
		graphics.drawCenteredString(this.font, DISPLAY_NAMES[index], textCenterX, y + 5, color);

		// Divider line
		int divY = y + 18;
		graphics.fill(x + 10, divY, x + CARD_WIDTH - 10, divY + 1, 0x44FFFFFF);

		// Description — word-wrapped, clipped to card bounds
		int descY = divY + 4;
		int maxDescWidth = CARD_WIDTH - 16;
		int cardBottom = y + CARD_HEIGHT - 4; // leave 4px padding at bottom
		Component descText = Component.literal(DESCRIPTIONS[index]);
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
		if (event.button() == 0 && hoveredIndex >= 0 && hoveredIndex < PATH_NAMES.length) {
			// Send the chosen path to the server
			ClientPlayNetworking.send(new ChoosePathPayload(PATH_NAMES[hoveredIndex]));
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

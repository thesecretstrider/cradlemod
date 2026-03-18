package com.cradle.mod;

import com.cradle.mod.network.ChooseCharacterPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Full-screen character selection GUI for Cradle (Story) mode.
 * Shown on first join when the player hasn't chosen a character yet.
 * Cannot be closed with Escape — must pick a character.
 */
public class CharacterSelectionScreen extends Screen {

	// ── Character data ────────────────────────────────────────────────

	private static final String[] CHARACTER_NAMES = { "LINDON", "YERIN" };

	private static final String[] DISPLAY_NAMES = {
			"Wei Shi Lindon",
			"Yerin"
	};

	private static final String[] DESCRIPTIONS = {
			"An Unsouled of the Wei clan, born without the ability to use madra. "
					+ "Through sheer determination and an encounter with the Abidan, "
					+ "Lindon will forge his own path. Begins at Foundation with Pure madra.",
			"Disciple of the Sword Sage, raised outside Sacred Valley. "
					+ "A deadly swordswoman haunted by the blood shadow within her. "
					+ "Begins at Copper on the Path of the Endless Sword."
	};

	private static final int[] CHARACTER_COLORS = {
			0xFFDDDDEE, // Lindon — pale white (Pure path)
			0xFFCCCCCC  // Yerin — silver (Endless Sword)
	};

	// ── Layout constants ──────────────────────────────────────────────

	private static final int CARD_WIDTH = 200;
	private static final int CARD_HEIGHT = 100;
	private static final int GAP = 20;

	// ── State ─────────────────────────────────────────────────────────

	private int hoveredIndex = -1;
	private final int[] cardX = new int[2];
	private final int[] cardY = new int[2];

	public CharacterSelectionScreen() {
		super(Component.literal("Choose Your Character"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	// ── Rendering ─────────────────────────────────────────────────────

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderTransparentBackground(graphics);

		int centerX = this.width / 2;
		int centerY = this.height / 2;

		// Title area
		int startY = centerY - (CARD_HEIGHT / 2) - 40;
		graphics.drawCenteredString(this.font, "Choose Your Character", centerX, startY, 0xFFFFD700);
		graphics.drawCenteredString(this.font, "Experience Sacred Valley through their eyes",
				centerX, startY + 14, 0xFFAAAAAA);

		// Two cards side by side
		int totalWidth = 2 * CARD_WIDTH + GAP;
		int leftX = centerX - totalWidth / 2;
		int cardTopY = startY + 36;

		for (int i = 0; i < 2; i++) {
			cardX[i] = leftX + i * (CARD_WIDTH + GAP);
			cardY[i] = cardTopY;
		}

		// Detect hover
		hoveredIndex = -1;
		for (int i = 0; i < 2; i++) {
			if (mouseX >= cardX[i] && mouseX <= cardX[i] + CARD_WIDTH
					&& mouseY >= cardY[i] && mouseY <= cardY[i] + CARD_HEIGHT) {
				hoveredIndex = i;
				break;
			}
		}

		// Draw cards
		for (int i = 0; i < 2; i++) {
			drawCharacterCard(graphics, i, i == hoveredIndex);
		}

		// Hint
		graphics.drawCenteredString(this.font, "Click a character to begin",
				centerX, this.height - 14, 0x66FFFFFF);
	}

	private void drawCharacterCard(GuiGraphics graphics, int index, boolean hovered) {
		int x = cardX[index];
		int y = cardY[index];
		int color = CHARACTER_COLORS[index];

		int borderColor = hovered ? brighten(color) : color;
		int bgColor = hovered ? 0xDD2A2A3E : 0xDD1A1A2E;

		// Border (2px)
		graphics.fill(x - 2, y - 2, x + CARD_WIDTH + 2, y + CARD_HEIGHT + 2, borderColor);
		// Background
		graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, bgColor);

		// Character name (centered, colored)
		int textCenterX = x + CARD_WIDTH / 2;
		graphics.drawCenteredString(this.font, DISPLAY_NAMES[index], textCenterX, y + 6, color);

		// Divider
		int divY = y + 20;
		graphics.fill(x + 10, divY, x + CARD_WIDTH - 10, divY + 1, 0x44FFFFFF);

		// Description — word-wrapped
		int descY = divY + 5;
		int maxDescWidth = CARD_WIDTH - 20;
		int cardBottom = y + CARD_HEIGHT - 4;
		Component descText = Component.literal(DESCRIPTIONS[index]);
		List<FormattedCharSequence> lines = this.font.split(descText, maxDescWidth);

		for (FormattedCharSequence line : lines) {
			if (descY + this.font.lineHeight > cardBottom) {
				break;
			}
			graphics.drawString(this.font, line, x + 10, descY, 0xFFBBBBBB);
			descY += this.font.lineHeight + 1;
		}
	}

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
		if (event.button() == 0 && hoveredIndex >= 0 && hoveredIndex < CHARACTER_NAMES.length) {
			ClientPlayNetworking.send(new ChooseCharacterPayload(CHARACTER_NAMES[hoveredIndex]));
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

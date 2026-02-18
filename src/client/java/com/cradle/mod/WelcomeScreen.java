package com.cradle.mod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Full-screen welcome/intro screen shown to first-time players.
 * Introduces the Cradle universe and Sacred Arts before opening
 * the Path Selection screen. Cannot be closed with Escape.
 */
public class WelcomeScreen extends Screen {

	private static final String[] LORE_LINES = {
			"\u00A7d\u00A7lWelcome to the World of Cradle",
			"",
			"\u00A77In this world, all power flows from \u00A7bMadra \u00A77\u2014 the vital",
			"\u00A77energy that runs through every living thing.",
			"",
			"\u00A77Those who learn to harness Madra are known as",
			"\u00A7e\u00A7lSacred Artists\u00A77. Through discipline and training,",
			"\u00A77they refine their spirit and ascend through the stages",
			"\u00A77of power \u2014 from \u00A7aFoundation \u00A77to \u00A76Monarch\u00A77.",
			"",
			"\u00A77To grow stronger, you must \u00A7aCycle \u00A77\u2014 stand still and",
			"\u00A77draw aura from the world around you, refining it into Madra.",
			"",
			"\u00A77Your journey begins with a choice: the \u00A7ePath \u00A77you walk",
			"\u00A77will shape your techniques, your fighting style, and your destiny.",
			"",
			"\u00A78Choose wisely. The sacred arts await.",
	};

	public WelcomeScreen() {
		super(Component.literal("Welcome to Cradle"));
	}

	@Override
	protected void init() {
		// "Begin Your Journey" button at the bottom center
		int buttonWidth = 200;
		int buttonHeight = 20;
		int buttonX = (this.width - buttonWidth) / 2;
		int buttonY = this.height - 40;

		this.addRenderableWidget(Button.builder(
				Component.literal("\u00A7e\u00A7lBegin Your Journey"),
				button -> {
					// Open path selection screen
					this.minecraft.setScreen(new PathSelectionScreen());
				}
		).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false; // Must click "Begin Your Journey"
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Dark background
		renderTransparentBackground(graphics);

		// Draw lore text, centered
		int startY = 40;
		int lineSpacing = 12;

		for (int i = 0; i < LORE_LINES.length; i++) {
			String line = LORE_LINES[i];
			if (line.isEmpty()) {
				startY += lineSpacing / 2; // Half-space for blank lines
				continue;
			}

			Component text = Component.literal(line);
			// Word-wrap long lines
			List<FormattedCharSequence> wrapped = this.font.split(text, this.width - 60);
			for (FormattedCharSequence wrappedLine : wrapped) {
				int lineWidth = this.font.width(wrappedLine);
				int x = (this.width - lineWidth) / 2;
				graphics.drawString(this.font, wrappedLine, x, startY, 0xFFFFFFFF, true);
				startY += lineSpacing;
			}
		}

		// Render widgets (the button)
		super.render(graphics, mouseX, mouseY, partialTick);
	}
}

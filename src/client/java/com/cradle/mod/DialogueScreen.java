package com.cradle.mod;

import com.cradle.mod.network.DialogueResponsePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Dialogue screen for NPC conversations. Renders a dark semi-transparent panel
 * at the bottom of the screen with the speaker name, dialogue text, and
 * clickable response options.
 */
public class DialogueScreen extends Screen {
	private final String speakerName;
	private final String dialogueText;
	private final List<String> optionLabels;
	private final int npcEntityId;
	private int hoveredOption = -1;

	public DialogueScreen(String speakerName, String text, List<String> options, int npcEntityId) {
		super(Component.literal("Dialogue"));
		this.speakerName = speakerName;
		this.dialogueText = text;
		this.optionLabels = options;
		this.npcEntityId = npcEntityId;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Dark semi-transparent panel at the bottom of the screen
		int panelHeight = 140;
		int panelTop = this.height - panelHeight - 10;
		int panelLeft = 20;
		int panelRight = this.width - 20;
		int panelBottom = this.height - 10;

		// Draw panel background
		graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xCC1A1A2E);

		// Draw border
		graphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0xFF4A4A6E);        // top
		graphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0xFF4A4A6E);   // bottom
		graphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0xFF4A4A6E);       // left
		graphics.fill(panelRight - 1, panelTop, panelRight, panelBottom, 0xFF4A4A6E);     // right

		// Speaker name in gold
		graphics.drawString(this.font, speakerName, panelLeft + 12, panelTop + 10, 0xFFFFD700);

		// Separator line under speaker name
		graphics.fill(panelLeft + 12, panelTop + 22, panelRight - 12, panelTop + 23, 0xFF4A4A6E);

		// Dialogue text (word-wrapped)
		int textX = panelLeft + 12;
		int textY = panelTop + 28;
		int maxWidth = panelRight - panelLeft - 24;

		List<FormattedCharSequence> wrappedLines =
				this.font.split(Component.literal(dialogueText), maxWidth);
		for (FormattedCharSequence line : wrappedLines) {
			graphics.drawString(this.font, line, textX, textY, 0xFFE0E0E0);
			textY += 12;
		}

		// Options section
		int optionY = panelTop + 80;
		hoveredOption = -1;
		for (int i = 0; i < optionLabels.size(); i++) {
			String label = "> " + optionLabels.get(i);
			int optionX = panelLeft + 20;
			int labelWidth = this.font.width(label);

			// Check hover
			boolean hovered = mouseX >= optionX && mouseX <= optionX + labelWidth
					&& mouseY >= optionY && mouseY <= optionY + 12;
			if (hovered) {
				hoveredOption = i;
			}

			int color = hovered ? 0xFFFFAA00 : 0xFFCCCCCC;
			graphics.drawString(this.font, label, optionX, optionY, color);
			optionY += 14;
		}

		// Hint text at bottom
		String hint = "Click a response to continue";
		int hintWidth = this.font.width(hint);
		graphics.drawString(this.font, hint, panelRight - hintWidth - 12, panelBottom - 14, 0xFF666666);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	// ── Click handling ────────────────────────────────────────────────

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		if (event.button() == 0 && hoveredOption >= 0 && hoveredOption < optionLabels.size()) {
			ClientPlayNetworking.send(new DialogueResponsePayload(npcEntityId, hoveredOption));
			// Don't close yet - wait for server to send next node or close signal
			return true;
		}
		return super.mouseClicked(event, bl);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(null);
	}

	/**
	 * Update the screen with new dialogue data (called when server sends next node).
	 * If there are no more options and hasMore is false, close the dialogue.
	 */
	public static void updateOrOpen(Minecraft mc, String speakerName,
	                                 String text, List<String> options, int entityId, boolean hasMore) {
		if (!hasMore && options.isEmpty()) {
			// End of dialogue
			mc.setScreen(null);
			return;
		}
		mc.setScreen(new DialogueScreen(speakerName, text, options, entityId));
	}
}

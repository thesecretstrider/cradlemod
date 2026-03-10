package com.cradle.mod;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * HUD renderer for the ability slot bar.
 * Draws a small bar of 3-6 ability slot squares in the bottom-left,
 * above the willpower bar.
 *
 * Each slot shows:
 * - Background color based on ability type (Enforcer=green, Striker=red, Ruler=blue)
 * - Active glow border for toggle abilities
 * - Upgrade level number
 * - Key label (Z/X/C/R/F/T)
 * - Empty/locked state for unequipped or unavailable slots
 */
public final class AbilitySlotBarRenderer {

	private static final int SLOT_SIZE = 22;
	private static final int SLOT_GAP = 2;
	private static final int BAR_X = 4;    // 4px from left edge
	private static final int BAR_Y_OFFSET = 30; // px above bottom of screen
	private static final String[] KEY_LABELS = {"Z", "X", "C", "R", "F", "T"};

	// Type colors (ARGB)
	private static final int COLOR_ENFORCER = 0xFF22AA44; // green
	private static final int COLOR_STRIKER = 0xFFCC3333;  // red
	private static final int COLOR_RULER = 0xFF3366CC;    // blue
	private static final int COLOR_EMPTY = 0xFF333333;    // dark grey
	private static final int COLOR_LOCKED = 0xFF1A1A1A;   // very dark
	private static final int COLOR_ACTIVE_GLOW = 0xFFFFDD00; // yellow glow
	private static final int COLOR_BORDER = 0xFF555555;   // slot border
	private static final int COLOR_BG = 0xAA111111;       // background fill

	private AbilitySlotBarRenderer() {}

	/**
	 * Called from HudRenderCallback every frame.
	 */
	public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		if (!ClientCradleData.hasChosenPath()) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;

		long time = System.currentTimeMillis();

		int screenHeight = mc.getWindow().getGuiScaledHeight();
		int barY = screenHeight - BAR_Y_OFFSET - SLOT_SIZE;

		// Determine how many slots to show (based on progression stage)
		int visibleSlots = getVisibleSlotCount();

		for (int i = 0; i < visibleSlots; i++) {
			int x = BAR_X + i * (SLOT_SIZE + SLOT_GAP);
			int y = barY;

			String abilityId = ClientLoadoutData.getAbilityId(i);
			boolean hasAbility = abilityId != null;
			boolean isActive = ClientLoadoutData.isSlotActive(i);
			int level = ClientLoadoutData.getUpgradeLevel(i);

			// Draw slot background
			if (hasAbility) {
				int typeColor = getAbilityTypeColor(abilityId);
				// Active glow border
				if (isActive) {
					// Pulsing glow effect
					float pulse = (float) (0.6 + 0.4 * Math.sin(time / 200.0));
					int alpha = (int) (255 * pulse);
					int glowColor = (alpha << 24) | (COLOR_ACTIVE_GLOW & 0x00FFFFFF);
					graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, glowColor);
				} else {
					graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, COLOR_BORDER);
				}
				// Type-colored background
				graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, typeColor);

				// Cooldown overlay (grey sweep from top)
				float cooldown = ClientLoadoutData.getCooldownProgress(i);
				if (cooldown > 0f) {
					int cooldownHeight = (int) (SLOT_SIZE * cooldown);
					graphics.fill(x, y, x + SLOT_SIZE, y + cooldownHeight, 0xAA222222);
				}

				// Charge progress overlay (fills from bottom, yellow→orange)
				float chargeProgress = ClientLoadoutData.getChargeProgress(i);
				if (chargeProgress > 0f) {
					int chargeHeight = (int) (SLOT_SIZE * chargeProgress);
					int chargeY = y + SLOT_SIZE - chargeHeight;

					// Color shifts from yellow (low charge) to orange-red (full charge)
					int r = 255;
					int g = (int) (255 - 100 * chargeProgress); // 255 → 155
					int b = (int) (50 * (1 - chargeProgress));   // 50 → 0
					int chargeColor = (0xCC << 24) | (r << 16) | (g << 8) | b;

					graphics.fill(x, chargeY, x + SLOT_SIZE, y + SLOT_SIZE, chargeColor);

					// Pulsing border when fully charged
					if (chargeProgress >= 1.0f) {
						float pulse = (float) (0.5 + 0.5 * Math.sin(time / 100.0));
						int alpha = (int) (200 * pulse);
						int fullChargeGlow = (alpha << 24) | 0xFF6600;
						graphics.fill(x - 2, y - 2, x + SLOT_SIZE + 2, y + SLOT_SIZE + 2, fullChargeGlow);
					}

					// Charge percentage text centered in slot
					String pctStr = String.format("%.0f%%", chargeProgress * 100);
					int textX = x + (SLOT_SIZE - mc.font.width(pctStr)) / 2;
					int textY = y + (SLOT_SIZE - 8) / 2;
					graphics.drawString(mc.font, pctStr, textX, textY, 0xFFFFFF00, true);
				}

				// Level number (bottom-right corner)
				if (level > 0 && chargeProgress <= 0f) {
					// Hide level number while charging (charge % takes priority)
					String lvlStr = String.valueOf(level);
					int textX = x + SLOT_SIZE - mc.font.width(lvlStr) - 1;
					int textY = y + SLOT_SIZE - 9;
					graphics.drawString(mc.font, lvlStr, textX, textY, 0xFFFFFFFF, true);
				}
			} else {
				// Empty or locked slot
				graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, COLOR_BORDER);
				graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE,
						isSlotUnlocked(i) ? COLOR_EMPTY : COLOR_LOCKED);
			}

			// Key label (top-left corner, always visible)
			graphics.drawString(mc.font, KEY_LABELS[i], x + 2, y + 1, 0xFFCCCCCC, true);
		}
	}

	/**
	 * Get the type color for an ability based on its ID.
	 * Simple heuristic based on naming conventions in AbilityDefinitions.
	 */
	private static int getAbilityTypeColor(String abilityId) {
		if (abilityId == null) return COLOR_EMPTY;
		// Delegate to SkillTreeScreen which has the full ability type sets
		int color = SkillTreeScreen.getAbilityTypeColorStatic(abilityId);
		return color != 0 ? color : COLOR_EMPTY;
	}

	/**
	 * How many slots are visible for the player's current stage.
	 */
	private static int getVisibleSlotCount() {
		String stage = ClientCradleData.stage;
		return switch (stage) {
			case "FOUNDATION" -> 1;  // Basic Enforcement only
			case "COPPER" -> 2;      // + Striker pick
			case "IRON" -> 3;        // + Enforcer or Ruler
			case "JADE", "LOW_GOLD", "HIGH_GOLD", "TRUEGOLD" -> 4; // + remaining type
			case "UNDERLORD", "OVERLORD" -> 5; // Branch/universal unlocks
			default -> 6; // Archlord+ gets all 6
		};
	}

	/**
	 * Whether a slot is unlocked for the player (even if empty).
	 */
	private static boolean isSlotUnlocked(int slot) {
		return slot < getVisibleSlotCount();
	}
}

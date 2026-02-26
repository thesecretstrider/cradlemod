package com.cradle.mod;

import com.cradle.mod.network.ChooseAbilityPayload;
import com.cradle.mod.network.SwapAbilityPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Ability picker screen — shows a grid of available abilities for the player
 * to equip in a specific loadout slot.
 *
 * Used for:
 * - Swapping abilities from the skill tree screen
 * - Picking abilities at stage gates (Foundation/Copper/Iron/Low Gold)
 *
 * Shows warning when swapping: "This will reset upgrade levels!"
 */
public class AbilityPickerScreen extends Screen {

	private static final int CARD_WIDTH = 150;
	private static final int CARD_HEIGHT = 50;
	private static final int CARD_GAP = 8;
	private static final int MAX_COLS = 3;

	private final int targetSlot;
	private final Screen parentScreen; // null if this is a stage-gate pick

	// Ability options
	private String[] abilityIds;
	private String[] abilityNames;
	private String[] abilityDescs;
	private int[] abilityColors;
	private int abilityCount = 0;

	// Card positions
	private int[] cardX;
	private int[] cardY;
	private int hoveredIndex = -1;

	// Warning confirmation state
	private boolean showingWarning = false;
	private String pendingAbilityId = null;
	private int confirmBtnX, confirmBtnY, cancelBtnX, cancelBtnY;
	private boolean confirmHovered, cancelHovered;

	/**
	 * Create for swapping from skill tree.
	 */
	public AbilityPickerScreen(int targetSlot, Screen parent) {
		super(Component.literal("Choose Ability"));
		this.targetSlot = targetSlot;
		this.parentScreen = parent;
	}

	/**
	 * Create for stage-gate pick (no parent screen to return to).
	 */
	public AbilityPickerScreen(int targetSlot) {
		this(targetSlot, null);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		super.init();
		buildAbilityList();
	}

	/**
	 * Build the list of available abilities based on the player's path and stage.
	 */
	private void buildAbilityList() {
		// Build list based on slot position and path
		// Client doesn't have access to AbilityRegistry, so we use hardcoded lists
		// that match what AbilityDefinitions registers on the server.
		String path = ClientCradleData.path;

		java.util.List<String[]> options = new java.util.ArrayList<>();

		// Get abilities available for this slot type
		// Slot determines what type is expected:
		// Slot 0: Enforcers (basic or path-specific)
		// Slot 1: Strikers (path-specific)
		// Slot 2-3: Enforcers or Rulers (path-specific)
		// Slot 4-5: Any (branches, universals)

		// For simplicity, offer all abilities for the player's path + universals
		addPathAbilities(path, options);
		addUniversalAbilities(options);

		abilityCount = options.size();
		abilityIds = new String[abilityCount];
		abilityNames = new String[abilityCount];
		abilityDescs = new String[abilityCount];
		abilityColors = new int[abilityCount];
		cardX = new int[abilityCount];
		cardY = new int[abilityCount];

		for (int i = 0; i < abilityCount; i++) {
			abilityIds[i] = options.get(i)[0];
			abilityNames[i] = options.get(i)[1];
			abilityDescs[i] = options.get(i)[2];
			abilityColors[i] = Integer.parseInt(options.get(i)[3]);
		}
	}

	private void addPathAbilities(String path, java.util.List<String[]> options) {
		// Universal starter
		options.add(new String[]{"basic_enforcement", "Basic Enforcement", "Basic enforcer. Small buff, low drain.", String.valueOf(0xFF22AA44)});

		boolean isUnderlordPlus = isUnderlordOrHigher();

		switch (path) {
			case "BLACK_FLAME" -> {
				options.add(new String[]{"blackflame_burning_body", "Burning Body", "Enforcer: +damage, +speed, ignites enemies", String.valueOf(0xFF22AA44)});
				options.add(new String[]{"blackflame_burst", "Blackflame Burst", "Striker: Explosive burst, burn DoT", String.valueOf(0xFFCC3333)});
				options.add(new String[]{"blackflame_domain_of_ash", "Domain of Ash", "Ruler: Area burn damage to enemies", String.valueOf(0xFF3366CC)});
				if (isUnderlordPlus) {
					options.add(new String[]{"blackflame_inferno_form", "\u00A7lInferno Form", "Branch: Evolved Burning Body. Massive power.", String.valueOf(0xFF22AA44)});
					options.add(new String[]{"blackflame_meteor", "\u00A7lMeteor", "Branch: Evolved Burst. Explosive impact.", String.valueOf(0xFFCC3333)});
					options.add(new String[]{"blackflame_scorched_earth", "\u00A7lScorched Earth", "Branch: Evolved Domain. Massive area.", String.valueOf(0xFF3366CC)});
				}
			}
			case "ENDLESS_SWORD" -> {
				options.add(new String[]{"endless_flowing_edge", "Flowing Edge", "Enforcer: +attack speed, moving bonus", String.valueOf(0xFF22AA44)});
				options.add(new String[]{"endless_slash", "Endless Slash", "Striker: Pierces multiple enemies", String.valueOf(0xFFCC3333)});
				options.add(new String[]{"endless_field_of_blades", "Field of Blades", "Ruler: Moving enemies take damage", String.valueOf(0xFF3366CC)});
				if (isUnderlordPlus) {
					options.add(new String[]{"endless_thousand_cuts", "\u00A7lThousand Cuts", "Branch: Evolved Flowing Edge. Extreme speed.", String.valueOf(0xFF22AA44)});
					options.add(new String[]{"endless_sword_storm", "\u00A7lSword Storm", "Branch: Evolved Slash. Triple barrage.", String.valueOf(0xFFCC3333)});
					options.add(new String[]{"endless_blade_barrier", "\u00A7lBlade Barrier", "Branch: Evolved Field. Constant damage.", String.valueOf(0xFF3366CC)});
				}
			}
			case "STELLAR_SPEAR" -> {
				options.add(new String[]{"stellar_alignment", "Stellar Alignment", "Enforcer: +speed, +reach, sprint bonus", String.valueOf(0xFF22AA44)});
				options.add(new String[]{"stellar_piercing_star", "Piercing Star", "Striker: Long-range piercing projectile", String.valueOf(0xFFCC3333)});
				options.add(new String[]{"stellar_spear_domain", "Spear Domain", "Ruler: Approaching enemies take damage", String.valueOf(0xFF3366CC)});
				if (isUnderlordPlus) {
					options.add(new String[]{"stellar_lightspeed", "\u00A7lLightspeed", "Branch: Evolved Alignment. Extreme speed.", String.valueOf(0xFF22AA44)});
					options.add(new String[]{"stellar_nova", "\u00A7lNova", "Branch: Evolved Piercing Star. Massive AoE.", String.valueOf(0xFFCC3333)});
					options.add(new String[]{"stellar_constellation", "\u00A7lConstellation", "Branch: Evolved Spear Domain. Allies buffed.", String.valueOf(0xFF3366CC)});
				}
			}
			case "CLOUD_HAMMER" -> {
				options.add(new String[]{"cloud_thunderous_weight", "Thunderous Weight", "Enforcer: +armor, +knockback, charged hits", String.valueOf(0xFF22AA44)});
				options.add(new String[]{"cloud_falling_hammer", "Falling Hammer", "Striker: Area strike from above, knockback", String.valueOf(0xFFCC3333)});
				options.add(new String[]{"cloud_gravity_field", "Gravity Field", "Ruler: Slows enemies, reduces jump height", String.valueOf(0xFF3366CC)});
				if (isUnderlordPlus) {
					options.add(new String[]{"cloud_living_fortress", "\u00A7lLiving Fortress", "Branch: Evolved Weight. Siege engine.", String.valueOf(0xFF22AA44)});
					options.add(new String[]{"cloud_thunderstrike", "\u00A7lThunderstrike", "Branch: Evolved Hammer. Lightning strike.", String.valueOf(0xFFCC3333)});
					options.add(new String[]{"cloud_vortex", "\u00A7lVortex", "Branch: Evolved Gravity. Pulls enemies.", String.valueOf(0xFF3366CC)});
				}
			}
			case "HOLLOW_KING" -> {
				options.add(new String[]{"hollow_circulation", "Hollow Circulation", "Enforcer: Reduced costs, passive regen", String.valueOf(0xFF22AA44)});
				options.add(new String[]{"hollow_empty_palm", "Empty Palm", "Striker: Shockwave, weakens enemies", String.valueOf(0xFFCC3333)});
				options.add(new String[]{"hollow_domain", "Hollow Domain", "Ruler: Damage reduction, slows enemies", String.valueOf(0xFF3366CC)});
				if (isUnderlordPlus) {
					options.add(new String[]{"hollow_void_body", "\u00A7lVoid Body", "Branch: Evolved Circulation. Ultimate defense.", String.valueOf(0xFF22AA44)});
					options.add(new String[]{"hollow_nullify", "\u00A7lNullify", "Branch: Evolved Empty Palm. Strips buffs.", String.valueOf(0xFFCC3333)});
					options.add(new String[]{"hollow_suppression_field", "\u00A7lSuppression Field", "Branch: Evolved Domain. Total suppression.", String.valueOf(0xFF3366CC)});
				}
			}
		}
	}

	private boolean isUnderlordOrHigher() {
		String stage = ClientCradleData.stage;
		return switch (stage) {
			case "UNDERLORD", "OVERLORD", "ARCHLORD", "SAGE", "HERALD", "MONARCH" -> true;
			default -> false;
		};
	}

	private void addUniversalAbilities(java.util.List<String[]> options) {
		// Only show universals for Underlord+
		String stage = ClientCradleData.stage;
		boolean isUnderlordPlus = switch (stage) {
			case "UNDERLORD", "OVERLORD", "ARCHLORD", "SAGE", "HERALD", "MONARCH" -> true;
			default -> false;
		};
		if (isUnderlordPlus) {
			options.add(new String[]{"universal_madra_shield", "Madra Shield", "Ruler: Absorbs damage (any path)", String.valueOf(0xFF3366CC)});
			options.add(new String[]{"universal_spirit_pulse", "Spirit Pulse", "Striker: AoE knockback (any path)", String.valueOf(0xFFCC3333)});
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderTransparentBackground(graphics);

		int centerX = this.width / 2;

		if (showingWarning) {
			renderWarningDialog(graphics, mouseX, mouseY, centerX);
			super.render(graphics, mouseX, mouseY, partialTick);
			return;
		}

		// Title
		String title = "\u00A76\u00A7lChoose Ability for Slot " + (targetSlot + 1);
		graphics.drawCenteredString(this.font, title, centerX, 20, 0xFFFFFFFF);

		// Warning text
		if (parentScreen != null && ClientLoadoutData.hasAbility(targetSlot)) {
			graphics.drawCenteredString(this.font, "\u00A7c\u26A0 Swapping will reset upgrade levels!",
					centerX, 32, 0xFFFF6666);
		}

		// Calculate grid
		int cols = Math.min(abilityCount, MAX_COLS);
		int rows = (abilityCount + cols - 1) / cols;
		int gridWidth = cols * CARD_WIDTH + (cols - 1) * CARD_GAP;
		int gridStartX = centerX - gridWidth / 2;
		int gridStartY = 50;

		hoveredIndex = -1;

		for (int i = 0; i < abilityCount; i++) {
			int row = i / cols;
			int col = i % cols;
			int cx = gridStartX + col * (CARD_WIDTH + CARD_GAP);
			int cy = gridStartY + row * (CARD_HEIGHT + CARD_GAP);
			cardX[i] = cx;
			cardY[i] = cy;

			boolean hovered = mouseX >= cx && mouseX <= cx + CARD_WIDTH
					&& mouseY >= cy && mouseY <= cy + CARD_HEIGHT;
			if (hovered) hoveredIndex = i;

			// Card background
			int bgColor = hovered ? 0xFF3A3A3A : 0xFF2A2A2A;
			graphics.fill(cx, cy, cx + CARD_WIDTH, cy + CARD_HEIGHT, 0xFF000000);
			graphics.fill(cx + 1, cy + 1, cx + CARD_WIDTH - 1, cy + CARD_HEIGHT - 1, bgColor);

			// Type color left border
			graphics.fill(cx, cy, cx + 3, cy + CARD_HEIGHT, abilityColors[i]);

			// Ability name
			graphics.drawString(this.font, abilityNames[i], cx + 6, cy + 4, 0xFFFFFFFF, true);

			// Description
			graphics.drawString(this.font, abilityDescs[i], cx + 6, cy + 16, 0xFFAAAAAA, false);

			// Already equipped indicator
			String currentId = ClientLoadoutData.getAbilityId(targetSlot);
			if (abilityIds[i].equals(currentId)) {
				graphics.drawString(this.font, "\u00A7a\u2713 Equipped", cx + 6, cy + CARD_HEIGHT - 12,
						0xFF44AA44, true);
			}

			// Hover highlight
			if (hovered) {
				graphics.fill(cx + 1, cy + 1, cx + CARD_WIDTH - 1, cy + 2, 0x44FFFFFF);
			}
		}

		// Back button
		String back = parentScreen != null ? "\u00A77Press ESC to go back" : "\u00A77Press ESC to cancel";
		graphics.drawCenteredString(this.font, back, centerX,
				this.height - 20, 0xFF888888);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void renderWarningDialog(GuiGraphics graphics, int mouseX, int mouseY, int centerX) {
		int dialogW = 250;
		int dialogH = 80;
		int dx = centerX - dialogW / 2;
		int dy = this.height / 2 - dialogH / 2;

		graphics.fill(dx, dy, dx + dialogW, dy + dialogH, 0xFF000000);
		graphics.fill(dx + 1, dy + 1, dx + dialogW - 1, dy + dialogH - 1, 0xFF2A2A2A);

		graphics.drawCenteredString(this.font, "\u00A7c\u00A7lWarning!", centerX, dy + 6, 0xFFFF4444);
		graphics.drawCenteredString(this.font, "Swap to " + getAbilityName(pendingAbilityId) + "?",
				centerX, dy + 20, 0xFFFFFFFF);
		graphics.drawCenteredString(this.font, "\u00A77All upgrade progress will be lost!",
				centerX, dy + 32, 0xFFCCCCCC);

		// Confirm button
		int btnW = 60;
		int btnH = 16;
		confirmBtnX = centerX - btnW - 5;
		confirmBtnY = dy + dialogH - btnH - 8;
		confirmHovered = mouseX >= confirmBtnX && mouseX <= confirmBtnX + btnW
				&& mouseY >= confirmBtnY && mouseY <= confirmBtnY + btnH;
		graphics.fill(confirmBtnX, confirmBtnY, confirmBtnX + btnW, confirmBtnY + btnH,
				confirmHovered ? 0xFF446644 : 0xFF335533);
		graphics.drawCenteredString(this.font, "Confirm", confirmBtnX + btnW / 2, confirmBtnY + 4, 0xFF44FF44);

		// Cancel button
		cancelBtnX = centerX + 5;
		cancelBtnY = confirmBtnY;
		cancelHovered = mouseX >= cancelBtnX && mouseX <= cancelBtnX + btnW
				&& mouseY >= cancelBtnY && mouseY <= cancelBtnY + btnH;
		graphics.fill(cancelBtnX, cancelBtnY, cancelBtnX + btnW, cancelBtnY + btnH,
				cancelHovered ? 0xFF664444 : 0xFF553333);
		graphics.drawCenteredString(this.font, "Cancel", cancelBtnX + btnW / 2, cancelBtnY + 4, 0xFFFF4444);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		if (event.button() == 0) {
			if (showingWarning) {
				if (confirmHovered && pendingAbilityId != null) {
					ClientPlayNetworking.send(new SwapAbilityPayload(targetSlot, pendingAbilityId));
					if (parentScreen != null) {
						minecraft.setScreen(parentScreen);
					} else {
						onClose();
					}
					return true;
				}
				if (cancelHovered) {
					showingWarning = false;
					pendingAbilityId = null;
					return true;
				}
				return false;
			}

			if (hoveredIndex >= 0 && hoveredIndex < abilityCount) {
				String selectedId = abilityIds[hoveredIndex];
				// Don't swap to the same ability
				String currentId = ClientLoadoutData.getAbilityId(targetSlot);
				if (selectedId.equals(currentId)) return true;

				// Stage-gate pick: slot is empty, use ChooseAbilityPayload
				if (!ClientLoadoutData.hasAbility(targetSlot)) {
					ClientPlayNetworking.send(new ChooseAbilityPayload(selectedId, targetSlot));
					if (parentScreen != null) {
						minecraft.setScreen(parentScreen);
					} else {
						onClose();
					}
					return true;
				}

				// If swapping and slot has an ability with levels, show warning
				if (parentScreen != null && ClientLoadoutData.hasAbility(targetSlot)
						&& ClientLoadoutData.getUpgradeLevel(targetSlot) > 1) {
					showingWarning = true;
					pendingAbilityId = selectedId;
					return true;
				}

				// Direct swap (no warning needed — level 1)
				ClientPlayNetworking.send(new SwapAbilityPayload(targetSlot, selectedId));
				if (parentScreen != null) {
					minecraft.setScreen(parentScreen);
				} else {
					onClose();
				}
				return true;
			}
		}
		return super.mouseClicked(event, bl);
	}

	@Override
	public void onClose() {
		if (parentScreen != null) {
			minecraft.setScreen(parentScreen);
		} else {
			super.onClose();
		}
	}

	private String getAbilityName(String id) {
		return SkillTreeScreen.getAbilityDisplayName(id);
	}
}

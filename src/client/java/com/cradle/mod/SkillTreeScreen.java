package com.cradle.mod;

import com.cradle.mod.network.UpgradeAbilityPayload;
import com.cradle.mod.network.BranchAbilityPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Skill Tree screen — opened via K key.
 * Displays 2 rows of 3 slot cards showing the player's ability loadout.
 * Each card shows ability name, type, level, upgrade button, swap button,
 * and branch indicator.
 *
 * Layout:
 *   [Slot 0: Z]  [Slot 1: X]  [Slot 2: C]
 *   [Slot 3: R]  [Slot 4: F]  [Slot 5: T]
 */
public class SkillTreeScreen extends Screen {

	// Panel and card dimensions
	private static final int CARD_WIDTH = 120;
	private static final int CARD_HEIGHT = 90;
	private static final int CARD_GAP = 10;
	private static final int ROW_GAP = 10;
	private static final int BUTTON_WIDTH = 50;
	private static final int BUTTON_HEIGHT = 14;

	private static final String[] KEY_LABELS = {"Z", "X", "C", "R", "F", "T"};

	// Type colors
	private static final int COLOR_ENFORCER = 0xFF22AA44;
	private static final int COLOR_STRIKER = 0xFFCC3333;
	private static final int COLOR_RULER = 0xFF3366CC;
	private static final int COLOR_EMPTY = 0xFF444444;
	private static final int COLOR_LOCKED = 0xFF222222;

	// Button tracking
	private final int[] upgradeBtnX = new int[6];
	private final int[] upgradeBtnY = new int[6];
	private final int[] swapBtnX = new int[6];
	private final int[] swapBtnY = new int[6];
	private final int[] branchBtnX = new int[6];
	private final int[] branchBtnY = new int[6];
	private final boolean[] upgradeHovered = new boolean[6];
	private final boolean[] swapHovered = new boolean[6];
	private final boolean[] branchHovered = new boolean[6];

	public SkillTreeScreen() {
		super(Component.literal("Skill Tree"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderTransparentBackground(graphics);

		int centerX = this.width / 2;
		int totalWidth = 3 * CARD_WIDTH + 2 * CARD_GAP;
		int totalHeight = 2 * CARD_HEIGHT + ROW_GAP;
		int startX = centerX - totalWidth / 2;
		int startY = this.height / 2 - totalHeight / 2 - 15;

		// Title
		String title = "\u00A76\u00A7lSkill Tree";
		graphics.drawCenteredString(this.font, title, centerX, startY - 18, 0xFFFFFFFF);

		// Upgrade points display
		String pointsStr = "\u00A7eUpgrade Points: \u00A7f" + ClientLoadoutData.upgradePoints;
		graphics.drawCenteredString(this.font, pointsStr, centerX, startY - 8, 0xFFFFFFFF);

		int visibleSlots = getVisibleSlotCount();

		for (int i = 0; i < 6; i++) {
			int row = i / 3;
			int col = i % 3;
			int cardX = startX + col * (CARD_WIDTH + CARD_GAP);
			int cardY = startY + 5 + row * (CARD_HEIGHT + ROW_GAP);

			boolean visible = i < visibleSlots;
			boolean hasAbility = ClientLoadoutData.hasAbility(i);
			String abilityId = ClientLoadoutData.getAbilityId(i);
			int level = ClientLoadoutData.getUpgradeLevel(i);
			boolean active = ClientLoadoutData.isSlotActive(i);

			// Card background
			int bgColor = !visible ? COLOR_LOCKED : (hasAbility ? 0xFF2A2A2A : COLOR_EMPTY);
			graphics.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + CARD_HEIGHT, 0xFF000000);
			graphics.fill(cardX + 1, cardY + 1, cardX + CARD_WIDTH - 1, cardY + CARD_HEIGHT - 1, bgColor);

			// Active indicator - colored left border
			if (active) {
				int typeColor = getTypeColor(abilityId);
				graphics.fill(cardX, cardY, cardX + 3, cardY + CARD_HEIGHT, typeColor);
			}

			// Key label (top-left)
			graphics.drawString(this.font, "\u00A77[" + KEY_LABELS[i] + "]", cardX + 4, cardY + 3, 0xFFAAAAAA, true);

			if (!visible) {
				// Locked slot
				String lockMsg = getUnlockMessage(i);
				graphics.drawCenteredString(this.font, "\u00A78" + lockMsg,
						cardX + CARD_WIDTH / 2, cardY + CARD_HEIGHT / 2 - 4, 0xFF666666);
				continue;
			}

			if (!hasAbility) {
				// Empty slot
				graphics.drawCenteredString(this.font, "\u00A78Empty",
						cardX + CARD_WIDTH / 2, cardY + CARD_HEIGHT / 2 - 4, 0xFF888888);
				continue;
			}

			// Ability name
			String name = getAbilityDisplayName(abilityId);
			int typeColor = getTypeColor(abilityId);
			String typeName = getTypeName(abilityId);

			// Type indicator
			graphics.drawString(this.font, typeName, cardX + CARD_WIDTH - this.font.width(typeName) - 4,
					cardY + 3, typeColor, true);

			// Ability name (truncated if too long)
			if (this.font.width(name) > CARD_WIDTH - 8) {
				name = this.font.plainSubstrByWidth(name, CARD_WIDTH - 12) + "..";
			}
			graphics.drawString(this.font, name, cardX + 4, cardY + 14, 0xFFFFFFFF, true);

			// Level display
			String levelStr = "Lv. " + level;
			graphics.drawString(this.font, levelStr, cardX + 4, cardY + 26, 0xFFCCCC00, true);

			// Visual tier indicator
			int tier = getVisualTier(level);
			if (tier > 0) {
				String stars = "\u2605".repeat(tier);
				graphics.drawString(this.font, stars, cardX + 4 + this.font.width(levelStr) + 4,
						cardY + 26, 0xFFFFAA00, true);
			}

			// Active status
			if (active) {
				graphics.drawString(this.font, "\u00A7aACTIVE", cardX + CARD_WIDTH - 34,
						cardY + 14, 0xFF00FF00, true);
			}

			// Upgrade button
			int ubX = cardX + 4;
			int ubY = cardY + CARD_HEIGHT - BUTTON_HEIGHT - 4;
			upgradeBtnX[i] = ubX;
			upgradeBtnY[i] = ubY;
			boolean canUpgrade = ClientLoadoutData.upgradePoints > 0 && level < 20;
			upgradeHovered[i] = canUpgrade && mouseX >= ubX && mouseX <= ubX + BUTTON_WIDTH
					&& mouseY >= ubY && mouseY <= ubY + BUTTON_HEIGHT;
			int ubColor = canUpgrade ? (upgradeHovered[i] ? 0xFF44AA44 : 0xFF336633) : 0xFF333333;
			graphics.fill(ubX, ubY, ubX + BUTTON_WIDTH, ubY + BUTTON_HEIGHT, ubColor);
			graphics.drawCenteredString(this.font, canUpgrade ? "Upgrade" : "\u00A78Upgrade",
					ubX + BUTTON_WIDTH / 2, ubY + 3, canUpgrade ? 0xFFFFFFFF : 0xFF666666);

			// Swap button (next to upgrade)
			int sbX = cardX + CARD_WIDTH - BUTTON_WIDTH - 4;
			int sbY = ubY;
			swapBtnX[i] = sbX;
			swapBtnY[i] = sbY;
			swapHovered[i] = mouseX >= sbX && mouseX <= sbX + BUTTON_WIDTH
					&& mouseY >= sbY && mouseY <= sbY + BUTTON_HEIGHT;
			int sbColor = swapHovered[i] ? 0xFF664444 : 0xFF443333;
			graphics.fill(sbX, sbY, sbX + BUTTON_WIDTH, sbY + BUTTON_HEIGHT, sbColor);
			graphics.drawCenteredString(this.font, "Swap",
					sbX + BUTTON_WIDTH / 2, sbY + 3, 0xFFCCAAAA);

			// Branch indicator (if upgrade level >= 10 and branch available)
			if (level >= 10 && hasBranch(abilityId)) {
				int emptySlot = ClientLoadoutData.getEquippedCount() < 6
						? findFirstEmptySlot() : -1;
				boolean canBranch = emptySlot >= 0;

				int bbX = cardX + 4;
				int bbY = ubY - BUTTON_HEIGHT - 3;
				branchBtnX[i] = bbX;
				branchBtnY[i] = bbY;
				branchHovered[i] = canBranch && mouseX >= bbX && mouseX <= bbX + CARD_WIDTH - 8
						&& mouseY >= bbY && mouseY <= bbY + BUTTON_HEIGHT;
				int bbColor = canBranch ? (branchHovered[i] ? 0xFFAA44CC : 0xFF7733AA) : 0xFF333333;
				graphics.fill(bbX, bbY, bbX + CARD_WIDTH - 8, bbY + BUTTON_HEIGHT, bbColor);

				// Pulsing "Branch!" text
				long time = System.currentTimeMillis();
				float pulse = (float) (0.7 + 0.3 * Math.sin(time / 300.0));
				int alpha = (int) (255 * pulse);
				int branchTextColor = canBranch ? ((alpha << 24) | 0x00FFDDFF) : 0xFF666666;
				graphics.drawCenteredString(this.font,
						canBranch ? "\u2728 Branch Available!" : "\u00A78No empty slot",
						bbX + (CARD_WIDTH - 8) / 2, bbY + 3, branchTextColor);
			}
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		if (event.button() == 0) {
			for (int i = 0; i < 6; i++) {
				if (!ClientLoadoutData.hasAbility(i)) continue;

				// Upgrade
				if (upgradeHovered[i] && ClientLoadoutData.upgradePoints > 0
						&& ClientLoadoutData.getUpgradeLevel(i) < 20) {
					ClientPlayNetworking.send(new UpgradeAbilityPayload(i));
					return true;
				}

				// Swap
				if (swapHovered[i]) {
					minecraft.setScreen(new AbilityPickerScreen(i, this));
					return true;
				}

				// Branch
				if (branchHovered[i]) {
					int emptySlot = findFirstEmptySlot();
					if (emptySlot >= 0) {
						ClientPlayNetworking.send(new BranchAbilityPayload(i, emptySlot));
						return true;
					}
				}
			}
		}
		return super.mouseClicked(event, bl);
	}

	// ── Helpers ───────────────────────────────────────────────────────

	private static int getVisibleSlotCount() {
		String stage = ClientCradleData.stage;
		return switch (stage) {
			case "FOUNDATION" -> 1;
			case "COPPER" -> 2;
			case "IRON" -> 3;
			case "JADE", "LOW_GOLD", "HIGH_GOLD", "TRUEGOLD" -> 4;
			case "UNDERLORD", "OVERLORD" -> 5;
			default -> 6;
		};
	}

	private static String getUnlockMessage(int slot) {
		return switch (slot) {
			case 1 -> "Unlocks at Copper";
			case 2 -> "Unlocks at Iron";
			case 3 -> "Unlocks at Low Gold";
			case 4 -> "Unlocks at Underlord";
			case 5 -> "Unlocks at Archlord";
			default -> "Locked";
		};
	}

	static String getAbilityDisplayName(String id) {
		if (id == null) return "Empty";
		return switch (id) {
			// Base abilities
			case "basic_enforcement" -> "Basic Enforcement";
			case "blackflame_burning_body" -> "Burning Body";
			case "blackflame_burst" -> "Blackflame Burst";
			case "blackflame_domain_of_ash" -> "Domain of Ash";
			case "endless_flowing_edge" -> "Flowing Edge";
			case "endless_slash" -> "Endless Slash";
			case "endless_field_of_blades" -> "Field of Blades";
			case "stellar_alignment" -> "Stellar Alignment";
			case "stellar_piercing_star" -> "Piercing Star";
			case "stellar_spear_domain" -> "Spear Domain";
			case "cloud_thunderous_weight" -> "Thunderous Weight";
			case "cloud_falling_hammer" -> "Falling Hammer";
			case "cloud_gravity_field" -> "Gravity Field";
			case "hollow_circulation" -> "Hollow Circulation";
			case "hollow_empty_palm" -> "Empty Palm";
			case "hollow_domain" -> "Hollow Domain";
			// Branch abilities
			case "blackflame_inferno_form" -> "Inferno Form";
			case "blackflame_meteor" -> "Meteor";
			case "blackflame_scorched_earth" -> "Scorched Earth";
			case "endless_thousand_cuts" -> "Thousand Cuts";
			case "endless_sword_storm" -> "Sword Storm";
			case "endless_blade_barrier" -> "Blade Barrier";
			case "stellar_lightspeed" -> "Lightspeed";
			case "stellar_nova" -> "Nova";
			case "stellar_constellation" -> "Constellation";
			case "cloud_living_fortress" -> "Living Fortress";
			case "cloud_thunderstrike" -> "Thunderstrike";
			case "cloud_vortex" -> "Vortex";
			case "hollow_void_body" -> "Void Body";
			case "hollow_nullify" -> "Nullify";
			case "hollow_suppression_field" -> "Suppression Field";
			// Universal abilities
			case "universal_madra_shield" -> "Madra Shield";
			case "universal_spirit_pulse" -> "Spirit Pulse";
			default -> {
				String name = id.replace('_', ' ');
				if (!name.isEmpty()) {
					name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
				}
				yield name;
			}
		};
	}

	// Enforcer ability IDs (base + branches) — package-private for AbilitySlotBarRenderer/CradleModClient access
	static final java.util.Set<String> ENFORCER_IDS = java.util.Set.of(
		"basic_enforcement", "blackflame_burning_body", "endless_flowing_edge",
		"stellar_alignment", "cloud_thunderous_weight", "hollow_circulation",
		"blackflame_inferno_form", "endless_thousand_cuts", "stellar_lightspeed",
		"cloud_living_fortress", "hollow_void_body"
	);
	// Striker ability IDs (base + branches) — package-private for CradleModClient access
	static final java.util.Set<String> STRIKER_IDS = java.util.Set.of(
		"blackflame_burst", "endless_slash", "stellar_piercing_star",
		"cloud_falling_hammer", "hollow_empty_palm", "universal_spirit_pulse",
		"blackflame_meteor", "endless_sword_storm", "stellar_nova",
		"cloud_thunderstrike", "hollow_nullify"
	);
	// Ruler ability IDs (base + branches) — package-private for AbilitySlotBarRenderer access
	static final java.util.Set<String> RULER_IDS = java.util.Set.of(
		"blackflame_domain_of_ash", "endless_field_of_blades", "stellar_spear_domain",
		"cloud_gravity_field", "hollow_domain", "universal_madra_shield",
		"blackflame_scorched_earth", "endless_blade_barrier", "stellar_constellation",
		"cloud_vortex", "hollow_suppression_field"
	);

	private static int getTypeColor(String id) {
		if (id == null) return COLOR_EMPTY;
		if (ENFORCER_IDS.contains(id)) return COLOR_ENFORCER;
		if (STRIKER_IDS.contains(id)) return COLOR_STRIKER;
		if (RULER_IDS.contains(id)) return COLOR_RULER;
		return COLOR_EMPTY;
	}

	/**
	 * Public accessor for ability type color, used by AbilitySlotBarRenderer.
	 * Returns 0 if unknown (caller can use its own fallback).
	 */
	static int getAbilityTypeColorStatic(String id) {
		if (id == null) return 0;
		if (ENFORCER_IDS.contains(id)) return COLOR_ENFORCER;
		if (STRIKER_IDS.contains(id)) return COLOR_STRIKER;
		if (RULER_IDS.contains(id)) return COLOR_RULER;
		return 0;
	}

	private static String getTypeName(String id) {
		if (id == null) return "";
		if (ENFORCER_IDS.contains(id)) return "ENF";
		if (STRIKER_IDS.contains(id)) return "STR";
		if (RULER_IDS.contains(id)) return "RUL";
		return "???";
	}

	private static int getVisualTier(int level) {
		if (level >= 15) return 3;
		if (level >= 10) return 2;
		if (level >= 5) return 1;
		return 0;
	}

	// IDs of abilities that are themselves branches (they don't branch further)
	private static final java.util.Set<String> BRANCH_ABILITY_IDS = java.util.Set.of(
		"blackflame_inferno_form", "blackflame_meteor", "blackflame_scorched_earth",
		"endless_thousand_cuts", "endless_sword_storm", "endless_blade_barrier",
		"stellar_lightspeed", "stellar_nova", "stellar_constellation",
		"cloud_living_fortress", "cloud_thunderstrike", "cloud_vortex",
		"hollow_void_body", "hollow_nullify", "hollow_suppression_field"
	);

	private static boolean hasBranch(String id) {
		// Branch abilities are defined at level 10 for all 15 base path abilities.
		// Branch abilities themselves, basic enforcement, and universals don't branch further.
		if (id == null) return false;
		if ("basic_enforcement".equals(id)) return false;
		if ("universal_madra_shield".equals(id) || "universal_spirit_pulse".equals(id)) return false;
		if (BRANCH_ABILITY_IDS.contains(id)) return false; // Branch abilities don't branch again
		return true; // All 15 base path abilities have branches
	}

	private static int findFirstEmptySlot() {
		for (int i = 0; i < 6; i++) {
			if (!ClientLoadoutData.hasAbility(i)) return i;
		}
		return -1;
	}
}

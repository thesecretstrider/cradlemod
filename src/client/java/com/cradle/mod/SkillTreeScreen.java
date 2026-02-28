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
 * Center-out radial layout with Basic Enforcement at the hub.
 *
 * Layout:
 *            [Slot 4]              [Slot 5]
 *          (outer-UL)            (outer-UR)
 *              \                    /
 *               \                  /
 *                [Slot 1: top]
 *                      |
 *               [Slot 0: CENTER]
 *              /                  \
 *     [Slot 2: lower-left]   [Slot 3: lower-right]
 */
public class SkillTreeScreen extends Screen {

	// Node sizes per tier
	private static final int CENTER_SIZE = 54;   // Slot 0 — hub
	private static final int INNER_SIZE = 48;     // Slots 1-3
	private static final int OUTER_SIZE = 44;     // Slots 4-5
	private static final int BRANCH_SIZE = 36;    // Branch indicator nodes

	// Radial offsets from screen center (before scaling)
	private static final int[][] SLOT_OFFSETS = {
		{   0,    0 },   // 0: center
		{   0,  -78 },   // 1: top (striker pick)
		{ -68,   40 },   // 2: lower-left (iron pick)
		{  68,   40 },   // 3: lower-right (low gold auto)
		{ -98,  -98 },   // 4: outer upper-left (underlord+)
		{  98,  -98 },   // 5: outer upper-right (archlord+)
	};

	// Branch nodes appear 1.6× further along same radial direction
	private static final float BRANCH_OFFSET_MULT = 1.6f;

	// Selection panel at bottom
	private static final int PANEL_WIDTH = 280;
	private static final int PANEL_HEIGHT = 56;
	private static final int BTN_W = 56;
	private static final int BTN_H = 16;

	// Type colors
	private static final int COLOR_ENFORCER = 0xFF22AA44;
	private static final int COLOR_STRIKER = 0xFFCC3333;
	private static final int COLOR_RULER = 0xFF3366CC;
	private static final int COLOR_EMPTY = 0xFF444444;
	private static final int COLOR_LOCKED = 0xFF1A1A1A;

	// Selection / hover
	private int selectedSlot = -1;
	private int hoveredSlot = -1;
	private int hoveredBranch = -1; // which slot's branch node is hovered

	// Selection panel button hover states
	private boolean upgradeHovered = false;
	private boolean swapHovered = false;
	private boolean branchPanelHovered = false;

	// Computed positions (updated each frame based on scaling)
	private final int[] nodeX = new int[6];
	private final int[] nodeY = new int[6];
	private final int[] nodeSize = new int[6];
	private final int[] branchNodeX = new int[6];
	private final int[] branchNodeY = new int[6];
	private float scale = 1.0f;
	private int treeCenterX, treeCenterY;
	private int panelX, panelY; // selection panel position

	private static final String[] KEY_LABELS = {"Z", "X", "C", "R", "F", "T"};

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

		// Scale only if the screen is truly too small to fit
		float scaleX = (float) this.width / 300f;
		float scaleY = (float) this.height / 340f;
		scale = Math.min(1.0f, Math.min(scaleX, scaleY));
		scale = Math.max(scale, 0.4f);

		treeCenterX = this.width / 2;
		// Push the tree center lower so the upper nodes use the top of the screen
		// and the lower nodes + panel fill the bottom
		treeCenterY = this.height / 2 + (int)(30 * scale);

		int visibleSlots = getVisibleSlotCount();

		// Compute node positions
		for (int i = 0; i < 6; i++) {
			int ox = (int)(SLOT_OFFSETS[i][0] * scale);
			int oy = (int)(SLOT_OFFSETS[i][1] * scale);
			int size = i == 0 ? (int)(CENTER_SIZE * scale) :
					   i <= 3 ? (int)(INNER_SIZE * scale) :
					            (int)(OUTER_SIZE * scale);
			size = Math.max(size, 24); // minimum size
			nodeX[i] = treeCenterX + ox - size / 2;
			nodeY[i] = treeCenterY + oy - size / 2;
			nodeSize[i] = size;

			// Branch node positions
			if (i > 0) {
				int bx = (int)(SLOT_OFFSETS[i][0] * BRANCH_OFFSET_MULT * scale);
				int by = (int)(SLOT_OFFSETS[i][1] * BRANCH_OFFSET_MULT * scale);
				int bSize = (int)(BRANCH_SIZE * scale);
				bSize = Math.max(bSize, 20);
				branchNodeX[i] = treeCenterX + bx - bSize / 2;
				branchNodeY[i] = treeCenterY + by - bSize / 2;
			}
		}

		// Determine hover state
		hoveredSlot = -1;
		hoveredBranch = -1;
		for (int i = 0; i < 6; i++) {
			if (i >= visibleSlots) continue;
			if (isInsideNode(mouseX, mouseY, nodeX[i], nodeY[i], nodeSize[i])) {
				hoveredSlot = i;
			}
			// Check branch node hover
			if (i > 0 && ClientLoadoutData.hasAbility(i)) {
				String aid = ClientLoadoutData.getAbilityId(i);
				int lvl = ClientLoadoutData.getUpgradeLevel(i);
				if (lvl >= 10 && hasBranch(aid)) {
					int bSize = (int)(BRANCH_SIZE * scale);
					bSize = Math.max(bSize, 20);
					if (isInsideNode(mouseX, mouseY, branchNodeX[i], branchNodeY[i], bSize)) {
						hoveredBranch = i;
					}
				}
			}
		}

		// ── 1. Title + upgrade points ──
		String title = "\u00A76\u00A7lSkill Tree";
		graphics.drawCenteredString(this.font, title, treeCenterX, 6, 0xFFFFFFFF);
		String pointsStr = "\u00A7eUpgrade Points: \u00A7f" + ClientLoadoutData.upgradePoints;
		graphics.drawCenteredString(this.font, pointsStr, treeCenterX, 18, 0xFFFFFFFF);

		// ── 2. Connection lines (center → each inner/outer slot) ──
		for (int i = 1; i < 6; i++) {
			boolean slotVisible = i < visibleSlots;
			int lineColor = slotVisible ? 0xFF555555 : 0xFF2A2A2A;

			int fromCX = treeCenterX;
			int fromCY = treeCenterY;
			int toCX = treeCenterX + (int)(SLOT_OFFSETS[i][0] * scale);
			int toCY = treeCenterY + (int)(SLOT_OFFSETS[i][1] * scale);

			// For outer slots 4 and 5, connect through slot 1 (the top node)
			if (i == 4 || i == 5) {
				int midCX = treeCenterX + (int)(SLOT_OFFSETS[1][0] * scale);
				int midCY = treeCenterY + (int)(SLOT_OFFSETS[1][1] * scale);
				drawDottedLine(graphics, fromCX, fromCY, midCX, midCY, lineColor);
				drawDottedLine(graphics, midCX, midCY, toCX, toCY, lineColor);
			} else {
				drawDottedLine(graphics, fromCX, fromCY, toCX, toCY, lineColor);
			}
		}

		// ── 3. Branch connector lines ──
		for (int i = 1; i < 6; i++) {
			if (i >= visibleSlots || !ClientLoadoutData.hasAbility(i)) continue;
			String aid = ClientLoadoutData.getAbilityId(i);
			int lvl = ClientLoadoutData.getUpgradeLevel(i);
			if (lvl >= 10 && hasBranch(aid)) {
				int nodeCX = treeCenterX + (int)(SLOT_OFFSETS[i][0] * scale);
				int nodeCY = treeCenterY + (int)(SLOT_OFFSETS[i][1] * scale);
				int bSize = (int)(BRANCH_SIZE * scale);
				bSize = Math.max(bSize, 20);
				int branchCX = branchNodeX[i] + bSize / 2;
				int branchCY = branchNodeY[i] + bSize / 2;
				drawDottedLine(graphics, nodeCX, nodeCY, branchCX, branchCY, 0xFF7733AA);
			}
		}

		// ── 4. Render all nodes ──
		for (int i = 0; i < 6; i++) {
			boolean visible = i < visibleSlots;
			renderNode(graphics, i, visible, mouseX, mouseY);
		}

		// ── 5. Branch indicator nodes ──
		for (int i = 1; i < 6; i++) {
			if (i >= visibleSlots || !ClientLoadoutData.hasAbility(i)) continue;
			String aid = ClientLoadoutData.getAbilityId(i);
			int lvl = ClientLoadoutData.getUpgradeLevel(i);
			if (lvl >= 10 && hasBranch(aid)) {
				renderBranchNode(graphics, i);
			}
		}

		// ── 6. Selection panel at bottom ──
		if (selectedSlot >= 0 && selectedSlot < visibleSlots && ClientLoadoutData.hasAbility(selectedSlot)) {
			renderSelectionPanel(graphics, mouseX, mouseY);
		}

		// ── 7. Hint text ──
		String hint = selectedSlot >= 0 ? "\u00A77Click background to deselect" : "\u00A77Click a node to select it";
		graphics.drawCenteredString(this.font, hint, treeCenterX, this.height - 12, 0xFF888888);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	// ── Node rendering ───────────────────────────────────────────────

	private void renderNode(GuiGraphics graphics, int slot, boolean visible, int mouseX, int mouseY) {
		int x = nodeX[slot];
		int y = nodeY[slot];
		int size = nodeSize[slot];

		boolean hasAbility = ClientLoadoutData.hasAbility(slot);
		String abilityId = ClientLoadoutData.getAbilityId(slot);
		boolean isActive = ClientLoadoutData.isSlotActive(slot);
		int level = ClientLoadoutData.getUpgradeLevel(slot);
		boolean isHovered = hoveredSlot == slot;
		boolean isSelected = selectedSlot == slot;

		// Border
		int borderColor;
		if (isSelected) {
			borderColor = 0xFFFFDD00; // bright yellow
		} else if (isHovered && visible) {
			borderColor = 0xFFAAAAAA; // light grey
		} else {
			borderColor = 0xFF333333; // dark
		}
		// Draw border (2px for selected, 1px for others)
		int borderW = isSelected ? 2 : 1;
		graphics.fill(x - borderW, y - borderW, x + size + borderW, y + size + borderW, borderColor);

		if (!visible) {
			// Locked node
			graphics.fill(x, y, x + size, y + size, COLOR_LOCKED);
			// Lock message centered
			String lockMsg = getUnlockMessage(slot);
			int textWidth = this.font.width(lockMsg);
			if (textWidth > size - 4) {
				// Too wide — abbreviate
				lockMsg = "Lv " + switch (slot) {
					case 1 -> "Cu";
					case 2 -> "Fe";
					case 3 -> "Au";
					case 4 -> "UL";
					case 5 -> "AL";
					default -> "?";
				};
			}
			graphics.drawCenteredString(this.font, "\u00A78" + lockMsg,
					x + size / 2, y + size / 2 - 4, 0xFF555555);
			return;
		}

		if (!hasAbility) {
			// Empty unlocked node
			graphics.fill(x, y, x + size, y + size, COLOR_EMPTY);
			graphics.drawCenteredString(this.font, "\u00A78Empty",
					x + size / 2, y + size / 2 - 4, 0xFF888888);
			// Key label
			graphics.drawString(this.font, "\u00A77" + KEY_LABELS[slot],
					x + 2, y + 2, 0xFFAAAAAA, true);
			return;
		}

		// Filled node
		int typeColor = getTypeColor(abilityId);
		graphics.fill(x, y, x + size, y + size, 0xFF1A1A1A);

		// Type-colored strip at top (3px tall)
		graphics.fill(x, y, x + size, y + 3, typeColor);

		// Active glow effect on border
		if (isActive) {
			long time = System.currentTimeMillis();
			float pulse = (float)(0.6 + 0.4 * Math.sin(time / 200.0));
			int alpha = (int)(200 * pulse);
			int glowColor = (alpha << 24) | (0x00FFDD00);
			graphics.fill(x - borderW, y - borderW, x + size + borderW, y + 1, glowColor);
			graphics.fill(x - borderW, y + size - 1, x + size + borderW, y + size + borderW, glowColor);
			graphics.fill(x - borderW, y, x + 1, y + size, glowColor);
			graphics.fill(x + size - 1, y, x + size + borderW, y + size, glowColor);
		}

		// Type tag at top center (below strip)
		String typeName = getTypeName(abilityId);
		graphics.drawCenteredString(this.font, typeName,
				x + size / 2, y + 5, typeColor);

		// Ability name centered
		String name = getAbilityDisplayName(abilityId);
		// Truncate if too wide
		if (this.font.width(name) > size - 4) {
			name = this.font.plainSubstrByWidth(name, size - 8) + "..";
		}
		graphics.drawCenteredString(this.font, name,
				x + size / 2, y + size / 2 - 6, 0xFFFFFFFF);

		// Level + stars below name
		int tier = getVisualTier(level);
		String lvlStr = "Lv." + level;
		if (tier > 0) {
			lvlStr += " " + "\u2605".repeat(tier);
		}
		int lvlColor = tier >= 3 ? 0xFFFFAA00 : tier >= 2 ? 0xFFCCCC00 : tier >= 1 ? 0xFFAAAA44 : 0xFF888888;
		graphics.drawCenteredString(this.font, lvlStr,
				x + size / 2, y + size / 2 + 4, lvlColor);

		// "ON" indicator if active
		if (isActive) {
			graphics.drawString(this.font, "\u00A7aON",
					x + size - 16, y + size - 10, 0xFF00FF00, true);
		}

		// Key label (bottom-left)
		graphics.drawString(this.font, "\u00A77" + KEY_LABELS[slot],
				x + 2, y + size - 10, 0xFFAAAAAA, true);
	}

	private void renderBranchNode(GuiGraphics graphics, int slot) {
		int bSize = (int)(BRANCH_SIZE * scale);
		bSize = Math.max(bSize, 20);
		int x = branchNodeX[slot];
		int y = branchNodeY[slot];

		boolean isHovered = hoveredBranch == slot;
		int emptySlot = findFirstEmptySlot();
		boolean canBranch = emptySlot >= 0;

		// Pulsing border
		long time = System.currentTimeMillis();
		float pulse = (float)(0.5 + 0.5 * Math.sin(time / 250.0));
		int alpha = (int)(180 + 75 * pulse);
		int borderColor = canBranch
				? ((alpha << 24) | 0x00AA44CC)
				: 0xFF333333;

		if (isHovered && canBranch) {
			borderColor = 0xFFDD88FF;
		}

		graphics.fill(x - 1, y - 1, x + bSize + 1, y + bSize + 1, borderColor);
		graphics.fill(x, y, x + bSize, y + bSize, canBranch ? 0xFF2A1A33 : 0xFF1A1A1A);

		// "B" or branch icon
		String label = canBranch ? "\u2728" : "\u00A78B";
		graphics.drawCenteredString(this.font, label,
				x + bSize / 2, y + bSize / 2 - 4, canBranch ? 0xFFDD88FF : 0xFF555555);
	}

	// ── Selection panel ──────────────────────────────────────────────

	private void renderSelectionPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		panelX = treeCenterX - PANEL_WIDTH / 2;
		panelY = this.height - PANEL_HEIGHT - 18;

		// Panel background
		graphics.fill(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + PANEL_HEIGHT + 1, 0xFF555555);
		graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xDD111111);

		String abilityId = ClientLoadoutData.getAbilityId(selectedSlot);
		int level = ClientLoadoutData.getUpgradeLevel(selectedSlot);
		boolean isActive = ClientLoadoutData.isSlotActive(selectedSlot);
		String name = getAbilityDisplayName(abilityId);
		String typeName = getTypeName(abilityId);
		int typeColor = getTypeColor(abilityId);
		int tier = getVisualTier(level);

		// Left side: ability info
		int infoX = panelX + 8;
		int infoY = panelY + 6;

		// Name + type
		graphics.drawString(this.font, name, infoX, infoY, 0xFFFFFFFF, true);
		graphics.drawString(this.font, " [" + typeName + "]",
				infoX + this.font.width(name), infoY, typeColor, true);

		// Level + stars + active
		String lvlStr = "Lv." + level;
		if (tier > 0) {
			lvlStr += " " + "\u2605".repeat(tier);
		}
		if (isActive) {
			lvlStr += " \u00A7aACTIVE";
		}
		graphics.drawString(this.font, lvlStr, infoX, infoY + 12, 0xFFCCCC00, true);

		// Key label
		graphics.drawString(this.font, "\u00A77Key: [" + KEY_LABELS[selectedSlot] + "]",
				infoX, infoY + 24, 0xFFAAAAAA, true);

		// Right side: buttons
		int btnAreaX = panelX + PANEL_WIDTH - 8;
		int btnY1 = panelY + 6;
		int btnY2 = panelY + 6 + BTN_H + 4;

		// Upgrade button (top-right)
		boolean canUpgrade = ClientLoadoutData.upgradePoints > 0 && level < 20;
		int ubX = btnAreaX - BTN_W;
		upgradeHovered = canUpgrade && mouseX >= ubX && mouseX <= ubX + BTN_W
				&& mouseY >= btnY1 && mouseY <= btnY1 + BTN_H;
		int ubColor = canUpgrade ? (upgradeHovered ? 0xFF44CC44 : 0xFF336633) : 0xFF333333;
		graphics.fill(ubX, btnY1, ubX + BTN_W, btnY1 + BTN_H, ubColor);
		graphics.drawCenteredString(this.font,
				canUpgrade ? "Upgrade" : "\u00A78Upgrade",
				ubX + BTN_W / 2, btnY1 + 4, canUpgrade ? 0xFFFFFFFF : 0xFF666666);

		// Swap button (next to upgrade)
		int sbX = ubX - BTN_W - 4;
		swapHovered = mouseX >= sbX && mouseX <= sbX + BTN_W
				&& mouseY >= btnY1 && mouseY <= btnY1 + BTN_H;
		int sbColor = swapHovered ? 0xFF884444 : 0xFF553333;
		graphics.fill(sbX, btnY1, sbX + BTN_W, btnY1 + BTN_H, sbColor);
		graphics.drawCenteredString(this.font, "Swap",
				sbX + BTN_W / 2, btnY1 + 4, 0xFFCCAAAA);

		// Branch button (below, only if level >= 10 and has branch)
		if (level >= 10 && hasBranch(abilityId)) {
			int emptySlot = findFirstEmptySlot();
			boolean canBranch = emptySlot >= 0;
			int bbX = btnAreaX - BTN_W;
			branchPanelHovered = canBranch && mouseX >= bbX && mouseX <= bbX + BTN_W
					&& mouseY >= btnY2 && mouseY <= btnY2 + BTN_H;
			int bbColor = canBranch ? (branchPanelHovered ? 0xFFAA44CC : 0xFF7733AA) : 0xFF333333;
			graphics.fill(bbX, btnY2, bbX + BTN_W, btnY2 + BTN_H, bbColor);

			long time = System.currentTimeMillis();
			float pulse = (float)(0.7 + 0.3 * Math.sin(time / 300.0));
			int alpha = (int)(255 * pulse);
			int textColor = canBranch ? ((alpha << 24) | 0x00FFDDFF) : 0xFF666666;
			graphics.drawCenteredString(this.font,
					canBranch ? "Branch!" : "\u00A78Full",
					bbX + BTN_W / 2, btnY2 + 4, textColor);
		}
	}

	// ── Line drawing ─────────────────────────────────────────────────

	/**
	 * Draw a dotted/segmented line between two points using 2px fills.
	 * Fits Minecraft's pixel aesthetic.
	 */
	private void drawDottedLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float length = (float) Math.sqrt(dx * dx + dy * dy);
		if (length < 1) return;

		float stepSize = 4f; // dot every 4 pixels
		int steps = (int)(length / stepSize);
		if (steps < 1) steps = 1;

		for (int i = 0; i <= steps; i++) {
			float t = (float) i / steps;
			int px = (int)(x1 + dx * t);
			int py = (int)(y1 + dy * t);
			graphics.fill(px - 1, py - 1, px + 1, py + 1, color);
		}
	}

	// ── Click handling ───────────────────────────────────────────────

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		if (event.button() == 0) {
			int visibleSlots = getVisibleSlotCount();

			// Check selection panel buttons first (if panel is visible)
			if (selectedSlot >= 0 && selectedSlot < visibleSlots && ClientLoadoutData.hasAbility(selectedSlot)) {
				// Upgrade
				if (upgradeHovered && ClientLoadoutData.upgradePoints > 0
						&& ClientLoadoutData.getUpgradeLevel(selectedSlot) < 20) {
					ClientPlayNetworking.send(new UpgradeAbilityPayload(selectedSlot));
					return true;
				}

				// Swap
				if (swapHovered) {
					minecraft.setScreen(new AbilityPickerScreen(selectedSlot, this));
					return true;
				}

				// Branch (from panel button)
				if (branchPanelHovered) {
					int emptySlot = findFirstEmptySlot();
					if (emptySlot >= 0) {
						ClientPlayNetworking.send(new BranchAbilityPayload(selectedSlot, emptySlot));
						return true;
					}
				}
			}

			// Check branch node clicks
			if (hoveredBranch >= 0) {
				int emptySlot = findFirstEmptySlot();
				if (emptySlot >= 0) {
					ClientPlayNetworking.send(new BranchAbilityPayload(hoveredBranch, emptySlot));
					return true;
				}
			}

			// Check node clicks (select/deselect)
			if (hoveredSlot >= 0 && hoveredSlot < visibleSlots) {
				// Empty slot — open ability picker directly
				if (!ClientLoadoutData.hasAbility(hoveredSlot)) {
					minecraft.setScreen(new AbilityPickerScreen(hoveredSlot, this));
					return true;
				}
				if (selectedSlot == hoveredSlot) {
					selectedSlot = -1; // deselect
				} else {
					selectedSlot = hoveredSlot; // select
				}
				return true;
			}

			// Clicked background — deselect
			selectedSlot = -1;
		}
		return super.mouseClicked(event, bl);
	}

	// ── Utility ──────────────────────────────────────────────────────

	private boolean isInsideNode(int mx, int my, int nx, int ny, int size) {
		return mx >= nx && mx <= nx + size && my >= ny && my <= ny + size;
	}

	// ── Helpers (public/static — used by other classes) ──────────────

	static int getVisibleSlotCount() {
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

	static String getUnlockMessage(int slot) {
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

	static int getVisualTier(int level) {
		if (level >= 15) return 3;
		if (level >= 10) return 2;
		if (level >= 5) return 1;
		return 0;
	}

	// IDs of abilities that are themselves branches (they don't branch further)
	static final java.util.Set<String> BRANCH_ABILITY_IDS = java.util.Set.of(
		"blackflame_inferno_form", "blackflame_meteor", "blackflame_scorched_earth",
		"endless_thousand_cuts", "endless_sword_storm", "endless_blade_barrier",
		"stellar_lightspeed", "stellar_nova", "stellar_constellation",
		"cloud_living_fortress", "cloud_thunderstrike", "cloud_vortex",
		"hollow_void_body", "hollow_nullify", "hollow_suppression_field"
	);

	static boolean hasBranch(String id) {
		// Branch abilities are defined at level 10 for all 15 base path abilities.
		// Branch abilities themselves, basic enforcement, and universals don't branch further.
		if (id == null) return false;
		if ("basic_enforcement".equals(id)) return false;
		if ("universal_madra_shield".equals(id) || "universal_spirit_pulse".equals(id)) return false;
		if (BRANCH_ABILITY_IDS.contains(id)) return false; // Branch abilities don't branch again
		return true; // All 15 base path abilities have branches
	}

	static int findFirstEmptySlot() {
		for (int i = 0; i < 6; i++) {
			if (!ClientLoadoutData.hasAbility(i)) return i;
		}
		return -1;
	}
}

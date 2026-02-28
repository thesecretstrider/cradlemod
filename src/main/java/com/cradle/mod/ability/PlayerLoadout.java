package com.cradle.mod.ability;

import com.cradle.mod.CradleMod;
import com.cradle.mod.CradlePlayerData;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks a player's equipped abilities, upgrade levels, and upgrade points.
 * 6 ability slots (0-5), each can hold one ability ID with an upgrade level.
 * Mastery map remembers levels for previously equipped abilities so they
 * aren't lost when swapping.
 */
public final class PlayerLoadout {

	public static final int MAX_SLOTS = 6;

	private final String[] equippedAbilities = new String[MAX_SLOTS];
	private final int[] upgradeLevels = new int[MAX_SLOTS];
	private final boolean[] slotActive = new boolean[MAX_SLOTS]; // Transient — not saved (enforcer/ruler toggle state)
	private int upgradePoints;

	/** Remembers the highest level reached for each ability ID, even when unequipped. */
	private final Map<String, Integer> masteryMap = new HashMap<>();

	public PlayerLoadout() {
		for (int i = 0; i < MAX_SLOTS; i++) {
			equippedAbilities[i] = null;
			upgradeLevels[i] = 0;
			slotActive[i] = false;
		}
		upgradePoints = 0;
	}

	// ── Slot accessors ────────────────────────────────────────────────

	public String getAbility(int slot) {
		if (slot < 0 || slot >= MAX_SLOTS) return null;
		return equippedAbilities[slot];
	}

	public int getUpgradeLevel(int slot) {
		if (slot < 0 || slot >= MAX_SLOTS) return 0;
		return upgradeLevels[slot];
	}

	public boolean isSlotActive(int slot) {
		if (slot < 0 || slot >= MAX_SLOTS) return false;
		return slotActive[slot];
	}

	public void setSlotActive(int slot, boolean active) {
		if (slot >= 0 && slot < MAX_SLOTS) {
			slotActive[slot] = active;
		}
	}

	public boolean hasAbility(int slot) {
		return slot >= 0 && slot < MAX_SLOTS && equippedAbilities[slot] != null;
	}

	/**
	 * Equip an ability to a slot. Saves the old ability's level to mastery,
	 * then restores the new ability's mastery level (or defaults to 1).
	 */
	public void equipAbility(int slot, String abilityId) {
		if (slot < 0 || slot >= MAX_SLOTS) return;
		saveMastery(slot);
		equippedAbilities[slot] = abilityId;
		upgradeLevels[slot] = masteryMap.getOrDefault(abilityId, 1);
		slotActive[slot] = false;
	}

	/**
	 * Set ability with a specific level (for migration/debug).
	 * Also updates the mastery map.
	 */
	public void equipAbility(int slot, String abilityId, int level) {
		if (slot < 0 || slot >= MAX_SLOTS) return;
		saveMastery(slot);
		int lvl = Math.max(1, level);
		equippedAbilities[slot] = abilityId;
		upgradeLevels[slot] = lvl;
		masteryMap.put(abilityId, lvl);
		slotActive[slot] = false;
	}

	/**
	 * Clear a slot (remove ability). Saves mastery first.
	 */
	public void clearSlot(int slot) {
		if (slot < 0 || slot >= MAX_SLOTS) return;
		saveMastery(slot);
		equippedAbilities[slot] = null;
		upgradeLevels[slot] = 0;
		slotActive[slot] = false;
	}

	/**
	 * Save the current ability's level in a slot to the mastery map
	 * (keeps the highest level reached).
	 */
	private void saveMastery(int slot) {
		String oldAbility = equippedAbilities[slot];
		int oldLevel = upgradeLevels[slot];
		if (oldAbility != null && oldLevel > 0) {
			int existing = masteryMap.getOrDefault(oldAbility, 0);
			if (oldLevel > existing) {
				masteryMap.put(oldAbility, oldLevel);
			}
		}
	}

	/**
	 * Get the remembered mastery level for an ability, or 0 if never used.
	 */
	public int getMasteryLevel(String abilityId) {
		if (abilityId == null) return 0;
		return masteryMap.getOrDefault(abilityId, 0);
	}

	/**
	 * Deactivate all active slots (on death, disconnect, etc.)
	 */
	public void deactivateAll() {
		for (int i = 0; i < MAX_SLOTS; i++) {
			slotActive[i] = false;
		}
	}

	/**
	 * Find which slot an ability ID is in, or -1 if not equipped.
	 */
	public int findSlot(String abilityId) {
		if (abilityId == null) return -1;
		for (int i = 0; i < MAX_SLOTS; i++) {
			if (abilityId.equals(equippedAbilities[i])) return i;
		}
		return -1;
	}

	/**
	 * Find the first empty slot, or -1 if all full.
	 */
	public int firstEmptySlot() {
		for (int i = 0; i < MAX_SLOTS; i++) {
			if (equippedAbilities[i] == null) return i;
		}
		return -1;
	}

	/**
	 * Count how many slots have abilities equipped.
	 */
	public int equippedCount() {
		int count = 0;
		for (String s : equippedAbilities) {
			if (s != null) count++;
		}
		return count;
	}

	// ── Upgrade system ────────────────────────────────────────────────

	public int getUpgradePoints() {
		return upgradePoints;
	}

	public void setUpgradePoints(int points) {
		this.upgradePoints = Math.max(0, points);
	}

	public void addUpgradePoints(int amount) {
		this.upgradePoints = Math.max(0, this.upgradePoints + amount);
	}

	/**
	 * Upgrade an ability in a slot. Costs 1 upgrade point.
	 * Returns true if successful.
	 */
	public boolean upgradeSlot(int slot) {
		if (slot < 0 || slot >= MAX_SLOTS) return false;
		if (equippedAbilities[slot] == null) return false;
		if (upgradePoints <= 0) return false;

		AbilityDefinition def = AbilityRegistry.get(equippedAbilities[slot]);
		if (def == null) return false;
		if (upgradeLevels[slot] >= def.getMaxUpgradeLevel()) return false;

		upgradeLevels[slot]++;
		upgradePoints--;
		// Keep mastery map in sync
		masteryMap.put(equippedAbilities[slot], upgradeLevels[slot]);
		return true;
	}

	/**
	 * Branch an ability: the source slot resets to level 1 with its original ability,
	 * and the branch ability is placed in the target slot at level 1.
	 * Returns true if successful.
	 */
	public boolean branchAbility(int sourceSlot, int targetSlot) {
		if (sourceSlot < 0 || sourceSlot >= MAX_SLOTS) return false;
		if (targetSlot < 0 || targetSlot >= MAX_SLOTS) return false;
		if (equippedAbilities[sourceSlot] == null) return false;
		if (equippedAbilities[targetSlot] != null) return false; // Target must be empty

		AbilityDefinition def = AbilityRegistry.get(equippedAbilities[sourceSlot]);
		if (def == null || !def.hasBranch()) return false;
		if (upgradeLevels[sourceSlot] < def.getBranchLevel()) return false;

		String branchId = def.getBranchAbilityId();
		AbilityDefinition branchDef = AbilityRegistry.get(branchId);
		if (branchDef == null) return false;

		// Save source mastery before resetting, then reset source to level 1
		saveMastery(sourceSlot);
		upgradeLevels[sourceSlot] = 1;
		masteryMap.put(equippedAbilities[sourceSlot], 1); // branching intentionally resets mastery
		slotActive[sourceSlot] = false;
		equippedAbilities[targetSlot] = branchId;
		upgradeLevels[targetSlot] = masteryMap.getOrDefault(branchId, 1);
		slotActive[targetSlot] = false;

		return true;
	}

	// ── NBT persistence ───────────────────────────────────────────────

	public CompoundTag toNbt() {
		CompoundTag tag = new CompoundTag();
		for (int i = 0; i < MAX_SLOTS; i++) {
			if (equippedAbilities[i] != null) {
				tag.putString("slot" + i, equippedAbilities[i]);
				tag.putInt("level" + i, upgradeLevels[i]);
			}
		}
		tag.putInt("upgradePoints", upgradePoints);

		// Save mastery map
		if (!masteryMap.isEmpty()) {
			CompoundTag masteryTag = new CompoundTag();
			for (Map.Entry<String, Integer> entry : masteryMap.entrySet()) {
				masteryTag.putInt(entry.getKey(), entry.getValue());
			}
			tag.put("mastery", masteryTag);
		}

		return tag;
	}

	public static PlayerLoadout fromNbt(CompoundTag tag) {
		PlayerLoadout loadout = new PlayerLoadout();
		for (int i = 0; i < MAX_SLOTS; i++) {
			String key = "slot" + i;
			if (tag.contains(key)) {
				String abilityId = tag.getStringOr(key, "");
				if (!abilityId.isEmpty()) {
					loadout.equippedAbilities[i] = abilityId;
					loadout.upgradeLevels[i] = tag.getIntOr("level" + i, 1);
				}
			}
		}
		loadout.upgradePoints = tag.getIntOr("upgradePoints", 0);

		// Load mastery map
		if (tag.contains("mastery")) {
			tag.getCompound("mastery").ifPresent(masteryTag -> {
				for (String key : masteryTag.keySet()) {
					loadout.masteryMap.put(key, masteryTag.getIntOr(key, 1));
				}
			});
		}

		// Ensure currently equipped abilities are also in the mastery map
		for (int i = 0; i < MAX_SLOTS; i++) {
			if (loadout.equippedAbilities[i] != null && loadout.upgradeLevels[i] > 0) {
				String id = loadout.equippedAbilities[i];
				int existing = loadout.masteryMap.getOrDefault(id, 0);
				if (loadout.upgradeLevels[i] > existing) {
					loadout.masteryMap.put(id, loadout.upgradeLevels[i]);
				}
			}
		}

		return loadout;
	}

	// ── Migration from old save format ────────────────────────────────

	/**
	 * Creates a loadout for a player migrating from the old system
	 * (no explicit loadout in their NBT data).
	 */
	public static PlayerLoadout createMigrationLoadout(CradlePlayerData data) {
		PlayerLoadout loadout = new PlayerLoadout();
		CradlePlayerData.Path path = data.getChosenPath();
		CradlePlayerData.AdvancementStage stage = data.getAdvancementStage();

		if (path == CradlePlayerData.Path.UNSET) {
			return loadout; // No path chosen yet, empty loadout
		}

		// Slot 0: Basic Enforcement (everyone gets this)
		loadout.equipAbility(0, "basic_enforcement");

		// Slot 1: Path's default striker (if Copper+)
		if (stage.ordinal() >= CradlePlayerData.AdvancementStage.COPPER.ordinal()) {
			AbilityDefinition striker = AbilityRegistry.getDefaultStriker(path);
			if (striker != null) {
				loadout.equipAbility(1, striker.getId());
			}
		}

		// Slot 2: Path's default enforcer (if Iron+) — maps from old Z=enforcer
		if (stage.ordinal() >= CradlePlayerData.AdvancementStage.IRON.ordinal()) {
			AbilityDefinition enforcer = AbilityRegistry.getDefaultEnforcer(path);
			if (enforcer != null) {
				loadout.equipAbility(2, enforcer.getId());
			}
		}

		// Slot 3: Path's default ruler (if Low Gold+) — maps from old C=ruler
		if (stage.ordinal() >= CradlePlayerData.AdvancementStage.LOW_GOLD.ordinal()) {
			AbilityDefinition ruler = AbilityRegistry.getDefaultRuler(path);
			if (ruler != null) {
				loadout.equipAbility(3, ruler.getId());
			}
		}

		// Grant retroactive upgrade points (1 per stage from Underlord+)
		int retroPoints = 0;
		if (stage.ordinal() >= CradlePlayerData.AdvancementStage.UNDERLORD.ordinal()) retroPoints++;
		if (stage.ordinal() >= CradlePlayerData.AdvancementStage.OVERLORD.ordinal()) retroPoints++;
		if (stage.ordinal() >= CradlePlayerData.AdvancementStage.ARCHLORD.ordinal()) retroPoints++;
		if (data.hasSage()) retroPoints++;
		if (data.hasHerald()) retroPoints++;
		if (stage.ordinal() >= CradlePlayerData.AdvancementStage.MONARCH.ordinal()) retroPoints++;
		loadout.setUpgradePoints(retroPoints);

		CradleMod.LOGGER.info("Migrated player loadout: {} abilities, {} upgrade points", loadout.equippedCount(), retroPoints);
		return loadout;
	}

	/**
	 * Encode loadout as a JSON string for network sync.
	 */
	public String toJsonString() {
		StringBuilder sb = new StringBuilder("{\"slots\":[");
		for (int i = 0; i < MAX_SLOTS; i++) {
			if (i > 0) sb.append(",");
			if (equippedAbilities[i] != null) {
				sb.append("{\"id\":\"").append(equippedAbilities[i])
					.append("\",\"lvl\":").append(upgradeLevels[i]).append("}");
			} else {
				sb.append("null");
			}
		}
		sb.append("],\"up\":").append(upgradePoints).append("}");
		return sb.toString();
	}

	/**
	 * Parse loadout from a JSON string (from network sync).
	 */
	public static PlayerLoadout fromJsonString(String json) {
		PlayerLoadout loadout = new PlayerLoadout();
		try {
			// Simple manual parsing — avoids needing Gson on client
			int slotsStart = json.indexOf("[") + 1;
			int slotsEnd = json.indexOf("]");
			String slotsStr = json.substring(slotsStart, slotsEnd);

			int slot = 0;
			int pos = 0;
			while (pos < slotsStr.length() && slot < MAX_SLOTS) {
				if (slotsStr.startsWith("null", pos)) {
					pos += 4;
					slot++;
				} else if (slotsStr.charAt(pos) == '{') {
					int idStart = slotsStr.indexOf("\"id\":\"", pos) + 6;
					int idEnd = slotsStr.indexOf("\"", idStart);
					String id = slotsStr.substring(idStart, idEnd);

					int lvlStart = slotsStr.indexOf("\"lvl\":", pos) + 6;
					int lvlEnd = slotsStr.indexOf("}", lvlStart);
					int lvl = Integer.parseInt(slotsStr.substring(lvlStart, lvlEnd).trim());

					loadout.equippedAbilities[slot] = id;
					loadout.upgradeLevels[slot] = lvl;
					pos = lvlEnd + 1;
					slot++;
				}
				// Skip commas
				if (pos < slotsStr.length() && slotsStr.charAt(pos) == ',') pos++;
			}

			// Parse upgrade points
			int upStart = json.indexOf("\"up\":") + 5;
			int upEnd = json.indexOf("}", upStart);
			loadout.upgradePoints = Integer.parseInt(json.substring(upStart, upEnd).trim());
		} catch (Exception e) {
			CradleMod.LOGGER.warn("Failed to parse loadout JSON: {}", json, e);
		}
		return loadout;
	}
}

package com.cradle.mod;

import com.cradle.mod.network.AbilityLoadoutSyncPayload;

/**
 * Client-side cache of the player's ability loadout, updated via
 * AbilityLoadoutSyncPayload from the server.
 *
 * All fields are static because there's only one local player on the client.
 * Stores the raw slot data so the HUD and skill tree screen can render it.
 */
public final class ClientLoadoutData {

	public static final int MAX_SLOTS = 6;

	// Slot ability IDs (null = empty slot)
	private static final String[] abilityIds = new String[MAX_SLOTS];
	// Slot upgrade levels (0 = empty/unused)
	private static final int[] upgradeLevels = new int[MAX_SLOTS];
	// Slot active state (toggle abilities like enforcer/ruler)
	private static final boolean[] slotActive = new boolean[MAX_SLOTS];
	// Available upgrade points
	public static int upgradePoints = 0;
	// Client-side cooldown tracking (visual only — server enforces real cooldowns)
	private static final long[] cooldownEndMs = new long[MAX_SLOTS];
	private static final long[] cooldownDurationMs = new long[MAX_SLOTS];

	// Charge state tracking for Striker charge-up HUD
	private static final boolean[] charging = new boolean[MAX_SLOTS];
	private static final int[] chargeTicks = new int[MAX_SLOTS];
	public static final int MAX_CHARGE_TICKS = 60;

	private ClientLoadoutData() {}

	/**
	 * Update from a loadout sync payload.
	 * Parses the JSON and active flags.
	 */
	public static void update(AbilityLoadoutSyncPayload payload) {
		// Parse active flags
		for (int i = 0; i < MAX_SLOTS; i++) {
			slotActive[i] = payload.isSlotActive(i);
		}

		// Parse loadout JSON
		String json = payload.loadoutJson();
		parseLoadoutJson(json);
	}

	/**
	 * Update just the active flags (from CradleSyncPayload flag bits 10-15).
	 * Called every tick so the HUD reflects toggle state in real-time.
	 */
	public static void updateActiveFlags(int flagBits) {
		for (int i = 0; i < MAX_SLOTS; i++) {
			slotActive[i] = (flagBits & (1 << i)) != 0;
		}
	}

	/**
	 * Reset all loadout data. Called on disconnect.
	 */
	public static void reset() {
		for (int i = 0; i < MAX_SLOTS; i++) {
			abilityIds[i] = null;
			upgradeLevels[i] = 0;
			slotActive[i] = false;
			charging[i] = false;
			chargeTicks[i] = 0;
		}
		upgradePoints = 0;
	}

	// ── Accessors ────────────────────────────────────────────────────

	public static String getAbilityId(int slot) {
		if (slot < 0 || slot >= MAX_SLOTS) return null;
		return abilityIds[slot];
	}

	public static int getUpgradeLevel(int slot) {
		if (slot < 0 || slot >= MAX_SLOTS) return 0;
		return upgradeLevels[slot];
	}

	public static boolean isSlotActive(int slot) {
		if (slot < 0 || slot >= MAX_SLOTS) return false;
		return slotActive[slot];
	}

	public static boolean hasAbility(int slot) {
		return slot >= 0 && slot < MAX_SLOTS && abilityIds[slot] != null;
	}

	/**
	 * Start a visual cooldown for a slot (called when a striker is fired).
	 * @param slot The slot that was activated
	 * @param durationMs The cooldown duration in milliseconds
	 */
	public static void startCooldown(int slot, long durationMs) {
		if (slot < 0 || slot >= MAX_SLOTS) return;
		cooldownEndMs[slot] = System.currentTimeMillis() + durationMs;
		cooldownDurationMs[slot] = durationMs;
	}

	/**
	 * Returns the cooldown progress (0.0 = ready, 1.0 = just started).
	 */
	public static float getCooldownProgress(int slot) {
		if (slot < 0 || slot >= MAX_SLOTS) return 0f;
		long now = System.currentTimeMillis();
		if (now >= cooldownEndMs[slot]) return 0f;
		long remaining = cooldownEndMs[slot] - now;
		if (cooldownDurationMs[slot] <= 0) return 0f;
		return Math.min(1.0f, (float) remaining / cooldownDurationMs[slot]);
	}

	/**
	 * Returns true if the slot is on cooldown.
	 */
	public static boolean isOnCooldown(int slot) {
		return getCooldownProgress(slot) > 0f;
	}

	// ── Charge tracking ─────────────────────────────────────────────

	public static void startCharging(int slot) {
		if (slot >= 0 && slot < MAX_SLOTS) {
			charging[slot] = true;
			chargeTicks[slot] = 0;
		}
	}

	public static void updateChargeTick(int slot, int ticks) {
		if (slot >= 0 && slot < MAX_SLOTS) {
			chargeTicks[slot] = ticks;
		}
	}

	public static void stopCharging(int slot) {
		if (slot >= 0 && slot < MAX_SLOTS) {
			charging[slot] = false;
			chargeTicks[slot] = 0;
		}
	}

	public static boolean isCharging(int slot) {
		return slot >= 0 && slot < MAX_SLOTS && charging[slot];
	}

	/**
	 * Returns charge progress (0.0 = no charge, 1.0 = fully charged).
	 */
	public static float getChargeProgress(int slot) {
		if (slot < 0 || slot >= MAX_SLOTS || !charging[slot]) return 0f;
		return Math.min(1.0f, chargeTicks[slot] / (float) MAX_CHARGE_TICKS);
	}

	public static int getEquippedCount() {
		int count = 0;
		for (String id : abilityIds) {
			if (id != null) count++;
		}
		return count;
	}

	// ── JSON parsing ─────────────────────────────────────────────────

	/**
	 * Parse the compact loadout JSON format.
	 * Format: {"slots":[{"id":"ability_id","lvl":1},null,...],"up":3}
	 */
	private static void parseLoadoutJson(String json) {
		try {
			// Clear current data
			for (int i = 0; i < MAX_SLOTS; i++) {
				abilityIds[i] = null;
				upgradeLevels[i] = 0;
			}

			int slotsStart = json.indexOf("[") + 1;
			int slotsEnd = json.indexOf("]");
			if (slotsStart <= 0 || slotsEnd < 0) return;
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
					abilityIds[slot] = slotsStr.substring(idStart, idEnd);

					int lvlStart = slotsStr.indexOf("\"lvl\":", pos) + 6;
					int lvlEnd = slotsStr.indexOf("}", lvlStart);
					upgradeLevels[slot] = Integer.parseInt(slotsStr.substring(lvlStart, lvlEnd).trim());

					pos = lvlEnd + 1;
					slot++;
				}
				// Skip commas
				if (pos < slotsStr.length() && slotsStr.charAt(pos) == ',') pos++;
			}

			// Parse upgrade points
			int upStart = json.indexOf("\"up\":") + 5;
			int upEnd = json.indexOf("}", upStart);
			if (upStart > 4 && upEnd > upStart) {
				upgradePoints = Integer.parseInt(json.substring(upStart, upEnd).trim());
			}
		} catch (Exception e) {
			// Silently fail — client will get corrected on next sync
		}
	}
}

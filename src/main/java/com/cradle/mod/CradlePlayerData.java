package com.cradle.mod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CradlePlayerData {

	// ── Enums ──────────────────────────────────────────────────────────

	public enum Path {
		UNSET,
		BLACK_FLAME,
		ENDLESS_SWORD,
		STELLAR_SPEAR,
		CLOUD_HAMMER,
		HOLLOW_KING;

		public String displayName() {
			return switch (this) {
				case UNSET -> "None";
				case BLACK_FLAME -> "Path of Black Flame";
				case ENDLESS_SWORD -> "Path of the Endless Sword";
				case STELLAR_SPEAR -> "Path of the Stellar Spear";
				case CLOUD_HAMMER -> "Path of the Cloud Hammer";
				case HOLLOW_KING -> "Path of the Hollow King";
			};
		}
	}

	public enum IronBody {
		NONE,
		BLOODFORGED,
		STEELBORN,
		RAINDROP;

		public String displayName() {
			return switch (this) {
				case NONE -> "None";
				case BLOODFORGED -> "Bloodforged";
				case STEELBORN -> "Steelborn";
				case RAINDROP -> "Raindrop";
			};
		}

		public int color() {
			return switch (this) {
				case NONE -> 0xFFC0C0C0;
				case BLOODFORGED -> 0xFFCC3333;
				case STEELBORN -> 0xFF8888AA;
				case RAINDROP -> 0xFF3399FF;
			};
		}
	}

	public enum AdvancementStage {
		FOUNDATION,
		COPPER,
		IRON,
		JADE,
		LOW_GOLD,
		HIGH_GOLD,
		TRUEGOLD,
		UNDERLORD,
		OVERLORD,
		ARCHLORD,
		SAGE,
		HERALD,
		MONARCH;

		public String displayName() {
			return switch (this) {
				case FOUNDATION -> "Foundation";
				case COPPER -> "Copper";
				case IRON -> "Iron";
				case JADE -> "Jade";
				case LOW_GOLD -> "Low Gold";
				case HIGH_GOLD -> "High Gold";
				case TRUEGOLD -> "Truegold";
				case UNDERLORD -> "Underlord";
				case OVERLORD -> "Overlord";
				case ARCHLORD -> "Archlord";
				case SAGE -> "Sage";
				case HERALD -> "Herald";
				case MONARCH -> "Monarch";
			};
		}

		public AdvancementStage next() {
			AdvancementStage[] stages = values();
			int nextIndex = ordinal() + 1;
			if (nextIndex >= stages.length) {
				return this;
			}
			return stages[nextIndex];
		}

		public boolean hasNext() {
			return ordinal() < values().length - 1;
		}
	}

	// ── Static player registry ─────────────────────────────────────────

	private static final ConcurrentMap<UUID, CradlePlayerData> BY_PLAYER_ID = new ConcurrentHashMap<>();

	public static CradlePlayerData get(UUID playerId) {
		return BY_PLAYER_ID.get(Objects.requireNonNull(playerId, "playerId"));
	}

	public static CradlePlayerData getOrCreate(UUID playerId) {
		Objects.requireNonNull(playerId, "playerId");
		return BY_PLAYER_ID.computeIfAbsent(playerId, ignored -> new CradlePlayerData());
	}

	public static void remove(UUID playerId) {
		BY_PLAYER_ID.remove(Objects.requireNonNull(playerId, "playerId"));
	}

	public static void clearAll() {
		BY_PLAYER_ID.clear();
	}

	public static ConcurrentMap<UUID, CradlePlayerData> getAll() {
		return BY_PLAYER_ID;
	}

	// ── Instance fields ────────────────────────────────────────────────

	private int playerLevel;
	private int cyclingXp;
	private AdvancementStage advancementStage;
	private Path chosenPath;
	private boolean hasChosenPath;
	private float currentMadra;
	private float maxMadra;
	private boolean activelyCycling;
	private IronBody ironBody;
	private boolean ironBodyActive;
	private boolean enforcerActive;
	private boolean rulerActive;
	private boolean hasSage;
	private boolean hasHerald;
	private boolean swordCycling; // True when cycling with sword stabbed into soft block (Endless Sword / Stellar Spear)
	private boolean underlordFlying; // Transient — true when flight is enabled (Underlord+ / Cloud Hammer Copper+)
	private float currentWillpower;
	private float maxWillpower;

	private static final float DEFAULT_MAX_MADRA = 100.0f;
	private static final float DEFAULT_MAX_WILLPOWER = 50.0f;

	public CradlePlayerData() {
		this.playerLevel = 0;
		this.cyclingXp = 0;
		this.advancementStage = AdvancementStage.FOUNDATION;
		this.chosenPath = Path.UNSET;
		this.hasChosenPath = false;
		this.currentMadra = 0.0f;
		this.maxMadra = DEFAULT_MAX_MADRA;
		this.activelyCycling = false;
		this.ironBody = IronBody.NONE;
		this.ironBodyActive = false;
		this.enforcerActive = false;
		this.rulerActive = false;
		this.hasSage = false;
		this.hasHerald = false;
		this.swordCycling = false;
		this.underlordFlying = false;
		this.currentWillpower = 0.0f;
		this.maxWillpower = DEFAULT_MAX_WILLPOWER;
	}

	// ── Getters / setters ──────────────────────────────────────────────

	public int getPlayerLevel() {
		return playerLevel;
	}

	public void setPlayerLevel(int playerLevel) {
		this.playerLevel = playerLevel;
	}

	public int getCyclingXp() {
		return cyclingXp;
	}

	public void setCyclingXp(int cyclingXp) {
		this.cyclingXp = cyclingXp;
	}

	public AdvancementStage getAdvancementStage() {
		return advancementStage;
	}

	public void setAdvancementStage(AdvancementStage advancementStage) {
		this.advancementStage = Objects.requireNonNull(advancementStage, "advancementStage");
	}

	public Path getChosenPath() {
		return chosenPath;
	}

	public void setChosenPath(Path chosenPath) {
		this.chosenPath = Objects.requireNonNull(chosenPath, "chosenPath");
		this.hasChosenPath = chosenPath != Path.UNSET;
	}

	public boolean hasChosenPath() {
		return hasChosenPath;
	}

	public float getCurrentMadra() {
		return currentMadra;
	}

	public void setCurrentMadra(float currentMadra) {
		this.currentMadra = Math.max(0, Math.min(currentMadra, maxMadra));
	}

	public float getMaxMadra() {
		return maxMadra;
	}

	public void setMaxMadra(float maxMadra) {
		this.maxMadra = Math.max(0, maxMadra);
		if (this.currentMadra > this.maxMadra) {
			this.currentMadra = this.maxMadra;
		}
	}

	public boolean isActivelyCycling() {
		return activelyCycling;
	}

	public void setActivelyCycling(boolean activelyCycling) {
		this.activelyCycling = activelyCycling;
	}

	public IronBody getIronBody() {
		return ironBody;
	}

	public void setIronBody(IronBody ironBody) {
		this.ironBody = Objects.requireNonNull(ironBody, "ironBody");
	}

	public boolean isIronBodyActive() {
		return ironBodyActive;
	}

	public void setIronBodyActive(boolean ironBodyActive) {
		this.ironBodyActive = ironBodyActive && ironBody != IronBody.NONE;
	}

	public boolean isEnforcerActive() {
		return enforcerActive;
	}

	public void setEnforcerActive(boolean enforcerActive) {
		this.enforcerActive = enforcerActive;
	}

	public boolean isRulerActive() {
		return rulerActive;
	}

	public void setRulerActive(boolean rulerActive) {
		this.rulerActive = rulerActive;
	}

	public boolean hasSage() {
		return hasSage;
	}

	public void setHasSage(boolean hasSage) {
		this.hasSage = hasSage;
	}

	public boolean hasHerald() {
		return hasHerald;
	}

	public void setHasHerald(boolean hasHerald) {
		this.hasHerald = hasHerald;
	}

	public boolean isSwordCycling() {
		return swordCycling;
	}

	public void setSwordCycling(boolean swordCycling) {
		this.swordCycling = swordCycling;
	}

	public boolean isUnderlordFlying() {
		return underlordFlying;
	}

	public void setUnderlordFlying(boolean underlordFlying) {
		this.underlordFlying = underlordFlying;
	}

	public float getCurrentWillpower() {
		return currentWillpower;
	}

	public void setCurrentWillpower(float currentWillpower) {
		this.currentWillpower = Math.max(0, Math.min(currentWillpower, maxWillpower));
	}

	public float getMaxWillpower() {
		return maxWillpower;
	}

	public void setMaxWillpower(float maxWillpower) {
		this.maxWillpower = Math.max(0, maxWillpower);
		if (this.currentWillpower > this.maxWillpower) {
			this.currentWillpower = this.maxWillpower;
		}
	}

	/**
	 * Returns true if this player has unlocked willpower (Archlord+).
	 * Willpower is the resource used for Sage Authority and Herald powers.
	 */
	public boolean hasWillpower() {
		return advancementStage.ordinal() >= AdvancementStage.ARCHLORD.ordinal();
	}

	/**
	 * Returns true if this path benefits from sword-stabbing cycling
	 * (right-click sword into soft block for 2x cycling speed).
	 * Only sword-based paths benefit: Endless Sword and Stellar Spear.
	 */
	public boolean isSwordPath() {
		return chosenPath == Path.ENDLESS_SWORD || chosenPath == Path.STELLAR_SPEAR;
	}

	/**
	 * Returns true if this player's stage is high enough for flight.
	 * Cloud Hammer (wind/storm path) unlocks flight at Copper.
	 * All other paths unlock flight at Underlord.
	 */
	public boolean canFly() {
		if (chosenPath == Path.CLOUD_HAMMER) {
			return advancementStage.ordinal() >= AdvancementStage.COPPER.ordinal();
		}
		return advancementStage.ordinal() >= AdvancementStage.UNDERLORD.ordinal();
	}

	/**
	 * Returns the madra drain per tick while flying.
	 * Cloud Hammer drains less (wind is their element).
	 * Scaled by the stage-based cost multiplier.
	 */
	public float getFlightMadraDrain() {
		float base = (chosenPath == Path.CLOUD_HAMMER) ? 0.5f : 0.8f;
		return base * getMadraCostMultiplier();
	}

	/**
	 * Returns the cycling speed multiplier for the current advancement stage.
	 * Higher stages cycle faster (gain more XP and Madra per tick).
	 */
	public float getCyclingSpeedMultiplier() {
		return switch (advancementStage) {
			case FOUNDATION -> 1.0f;
			case COPPER -> 1.5f;
			case IRON -> 2.0f;
			case JADE -> 2.5f;
			case LOW_GOLD -> 3.0f;
			case HIGH_GOLD -> 3.5f;
			case TRUEGOLD -> 4.0f;
			case UNDERLORD -> 5.5f;
			case OVERLORD -> 7.0f;
			case ARCHLORD -> 9.0f;
			case SAGE, HERALD -> 12.0f;
			case MONARCH -> 16.0f;
		};
	}

	/**
	 * Returns the ability power multiplier for the current advancement stage.
	 * Higher stages deal more damage with techniques. Gold stages are modest —
	 * the big jump is at Underlord (Lord realm).
	 */
	public float getAbilityPowerMultiplier() {
		return switch (advancementStage) {
			case FOUNDATION, COPPER, IRON -> 1.0f;
			case JADE -> 1.05f;
			case LOW_GOLD -> 1.1f;
			case HIGH_GOLD -> 1.15f;
			case TRUEGOLD -> 1.25f;
			case UNDERLORD -> 1.5f;
			case OVERLORD -> 1.8f;
			case ARCHLORD -> 2.0f;
			case SAGE, HERALD -> 2.2f;
			case MONARCH -> 2.5f;
		};
	}

	/**
	 * Returns the Madra cost multiplier for the current advancement stage.
	 * Higher stages spend less Madra per ability use. Gold stages barely
	 * reduce cost — real discounts start at Underlord.
	 */
	public float getMadraCostMultiplier() {
		return switch (advancementStage) {
			case FOUNDATION, COPPER, IRON, JADE -> 1.0f;
			case LOW_GOLD -> 0.95f;
			case HIGH_GOLD -> 0.9f;
			case TRUEGOLD -> 0.85f;
			case UNDERLORD -> 0.75f;
			case OVERLORD -> 0.65f;
			case ARCHLORD -> 0.55f;
			case SAGE, HERALD -> 0.5f;
			case MONARCH -> 0.4f;
		};
	}

	/**
	 * Returns the ability cooldown multiplier for the current advancement stage.
	 * Higher stages have shorter cooldowns. Mirrors the cost curve but goes
	 * lower at the top end — cooldown reduction is very impactful.
	 */
	public float getCooldownMultiplier() {
		return switch (advancementStage) {
			case FOUNDATION -> 1.0f;
			case COPPER -> 0.95f;
			case IRON -> 0.9f;
			case JADE -> 0.85f;
			case LOW_GOLD -> 0.8f;
			case HIGH_GOLD -> 0.75f;
			case TRUEGOLD -> 0.7f;
			case UNDERLORD -> 0.6f;
			case OVERLORD -> 0.5f;
			case ARCHLORD -> 0.45f;
			case SAGE, HERALD -> 0.4f;
			case MONARCH -> 0.3f;
		};
	}

	// ── NBT persistence ────────────────────────────────────────────────

	public CompoundTag toNbt() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("playerLevel", playerLevel);
		tag.putInt("cyclingXp", cyclingXp);
		tag.putString("advancementStage", advancementStage.name());
		tag.putString("chosenPath", chosenPath.name());
		tag.putBoolean("hasChosenPath", hasChosenPath);
		tag.putFloat("currentMadra", currentMadra);
		tag.putFloat("maxMadra", maxMadra);
		tag.putString("ironBody", ironBody.name());
		tag.putBoolean("hasSage", hasSage);
		tag.putBoolean("hasHerald", hasHerald);
		tag.putFloat("currentWillpower", currentWillpower);
		tag.putFloat("maxWillpower", maxWillpower);
		return tag;
	}

	public static CradlePlayerData fromNbt(CompoundTag tag) {
		CradlePlayerData data = new CradlePlayerData();
		data.playerLevel = tag.getIntOr("playerLevel", 0);
		data.cyclingXp = tag.getIntOr("cyclingXp", 0);

		try {
			String stageName = tag.getStringOr("advancementStage", "FOUNDATION");
			// Migration: old "GOLD" saves become "LOW_GOLD"
			if ("GOLD".equals(stageName)) {
				stageName = "LOW_GOLD";
			}
			data.advancementStage = AdvancementStage.valueOf(stageName);
		} catch (IllegalArgumentException e) {
			data.advancementStage = AdvancementStage.FOUNDATION;
		}

		try {
			data.chosenPath = Path.valueOf(tag.getStringOr("chosenPath", "UNSET"));
		} catch (IllegalArgumentException e) {
			data.chosenPath = Path.UNSET;
		}

		data.hasChosenPath = tag.getBooleanOr("hasChosenPath", false);
		data.currentMadra = tag.getFloatOr("currentMadra", 0.0f);
		data.maxMadra = tag.getFloatOr("maxMadra", DEFAULT_MAX_MADRA);
		if (data.maxMadra <= 0) {
			data.maxMadra = DEFAULT_MAX_MADRA;
		}

		try {
			data.ironBody = IronBody.valueOf(tag.getStringOr("ironBody", "NONE"));
		} catch (IllegalArgumentException e) {
			data.ironBody = IronBody.NONE;
		}

		data.hasSage = tag.getBooleanOr("hasSage", false);
		data.hasHerald = tag.getBooleanOr("hasHerald", false);

		data.currentWillpower = tag.getFloatOr("currentWillpower", 0.0f);
		data.maxWillpower = tag.getFloatOr("maxWillpower", DEFAULT_MAX_WILLPOWER);
		if (data.maxWillpower <= 0) {
			data.maxWillpower = DEFAULT_MAX_WILLPOWER;
		}

		return data;
	}

	/**
	 * Saves all player data into a single CompoundTag (for world save).
	 */
	public static CompoundTag saveAll() {
		CompoundTag root = new CompoundTag();
		ListTag playerList = new ListTag();

		for (var entry : BY_PLAYER_ID.entrySet()) {
			CompoundTag playerTag = entry.getValue().toNbt();
			playerTag.putString("uuid", entry.getKey().toString());
			playerList.add(playerTag);
		}

		root.put("players", playerList);
		return root;
	}

	/**
	 * Loads all player data from a CompoundTag (on world load).
	 * Clears existing data first.
	 */
	public static void loadAll(CompoundTag root) {
		BY_PLAYER_ID.clear();

		if (!root.contains("players")) {
			return;
		}

		ListTag playerList = root.getListOrEmpty("players");
		for (int i = 0; i < playerList.size(); i++) {
			CompoundTag playerTag = playerList.getCompoundOrEmpty(i);
			String uuidString = playerTag.getStringOr("uuid", "");

			if (uuidString.isEmpty()) {
				continue;
			}

			try {
				UUID uuid = UUID.fromString(uuidString);
				CradlePlayerData data = fromNbt(playerTag);
				BY_PLAYER_ID.put(uuid, data);
			} catch (IllegalArgumentException e) {
				CradleMod.LOGGER.warn("Skipping player data with invalid UUID: {}", uuidString);
			}
		}
	}
}

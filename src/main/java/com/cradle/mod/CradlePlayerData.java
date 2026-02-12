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
		GOLD;

		public String displayName() {
			return name().charAt(0) + name().substring(1).toLowerCase();
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

	private static final float DEFAULT_MAX_MADRA = 100.0f;

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

	/**
	 * Returns the cycling speed multiplier for the current advancement stage.
	 * Higher stages cycle faster (gain more XP and Madra per tick).
	 */
	public float getCyclingSpeedMultiplier() {
		return switch (advancementStage) {
			case FOUNDATION -> 1.0f;
			case COPPER -> 1.5f;
			case IRON -> 2.0f;
			case JADE -> 3.0f;
			case GOLD -> 5.0f;
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
		return tag;
	}

	public static CradlePlayerData fromNbt(CompoundTag tag) {
		CradlePlayerData data = new CradlePlayerData();
		data.playerLevel = tag.getIntOr("playerLevel", 0);
		data.cyclingXp = tag.getIntOr("cyclingXp", 0);

		try {
			data.advancementStage = AdvancementStage.valueOf(tag.getStringOr("advancementStage", "FOUNDATION"));
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

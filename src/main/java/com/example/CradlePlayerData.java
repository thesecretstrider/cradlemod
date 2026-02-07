package com.example;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CradlePlayerData {
	public enum Path {
		UNSET
	}

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

	private int playerLevel;
	private int cyclingXp;
	private int advancementStage;
	private Path chosenPath;
	private boolean hasChosenPath;

	public CradlePlayerData() {
		this.playerLevel = 0;
		this.cyclingXp = 0;
		this.advancementStage = 0;
		this.chosenPath = Path.UNSET;
		this.hasChosenPath = false;
	}

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

	public int getAdvancementStage() {
		return advancementStage;
	}

	public void setAdvancementStage(int advancementStage) {
		this.advancementStage = advancementStage;
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

	public void setHasChosenPath(boolean hasChosenPath) {
		if (!hasChosenPath) {
			this.hasChosenPath = false;
			this.chosenPath = Path.UNSET;
			return;
		}

		this.hasChosenPath = this.chosenPath != Path.UNSET;
	}
}


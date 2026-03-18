package com.cradle.mod.story;

import net.minecraft.nbt.CompoundTag;

public class GameModeManager {
	public enum CradleGameMode { FREE, CRADLE }
	public enum PlayerCharacter { NONE, LINDON, YERIN }

	private static CradleGameMode currentMode = CradleGameMode.FREE;

	public static CradleGameMode getMode() { return currentMode; }
	public static boolean isCradleMode() { return currentMode == CradleGameMode.CRADLE; }
	public static boolean isFreeMode() { return currentMode == CradleGameMode.FREE; }
	public static void setMode(CradleGameMode mode) { currentMode = mode; }

	public static CompoundTag writeNbt() {
		CompoundTag tag = new CompoundTag();
		tag.putString("gameMode", currentMode.name());
		return tag;
	}

	public static void readNbt(CompoundTag tag) {
		try {
			currentMode = CradleGameMode.valueOf(tag.getStringOr("gameMode", "FREE"));
		} catch (IllegalArgumentException e) {
			currentMode = CradleGameMode.FREE;
		}
	}

	public static void reset() {
		currentMode = CradleGameMode.FREE;
	}
}

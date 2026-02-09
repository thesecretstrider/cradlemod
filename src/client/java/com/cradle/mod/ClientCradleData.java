package com.cradle.mod;

import com.cradle.mod.network.CradleSyncPayload;

/**
 * Client-side cache of the player's Cradle data, updated each tick
 * via the CradleSyncPayload packet from the server.
 *
 * All fields are static because there's only one local player on the client.
 */
public final class ClientCradleData {

	public static int level = 0;
	public static int cyclingXp = 0;
	public static int xpToNext = 1000;
	public static String path = "UNSET";
	public static String stage = "FOUNDATION";
	public static float currentMadra = 0f;
	public static float maxMadra = 100f;
	public static boolean cycling = false;

	public static void update(CradleSyncPayload payload) {
		level = payload.level();
		cyclingXp = payload.cyclingXp();
		xpToNext = payload.xpToNext();
		path = payload.path();
		stage = payload.stage();
		currentMadra = payload.currentMadra();
		maxMadra = payload.maxMadra();
		cycling = payload.cycling();
	}

	/**
	 * Returns the ARGB color for the current advancement stage.
	 * Used by the Madra bar and info screen.
	 */
	public static int getStageColor() {
		return switch (stage) {
			case "COPPER" -> 0xFFB87333;
			case "IRON" -> 0xFF71797E;
			case "JADE" -> 0xFF00A86B;
			case "GOLD" -> 0xFFFFD700;
			default -> 0xFFC0C0C0; // Foundation / unknown = light grey
		};
	}

	/**
	 * Returns a human-readable display name for the current stage.
	 */
	public static String getStageDisplayName() {
		return switch (stage) {
			case "FOUNDATION" -> "Foundation";
			case "COPPER" -> "Copper";
			case "IRON" -> "Iron";
			case "JADE" -> "Jade";
			case "GOLD" -> "Gold";
			default -> stage;
		};
	}

	/**
	 * Returns a human-readable display name for the current path.
	 */
	public static String getPathDisplayName() {
		return switch (path) {
			case "UNSET" -> "None";
			case "BLACK_FLAME" -> "Path of Black Flame";
			case "ENDLESS_SWORD" -> "Path of the Endless Sword";
			case "STELLAR_SPEAR" -> "Path of the Stellar Spear";
			case "CLOUD_HAMMER" -> "Path of the Cloud Hammer";
			case "HOLLOW_KING" -> "Path of the Hollow King";
			default -> path;
		};
	}
}

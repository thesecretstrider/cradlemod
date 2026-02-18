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
	public static boolean canAdvance = false;
	public static String ironBody = "NONE";
	public static boolean ironBodyActive = false;
	public static boolean enforcerActive = false;
	public static boolean rulerActive = false;
	public static boolean hasSage = false;
	public static boolean hasHerald = false;

	/**
	 * Reset all client data to defaults. Called when disconnecting from a world
	 * so that stale data doesn't carry over to the next world.
	 */
	public static void reset() {
		level = 0;
		cyclingXp = 0;
		xpToNext = 1000;
		path = "UNSET";
		stage = "FOUNDATION";
		currentMadra = 0f;
		maxMadra = 100f;
		cycling = false;
		canAdvance = false;
		ironBody = "NONE";
		ironBodyActive = false;
		enforcerActive = false;
		rulerActive = false;
		hasSage = false;
		hasHerald = false;
	}

	public static void update(CradleSyncPayload payload) {
		level = payload.level();
		cyclingXp = payload.cyclingXp();
		xpToNext = payload.xpToNext();
		path = payload.path();
		stage = payload.stage();
		currentMadra = payload.currentMadra();
		maxMadra = payload.maxMadra();
		cycling = payload.cycling();
		canAdvance = payload.canAdvance();
		ironBody = payload.ironBody();
		ironBodyActive = payload.ironBodyActive();
		enforcerActive = payload.enforcerActive();
		rulerActive = payload.rulerActive();
		hasSage = payload.hasSage();
		hasHerald = payload.hasHerald();
	}

	/**
	 * Returns true if the player has chosen a path (not UNSET).
	 */
	public static boolean hasChosenPath() {
		return !"UNSET".equals(path);
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
			case "LOW_GOLD" -> 0xFFCCAA00;
			case "HIGH_GOLD" -> 0xFFFFD700;
			case "TRUEGOLD" -> 0xFFFFE866;
			case "UNDERLORD" -> 0xFF6A0DAD;
			case "OVERLORD" -> 0xFFAA33FF;
			case "ARCHLORD" -> 0xFFFF4500;
			case "SAGE" -> 0xFF00B3B3;
			case "HERALD" -> 0xFFCC1166;
			case "MONARCH" -> 0xFFFFFFFF;
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
			case "LOW_GOLD" -> "Low Gold";
			case "HIGH_GOLD" -> "High Gold";
			case "TRUEGOLD" -> "Truegold";
			case "UNDERLORD" -> "Underlord";
			case "OVERLORD" -> "Overlord";
			case "ARCHLORD" -> "Archlord";
			case "SAGE" -> "Sage";
			case "HERALD" -> "Herald";
			case "MONARCH" -> "Monarch";
			default -> stage;
		};
	}

	/**
	 * Returns the ARGB color for the current path.
	 * Used by cycling particles (path-based, not stage-based).
	 */
	public static int getPathParticleColor() {
		return switch (path) {
			case "BLACK_FLAME" -> 0xFF8B0000;      // dark red embers
			case "ENDLESS_SWORD" -> 0xFFCCCCCC;    // silver/white
			case "STELLAR_SPEAR" -> 0xFFFFDD44;    // bright gold
			case "CLOUD_HAMMER" -> 0xFF444455;      // dark grey/storm
			case "HOLLOW_KING" -> 0xFFDDDDEE;       // pale white
			default -> 0xFFC0C0C0;                  // light grey (unset)
		};
	}

	/**
	 * Returns the level needed for the next breakthrough.
	 * Returns -1 if at max stage (Monarch).
	 * Returns -2 if the player needs to choose Sage or Herald (at Archlord with level met).
	 */
	public static int getNextBreakthroughLevel() {
		return switch (stage) {
			case "FOUNDATION" -> 10;
			case "COPPER" -> 25;
			case "IRON" -> 50;
			case "JADE" -> 100;
			case "LOW_GOLD" -> 130;
			case "HIGH_GOLD" -> 165;
			case "TRUEGOLD" -> 200;
			case "UNDERLORD" -> 250;
			case "OVERLORD" -> 300;
			case "ARCHLORD" -> {
				// At Archlord, player must choose Sage or Herald
				// -2 signals the UI to show choice buttons instead of advance
				if (!hasSage && !hasHerald) yield -2;
				// If they've already picked one (shouldn't normally be at Archlord still), show 350
				else yield 350;
			}
			case "SAGE" -> hasHerald ? 400 : 350; // needs Herald (350) or Monarch (400)
			case "HERALD" -> hasSage ? 400 : 350;  // needs Sage (350) or Monarch (400)
			case "MONARCH" -> -1;
			default -> -1;
		};
	}

	/**
	 * Returns a human-readable display name for the current Iron Body.
	 */
	public static String getIronBodyDisplayName() {
		return switch (ironBody) {
			case "BLOODFORGED" -> "Bloodforged";
			case "STEELBORN" -> "Steelborn";
			case "RAINDROP" -> "Raindrop";
			default -> "None";
		};
	}

	/**
	 * Returns the ARGB color for the current Iron Body.
	 */
	public static int getIronBodyColor() {
		return switch (ironBody) {
			case "BLOODFORGED" -> 0xFFCC3333;
			case "STEELBORN" -> 0xFF8888AA;
			case "RAINDROP" -> 0xFF3399FF;
			default -> 0xFF999999;
		};
	}

	/**
	 * Returns the name of the Enforcer technique for the current path.
	 */
	public static String getEnforcerTechniqueName() {
		return switch (path) {
			case "BLACK_FLAME" -> "Burning Body";
			case "ENDLESS_SWORD" -> "Flowing Edge";
			case "STELLAR_SPEAR" -> "Stellar Alignment";
			case "CLOUD_HAMMER" -> "Thunderous Weight";
			case "HOLLOW_KING" -> "Hollow Circulation";
			default -> "None";
		};
	}

	/**
	 * Returns the name of the Ruler technique for the current path.
	 */
	public static String getRulerTechniqueName() {
		return switch (path) {
			case "BLACK_FLAME" -> "Domain of Ash";
			case "ENDLESS_SWORD" -> "Field of Blades";
			case "STELLAR_SPEAR" -> "Spear Domain";
			case "CLOUD_HAMMER" -> "Gravity Field";
			case "HOLLOW_KING" -> "Hollow Domain";
			default -> "None";
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

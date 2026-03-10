package com.cradle.mod;

import com.cradle.mod.network.CradleSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.UUID;

/**
 * Client-side cache of the player's Cradle data, updated each tick
 * via the CradleSyncPayload packet from the server.
 *
 * All fields are static because there's only one local player on the client.
 * Remote player goldsigns are tracked in a separate UUID map for multiplayer rendering.
 */
public final class ClientCradleData {

	/** Goldsign ordinals for remote players, keyed by their UUID. */
	private static final HashMap<UUID, Integer> remotePlayerGoldsigns = new HashMap<>();

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
	public static boolean flying = false;
	public static boolean spiritShiftActive = false;
	public static float currentWillpower = 0f;
	public static float maxWillpower = 50f;
	public static String icon = "NONE";
	public static boolean copperSightActive = false;
	public static int goldsignOrdinal = 0; // 0=NONE, 1=BLACK_FLAME_EYES, etc.

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
		flying = false;
		spiritShiftActive = false;
		currentWillpower = 0f;
		maxWillpower = 50f;
		icon = "NONE";
		copperSightActive = false;
		goldsignOrdinal = 0;
		clearRemoteGoldsigns();
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
		flying = payload.flying();
		spiritShiftActive = payload.spiritShiftActive();
		currentWillpower = payload.currentWillpower();
		maxWillpower = payload.maxWillpower();
		icon = payload.icon();
		copperSightActive = payload.copperSightActive();
		goldsignOrdinal = payload.goldsignOrdinal();
	}

	/**
	 * Returns true if the player has chosen a path (not UNSET).
	 */
	public static boolean hasChosenPath() {
		return !"UNSET".equals(path);
	}

	/**
	 * Returns true if the player has unlocked willpower (Archlord+).
	 */
	public static boolean hasWillpower() {
		return switch (stage) {
			case "ARCHLORD", "SAGE", "HERALD", "MONARCH" -> true;
			default -> false;
		};
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
			case "JADE" -> 120; // Natural accumulation path (Remnant absorption available at 100)
			case "LOW_GOLD" -> 130;
			case "HIGH_GOLD" -> 165;
			case "TRUEGOLD" -> 200;
			case "UNDERLORD" -> 250;
			case "OVERLORD" -> 300;
			case "ARCHLORD" -> {
				// At Archlord, player must choose Sage or Herald
				// -2 signals the UI to show choice buttons instead of advance
				if (!hasSage && !hasHerald) yield -2;
				// After choosing one, next is Monarch (400)
				else yield 400;
			}
			case "SAGE" -> 400;   // Sage -> Monarch (fight Remnant)
			case "HERALD" -> 400; // Herald -> Monarch (touch Icon)
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

	/**
	 * Returns a human-readable display name for the current Icon.
	 */
	public static String getIconDisplayName() {
		return switch (icon) {
			case "DRAGON" -> "Dragon";
			case "STRENGTH" -> "Strength";
			case "SWORD" -> "Sword";
			case "DEATH" -> "Death";
			case "SPEAR" -> "Spear";
			case "HAMMER" -> "Hammer";
			case "STORM" -> "Storm";
			case "VOID" -> "Void";
			case "CROWN" -> "Crown";
			case "HEART" -> "Heart";
			case "SHIELD" -> "Shield";
			default -> "None";
		};
	}

	/**
	 * Returns the ARGB color for the current Icon.
	 */
	public static int getIconColor() {
		return switch (icon) {
			case "DRAGON" -> 0xFFFF4400;
			case "STRENGTH" -> 0xFFCC6600;
			case "SWORD" -> 0xFFCCCCDD;
			case "DEATH" -> 0xFF440044;
			case "SPEAR" -> 0xFFFFDD44;
			case "HAMMER" -> 0xFF888899;
			case "STORM" -> 0xFF4466CC;
			case "VOID" -> 0xFF220033;
			case "CROWN" -> 0xFFFFD700;
			case "HEART" -> 0xFFFF3366;
			case "SHIELD" -> 0xFF66AACC;
			default -> 0xFF999999;
		};
	}

	/**
	 * Returns true if the player has manifested an Icon.
	 */
	public static boolean hasIcon() {
		return !"NONE".equals(icon);
	}

	/**
	 * Returns true if the player is at Copper stage or higher.
	 * Copper Sight toggle is available at Copper+.
	 */
	public static boolean isCopper() {
		return !"FOUNDATION".equals(stage);
	}

	/**
	 * Returns true if the player has a Goldsign (from Remnant absorption).
	 */
	public static boolean hasGoldsign() {
		return goldsignOrdinal > 0;
	}

	// ── Remote player goldsign tracking (multiplayer) ──────────────────

	/**
	 * Stores a remote player's goldsign ordinal. If the ordinal is 0 (NONE),
	 * the entry is removed from the map to avoid accumulating stale data.
	 */
	public static void setRemotePlayerGoldsign(UUID playerUuid, int goldsignOrd) {
		if (goldsignOrd == 0) {
			remotePlayerGoldsigns.remove(playerUuid);
		} else {
			remotePlayerGoldsigns.put(playerUuid, goldsignOrd);
		}
	}

	/**
	 * Clears all remote player goldsign data. Called on disconnect via {@link #reset()}.
	 */
	public static void clearRemoteGoldsigns() {
		remotePlayerGoldsigns.clear();
	}

	/**
	 * Returns the goldsign ordinal for the player represented by the given render state.
	 * For the local player, returns the cached {@link #goldsignOrdinal}.
	 * For remote players, looks up their UUID in the remote goldsign map.
	 *
	 * @return goldsign ordinal (0 = NONE)
	 */
	public static int getGoldsignForPlayer(AvatarRenderState state) {
		Minecraft mc = Minecraft.getInstance();

		// Local player: use the directly-synced goldsign ordinal
		if (mc.player != null && state.id == mc.player.getId()) {
			return goldsignOrdinal;
		}

		// Remote player: resolve entity ID -> UUID -> map lookup
		if (mc.level != null) {
			Entity entity = mc.level.getEntity(state.id);
			if (entity != null) {
				Integer ordinal = remotePlayerGoldsigns.get(entity.getUUID());
				if (ordinal != null) {
					return ordinal;
				}
			}
		}

		return 0; // No goldsign
	}

	/**
	 * Returns a human-readable display name for the current Goldsign.
	 */
	public static String getGoldsignDisplayName() {
		return switch (goldsignOrdinal) {
			case 1 -> "Burning Eyes";
			case 2 -> "Blade Arms";
			case 3 -> "Spear Light";
			case 4 -> "Crackling Skin";
			case 5 -> "Pale Aura";
			default -> "None";
		};
	}

	/**
	 * Returns the ARGB color for the current Goldsign.
	 */
	public static int getGoldsignColor() {
		return switch (goldsignOrdinal) {
			case 1 -> 0xFFFF4400;   // Black Flame Eyes — fiery orange
			case 2 -> 0xFFCCCCDD;   // Sword Arms — silver
			case 3 -> 0xFFFFDD44;   // Spear Light — gold
			case 4 -> 0xFF8888CC;   // Crackling Skin — pale blue
			case 5 -> 0xFFDDDDFF;   // Pale Aura — white-blue
			default -> 0xFF999999;
		};
	}
}

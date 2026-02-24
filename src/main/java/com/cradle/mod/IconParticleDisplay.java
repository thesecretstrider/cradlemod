package com.cradle.mod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Spawns a large particle formation in the sky above a player when they manifest an Icon.
 * Each Icon has a unique shape made of particles, visible to all nearby players.
 * The formation appears ~15 blocks above the player and is ~10 blocks tall.
 */
public final class IconParticleDisplay {

	private static final double SKY_OFFSET_Y = 15.0; // blocks above the player
	private static final double SCALE = 1.5;          // overall scale multiplier

	/**
	 * Spawns the Icon particle formation above the player.
	 * Called when a player manifests their Icon (becomes Sage or Monarch).
	 */
	public static void spawnIconDisplay(ServerPlayer player, CradlePlayerData.Icon icon) {
		if (!(player.level() instanceof ServerLevel level)) return;

		double baseX = player.getX();
		double baseY = player.getY() + SKY_OFFSET_Y;
		double baseZ = player.getZ();

		// Get the pixel coordinates for this icon shape
		List<double[]> points = getIconShape(icon);

		// Spawn particles at each point — use END_ROD for golden glow + SOUL_FIRE_FLAME for ethereal feel
		for (double[] point : points) {
			double px = baseX + point[0] * SCALE;
			double py = baseY + point[1] * SCALE;

			// Spawn in a flat plane facing the player's look direction
			// Use the player's Y rotation to orient the shape so it faces outward
			double angle = Math.toRadians(player.getYRot() + 180);
			double cosA = Math.cos(angle);
			double sinA = Math.sin(angle);

			// Rotate the X offset around Y axis so the shape faces the player's facing direction
			double worldX = baseX + point[0] * SCALE * cosA;
			double worldZ = baseZ + point[0] * SCALE * sinA;
			double worldY = baseY + point[1] * SCALE;

			// Main particles — bright, visible from far
			level.sendParticles(ParticleTypes.END_ROD,
					worldX, worldY, worldZ,
					3, 0.1, 0.1, 0.1, 0.0);

			// Accent particles — soul fire flame for ethereal glow
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					worldX, worldY, worldZ,
					2, 0.05, 0.05, 0.05, 0.0);
		}

		// Extra burst at center for dramatic flair
		level.sendParticles(ParticleTypes.END_ROD,
				baseX, baseY + 3.0 * SCALE, baseZ,
				50, 1.5, 2.0, 1.5, 0.05);
	}

	/**
	 * Returns a list of [x, y] coordinates forming the shape for the given Icon.
	 * X is horizontal offset, Y is vertical offset (positive = up).
	 * Origin (0,0) is the center-bottom of the formation.
	 */
	private static List<double[]> getIconShape(CradlePlayerData.Icon icon) {
		return switch (icon) {
			case DRAGON -> getDragonShape();
			case STRENGTH -> getStrengthShape();
			case SWORD -> getSwordShape();
			case DEATH -> getDeathShape();
			case SPEAR -> getSpearShape();
			case HAMMER -> getHammerShape();
			case STORM -> getStormShape();
			case VOID -> getVoidShape();
			case CROWN -> getCrownShape();
			case HEART -> getHeartShape();
			case SHIELD -> getShieldShape();
			default -> List.of();
		};
	}

	// ── Icon Shapes ──────────────────────────────────────────────────

	/** Dragon: spread wings with a central body */
	private static List<double[]> getDragonShape() {
		List<double[]> points = new ArrayList<>();
		// Central body (vertical line)
		for (double y = 0; y <= 6; y += 0.5) {
			points.add(new double[]{0, y});
		}
		// Left wing (sweeping arc)
		for (double t = 0; t <= 1.0; t += 0.08) {
			double x = -t * 5.0;
			double y = 4.0 + Math.sin(t * Math.PI * 0.7) * 2.5;
			points.add(new double[]{x, y});
		}
		// Right wing (mirror)
		for (double t = 0; t <= 1.0; t += 0.08) {
			double x = t * 5.0;
			double y = 4.0 + Math.sin(t * Math.PI * 0.7) * 2.5;
			points.add(new double[]{x, y});
		}
		// Head (diamond at top)
		points.add(new double[]{0, 7});
		points.add(new double[]{-0.5, 6.5});
		points.add(new double[]{0.5, 6.5});
		// Tail (below)
		for (double t = 0; t <= 1.0; t += 0.15) {
			points.add(new double[]{Math.sin(t * 3) * 0.5, -t * 2});
		}
		return points;
	}

	/** Strength: raised fist / flexed arm */
	private static List<double[]> getStrengthShape() {
		List<double[]> points = new ArrayList<>();
		// Forearm (angled line going up-right)
		for (double t = 0; t <= 1.0; t += 0.08) {
			points.add(new double[]{-2.0 + t * 2.0, t * 3.5});
		}
		// Upper arm (vertical going up from elbow)
		for (double t = 0; t <= 1.0; t += 0.08) {
			points.add(new double[]{0, 3.5 + t * 3.0});
		}
		// Fist (circle at top)
		for (double a = 0; a < 360; a += 25) {
			double rad = Math.toRadians(a);
			points.add(new double[]{Math.cos(rad) * 1.0, 6.5 + Math.sin(rad) * 1.0});
		}
		// Bicep curve
		for (double t = 0; t <= 1.0; t += 0.1) {
			double bulge = Math.sin(t * Math.PI) * 0.8;
			points.add(new double[]{bulge + 0.5, 3.5 + t * 3.0});
		}
		return points;
	}

	/** Sword: vertical blade with crossguard and pommel */
	private static List<double[]> getSwordShape() {
		List<double[]> points = new ArrayList<>();
		// Blade (long vertical line)
		for (double y = 1.5; y <= 7; y += 0.4) {
			points.add(new double[]{0, y});
			// Blade width tapers toward tip
			double width = 0.3 * (1.0 - (y - 1.5) / 5.5);
			if (width > 0.05) {
				points.add(new double[]{-width, y});
				points.add(new double[]{width, y});
			}
		}
		// Tip
		points.add(new double[]{0, 7.5});
		// Crossguard (horizontal line at y=1.5)
		for (double x = -2.0; x <= 2.0; x += 0.4) {
			points.add(new double[]{x, 1.5});
		}
		// Handle
		for (double y = 0; y <= 1.5; y += 0.4) {
			points.add(new double[]{0, y});
		}
		// Pommel (small circle at bottom)
		for (double a = 0; a < 360; a += 45) {
			double rad = Math.toRadians(a);
			points.add(new double[]{Math.cos(rad) * 0.3, Math.sin(rad) * 0.3});
		}
		return points;
	}

	/** Death: skull shape (circle head + X eyes + jaw) */
	private static List<double[]> getDeathShape() {
		List<double[]> points = new ArrayList<>();
		// Skull outline (circle)
		for (double a = 0; a < 360; a += 12) {
			double rad = Math.toRadians(a);
			points.add(new double[]{Math.cos(rad) * 2.5, 4.5 + Math.sin(rad) * 2.5});
		}
		// Left eye (X)
		for (double t = -0.5; t <= 0.5; t += 0.15) {
			points.add(new double[]{-1.0 + t, 5.0 + t});
			points.add(new double[]{-1.0 + t, 5.0 - t});
		}
		// Right eye (X)
		for (double t = -0.5; t <= 0.5; t += 0.15) {
			points.add(new double[]{1.0 + t, 5.0 + t});
			points.add(new double[]{1.0 + t, 5.0 - t});
		}
		// Jaw (curved line below skull)
		for (double x = -1.5; x <= 1.5; x += 0.3) {
			double y = 2.0 - Math.abs(x) * 0.3;
			points.add(new double[]{x, y});
		}
		// Teeth
		for (double x = -1.0; x <= 1.0; x += 0.5) {
			points.add(new double[]{x, 2.3});
			points.add(new double[]{x, 1.8});
		}
		return points;
	}

	/** Spear: long vertical shaft with pointed head */
	private static List<double[]> getSpearShape() {
		List<double[]> points = new ArrayList<>();
		// Shaft (long vertical line)
		for (double y = 0; y <= 6; y += 0.4) {
			points.add(new double[]{0, y});
		}
		// Spearhead (diamond/triangle at top)
		for (double t = 0; t <= 1.0; t += 0.08) {
			double y = 6.0 + t * 2.0;
			double width = (1.0 - t) * 1.0;
			points.add(new double[]{-width, y});
			points.add(new double[]{width, y});
		}
		points.add(new double[]{0, 8.0}); // tip
		// Crossbar near spearhead base
		for (double x = -0.8; x <= 0.8; x += 0.3) {
			points.add(new double[]{x, 6.0});
		}
		return points;
	}

	/** Hammer: T-shape with heavy head */
	private static List<double[]> getHammerShape() {
		List<double[]> points = new ArrayList<>();
		// Handle (vertical)
		for (double y = 0; y <= 5; y += 0.4) {
			points.add(new double[]{0, y});
		}
		// Hammer head (thick horizontal rectangle at top)
		for (double x = -2.5; x <= 2.5; x += 0.35) {
			for (double y = 5.0; y <= 6.5; y += 0.5) {
				points.add(new double[]{x, y});
			}
		}
		// Head outline (brighter edges)
		for (double x = -2.5; x <= 2.5; x += 0.3) {
			points.add(new double[]{x, 5.0});
			points.add(new double[]{x, 6.5});
		}
		return points;
	}

	/** Storm: lightning bolt zigzag */
	private static List<double[]> getStormShape() {
		List<double[]> points = new ArrayList<>();
		// Lightning bolt shape (zigzag from top to bottom)
		double[][] zigzag = {
				{0.5, 7.0}, {-1.0, 5.5}, {0.5, 5.5},
				{-1.5, 3.5}, {0.0, 3.5}, {-2.0, 1.0},
				{0.0, 2.5}, {-0.5, 2.5}, {1.5, 5.0},
				{0.0, 5.0}, {2.0, 7.0}
		};
		// Interpolate between zigzag points for smoother lines
		for (int i = 0; i < zigzag.length - 1; i++) {
			double x0 = zigzag[i][0], y0 = zigzag[i][1];
			double x1 = zigzag[i + 1][0], y1 = zigzag[i + 1][1];
			int steps = (int) (Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2)) / 0.3);
			for (int s = 0; s <= steps; s++) {
				double t = (double) s / steps;
				points.add(new double[]{x0 + (x1 - x0) * t, y0 + (y1 - y0) * t});
			}
		}
		// Glow center
		points.add(new double[]{0, 4.0});
		return points;
	}

	/** Void: hollow ring/circle (emptiness) */
	private static List<double[]> getVoidShape() {
		List<double[]> points = new ArrayList<>();
		// Outer ring
		for (double a = 0; a < 360; a += 8) {
			double rad = Math.toRadians(a);
			points.add(new double[]{Math.cos(rad) * 3.0, 4.0 + Math.sin(rad) * 3.0});
		}
		// Inner ring (slightly smaller, for thickness)
		for (double a = 0; a < 360; a += 10) {
			double rad = Math.toRadians(a);
			points.add(new double[]{Math.cos(rad) * 2.5, 4.0 + Math.sin(rad) * 2.5});
		}
		// Center is intentionally empty — it's the Void Icon
		return points;
	}

	/** Crown: crown shape with three points */
	private static List<double[]> getCrownShape() {
		List<double[]> points = new ArrayList<>();
		// Base band (horizontal line)
		for (double x = -3.0; x <= 3.0; x += 0.3) {
			points.add(new double[]{x, 3.0});
			points.add(new double[]{x, 3.5});
		}
		// Three peaks
		double[][] peaks = {{-2.0, 3.5, -1.5, 6.0}, {-0.5, 3.5, 0.0, 7.0}, {1.0, 3.5, 1.5, 6.0}};
		// Left peak
		for (double t = 0; t <= 1.0; t += 0.08) {
			points.add(new double[]{-2.5 + t * 1.0, 3.5 + t * 2.5});
		}
		for (double t = 0; t <= 1.0; t += 0.08) {
			points.add(new double[]{-1.5 - t * 0.5 + t * 1.5, 6.0 - t * 2.5});
		}
		// Center peak (tallest)
		for (double t = 0; t <= 1.0; t += 0.08) {
			points.add(new double[]{-0.8 + t * 0.8, 3.5 + t * 3.5});
		}
		for (double t = 0; t <= 1.0; t += 0.08) {
			points.add(new double[]{0.0 + t * 0.8, 7.0 - t * 3.5});
		}
		// Right peak
		for (double t = 0; t <= 1.0; t += 0.08) {
			points.add(new double[]{1.0 + t * 1.5, 3.5 + t * 2.5});
		}
		for (double t = 0; t <= 1.0; t += 0.08) {
			points.add(new double[]{2.5 - t * 1.0, 6.0 - t * 2.5});
		}
		// Gems at peak tips
		for (double a = 0; a < 360; a += 45) {
			double rad = Math.toRadians(a);
			points.add(new double[]{-1.5 + Math.cos(rad) * 0.3, 6.0 + Math.sin(rad) * 0.3});
			points.add(new double[]{0.0 + Math.cos(rad) * 0.3, 7.0 + Math.sin(rad) * 0.3});
			points.add(new double[]{1.5 + Math.cos(rad) * 0.3, 6.0 + Math.sin(rad) * 0.3});
		}
		return points;
	}

	/** Heart: classic heart shape */
	private static List<double[]> getHeartShape() {
		List<double[]> points = new ArrayList<>();
		// Heart parametric curve
		for (double t = 0; t < 2 * Math.PI; t += 0.1) {
			double x = 16 * Math.pow(Math.sin(t), 3);
			double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
			// Scale down to reasonable size (the parametric heart is ~32 units wide)
			points.add(new double[]{x / 6.0, y / 6.0 + 4.0});
		}
		// Fill interior partially for visibility
		for (double t = 0; t < 2 * Math.PI; t += 0.15) {
			double x = 16 * Math.pow(Math.sin(t), 3) * 0.6;
			double y = (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) * 0.6;
			points.add(new double[]{x / 6.0, y / 6.0 + 4.0});
		}
		return points;
	}

	/** Shield: kite/heater shield shape */
	private static List<double[]> getShieldShape() {
		List<double[]> points = new ArrayList<>();
		// Shield outline — top is flat/curved, sides taper to a point at bottom
		// Top arc
		for (double a = 0; a <= 180; a += 10) {
			double rad = Math.toRadians(a);
			points.add(new double[]{Math.cos(rad) * 2.5, 6.0 + Math.sin(rad) * 1.0});
		}
		// Left side (straight from top-left down to bottom point)
		for (double t = 0; t <= 1.0; t += 0.06) {
			points.add(new double[]{-2.5 * (1.0 - t), 6.0 - t * 5.0});
		}
		// Right side (mirror)
		for (double t = 0; t <= 1.0; t += 0.06) {
			points.add(new double[]{2.5 * (1.0 - t), 6.0 - t * 5.0});
		}
		// Center vertical line (reinforcement)
		for (double y = 1.0; y <= 7.0; y += 0.4) {
			points.add(new double[]{0, y});
		}
		// Horizontal bar across middle
		double midY = 4.5;
		double widthAtMid = 2.5 * (1.0 - (6.0 - midY) / 5.0);
		for (double x = -widthAtMid; x <= widthAtMid; x += 0.3) {
			points.add(new double[]{x, midY});
		}
		return points;
	}
}

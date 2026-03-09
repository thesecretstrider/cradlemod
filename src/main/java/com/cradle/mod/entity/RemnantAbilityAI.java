package com.cradle.mod.entity;

import com.cradle.mod.ability.AbilityDefinition;
import com.cradle.mod.ability.AbilityRegistry;
import com.cradle.mod.ability.AbilityType;
import com.cradle.mod.ability.PlayerLoadout;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

/**
 * Range-based tactical AI for remnant ability usage.
 * Picks abilities based on distance to target:
 * - Close (< 4 blocks): Enforcer
 * - Mid (4-10 blocks): Striker
 * - Long (10+ blocks): Ruler
 */
public final class RemnantAbilityAI {

	private static final int DECISION_INTERVAL = 20; // Re-evaluate every 1 second
	private static final double CLOSE_RANGE = 4.0;
	private static final double MID_RANGE = 10.0;

	private final RemnantEntity remnant;
	private final Map<Integer, Long> cooldowns = new HashMap<>();
	private final Set<Integer> activeToggleSlots = new HashSet<>();
	private final Map<Integer, Integer> rulerTickCounters = new HashMap<>();
	private int decisionTicks = 0;
	private final int abilityCount;

	public RemnantAbilityAI(RemnantEntity remnant) {
		this.remnant = remnant;
		this.abilityCount = getAbilityCountForPower(remnant.getPowerLevel());
	}

	/**
	 * Called every server tick. Handles ability decisions and active ability effects.
	 */
	public void tick(ServerLevel level) {
		PlayerLoadout loadout = remnant.getStoredLoadout();
		if (loadout == null || abilityCount <= 0) return;

		LivingEntity target = remnant.getTarget();
		if (target == null || !target.isAlive()) {
			deactivateAll();
			return;
		}

		// Tick active enforcers/rulers (drain madra)
		tickActiveAbilities();

		// Decision tick: pick an ability to use
		decisionTicks++;
		if (decisionTicks >= DECISION_INTERVAL) {
			decisionTicks = 0;
			makeAbilityDecision(target);
		}
	}

	private void makeAbilityDecision(LivingEntity target) {
		PlayerLoadout loadout = remnant.getStoredLoadout();
		if (loadout == null) return;

		double distance = remnant.distanceTo(target);

		// Determine preferred type based on range
		AbilityType preferred;
		if (distance < CLOSE_RANGE) {
			preferred = AbilityType.ENFORCER;
		} else if (distance < MID_RANGE) {
			preferred = AbilityType.STRIKER;
		} else {
			preferred = AbilityType.RULER;
		}

		// Build list of usable abilities (up to abilityCount), preferring the right type
		List<int[]> candidates = new ArrayList<>();
		for (int slot = 0; slot < Math.min(PlayerLoadout.MAX_SLOTS, abilityCount); slot++) {
			if (!loadout.hasAbility(slot)) continue;
			AbilityDefinition def = AbilityRegistry.get(loadout.getAbility(slot));
			if (def == null) continue;
			if (isOnCooldown(slot, def, loadout.getUpgradeLevel(slot))) continue;

			int priority = (def.getType() == preferred) ? 0 : 1;
			candidates.add(new int[]{slot, priority});
		}

		// Sort by priority (preferred type first)
		candidates.sort(Comparator.comparingInt(a -> a[1]));

		// Try to use the best candidate
		for (int[] candidate : candidates) {
			int slot = candidate[0];
			if (tryUseAbility(slot, target)) {
				break;
			}
		}
	}

	private boolean tryUseAbility(int slot, LivingEntity target) {
		PlayerLoadout loadout = remnant.getStoredLoadout();
		String abilityId = loadout.getAbility(slot);
		AbilityDefinition def = AbilityRegistry.get(abilityId);
		if (def == null) return false;

		int upgradeLevel = loadout.getUpgradeLevel(slot);
		float cost = def.getScaledMadraCost(upgradeLevel);

		if (remnant.getMadraPool() < cost) return false;

		switch (def.getType()) {
			case STRIKER -> {
				// Fire and forget: deduct madra, apply cooldown, fire projectile
				remnant.setMadraPool(remnant.getMadraPool() - cost);
				setCooldown(slot);
				remnant.getLookControl().setLookAt(target);
				if (remnant.level() instanceof ServerLevel serverLevel) {
					RemnantAbilityExecutor.fireStriker(remnant, serverLevel, def, upgradeLevel);
				}
				return true;
			}
			case ENFORCER -> {
				if (!activeToggleSlots.contains(slot)) {
					activeToggleSlots.add(slot);
					return true;
				}
				return false;
			}
			case RULER -> {
				if (!activeToggleSlots.contains(slot)) {
					activeToggleSlots.add(slot);
					rulerTickCounters.put(slot, 0);
					return true;
				}
				return false;
			}
		}
		return false;
	}

	private void tickActiveAbilities() {
		PlayerLoadout loadout = remnant.getStoredLoadout();
		if (loadout == null) return;
		if (!(remnant.level() instanceof ServerLevel serverLevel)) return;

		Iterator<Integer> iter = activeToggleSlots.iterator();
		while (iter.hasNext()) {
			int slot = iter.next();
			String abilityId = loadout.getAbility(slot);
			if (abilityId == null) { iter.remove(); continue; }
			AbilityDefinition def = AbilityRegistry.get(abilityId);
			if (def == null) { iter.remove(); continue; }

			int upgradeLevel = loadout.getUpgradeLevel(slot);
			float drain = def.getScaledMadraCost(upgradeLevel);
			remnant.setMadraPool(remnant.getMadraPool() - drain);

			if (remnant.getMadraPool() <= 0) {
				// Out of madra — deactivate and clean up
				if (def.getType() == AbilityType.ENFORCER) {
					RemnantAbilityExecutor.deactivateEnforcer(remnant, def);
				}
				rulerTickCounters.remove(slot);
				iter.remove();
				continue;
			}

			// Apply actual ability effects
			switch (def.getType()) {
				case ENFORCER -> {
					RemnantAbilityExecutor.tickEnforcer(remnant, serverLevel, def, upgradeLevel);
				}
				case RULER -> {
					// Ruler area effects apply every RULER_EFFECT_INTERVAL ticks (0.5s)
					int ticks = rulerTickCounters.getOrDefault(slot, 0) + 1;
					if (ticks >= RemnantAbilityExecutor.getRulerEffectInterval()) {
						ticks = 0;
						RemnantAbilityExecutor.tickRuler(remnant, serverLevel, def, upgradeLevel);
					}
					rulerTickCounters.put(slot, ticks);
				}
				default -> {} // Strikers don't tick
			}
		}
	}

	private void deactivateAll() {
		if (!activeToggleSlots.isEmpty()) {
			PlayerLoadout loadout = remnant.getStoredLoadout();
			if (loadout != null) {
				for (int slot : activeToggleSlots) {
					String abilityId = loadout.getAbility(slot);
					if (abilityId == null) continue;
					AbilityDefinition def = AbilityRegistry.get(abilityId);
					if (def != null && def.getType() == AbilityType.ENFORCER) {
						RemnantAbilityExecutor.deactivateEnforcer(remnant, def);
					}
				}
			}
			activeToggleSlots.clear();
			rulerTickCounters.clear();
		}
	}

	private boolean isOnCooldown(int slot, AbilityDefinition def, int upgradeLevel) {
		Long lastUse = cooldowns.get(slot);
		if (lastUse == null) return false;
		long cooldownTicks = def.getScaledCooldownMs(upgradeLevel) / 50;
		return (remnant.tickCount - lastUse) < cooldownTicks;
	}

	private void setCooldown(int slot) {
		cooldowns.put(slot, (long) remnant.tickCount);
	}

	/**
	 * Returns the number of abilities a remnant can use based on its power level.
	 */
	private static int getAbilityCountForPower(int powerLevel) {
		if (powerLevel <= 2) return 0;  // Copper-Iron: melee only
		if (powerLevel <= 4) return 1;  // Jade-Lowgold: 1 ability
		if (powerLevel <= 6) return 2;  // Highgold-Truegold: 2
		if (powerLevel <= 8) return 3;  // Underlord-Overlord: 3
		if (powerLevel <= 10) return 4; // Archlord-Sage: 4
		if (powerLevel == 11) return 5; // Herald: 5
		return 6;                       // Monarch: full loadout
	}
}

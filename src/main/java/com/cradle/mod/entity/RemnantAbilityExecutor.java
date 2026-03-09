package com.cradle.mod.entity;

import com.cradle.mod.CradlePlayerData;
import com.cradle.mod.CyclingManager;
import com.cradle.mod.ability.AbilityDefinition;
import com.cradle.mod.ability.AbilityType;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Executes ability effects on behalf of RemnantEntity.
 *
 * This is the entity-aware counterpart to AbilityExecutor (which handles
 * ServerPlayer abilities). It replicates the same ability effects using
 * LivingEntity APIs, so remnants can fire projectiles, apply enforcer
 * buffs, and create ruler area effects.
 *
 * Design choice: Rather than changing all handler interfaces from
 * ServerPlayer to LivingEntity (which would touch 30+ lambdas and
 * multiple files), this class implements entity-specific versions of
 * each ability's effects in a single contained file. The logic mirrors
 * AbilityDefinitions but uses LivingEntity methods instead of
 * ServerPlayer-specific ones (like displayClientMessage).
 */
public final class RemnantAbilityExecutor {

	private RemnantAbilityExecutor() {}

	// ── Modifier IDs (must match AbilityDefinitions / CyclingManager) ────
	private static final Identifier ENFORCER_ATTACK_DAMAGE_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_attack_damage");
	private static final Identifier ENFORCER_SPEED_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_speed");
	private static final Identifier ENFORCER_ATTACK_SPEED_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_attack_speed");
	private static final Identifier ENFORCER_ARMOR_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_armor");
	private static final Identifier ENFORCER_KNOCKBACK_RESISTANCE_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_knockback_resistance");
	private static final Identifier ENFORCER_KNOCKBACK_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_knockback");

	// Ruler area effect interval in ticks (matches AbilityExecutor)
	private static final int RULER_EFFECT_INTERVAL = 10;

	// ══════════════════════════════════════════════════════════════════════
	// STRIKER — Fire projectiles from a remnant
	// ══════════════════════════════════════════════════════════════════════

	/**
	 * Fires a striker ability from the remnant toward its current target.
	 * Most strikers spawn a StrikerProjectileEntity (which already accepts
	 * LivingEntity as owner). Special cases like Spirit Pulse do AoE instead.
	 */
	public static void fireStriker(RemnantEntity remnant, ServerLevel level,
	                                AbilityDefinition def, int upgradeLevel) {
		LivingEntity target = remnant.getTarget();
		if (target == null || !target.isAlive()) return;

		float pm = effectivePower(remnant.getPowerLevel(), upgradeLevel);
		CradlePlayerData.Path path = parsePath(remnant.getRemnantPathName());

		// Look at target before firing
		remnant.getLookControl().setLookAt(target);

		// Spirit Pulse is a special AoE shockwave, not a projectile
		if ("universal_spirit_pulse".equals(def.getId())) {
			fireAoeShockwave(remnant, level, pm, upgradeLevel);
			return;
		}

		// Sword Storm fires 3 projectiles in a spread
		if ("endless_sword_storm".equals(def.getId())) {
			fireSwordStorm(remnant, level, path, pm, def);
			return;
		}

		// Standard projectile — per-ability velocity and power tuning
		float velocityMult = 1.5f;
		float powerMult = pm;

		switch (def.getId()) {
			case "blackflame_meteor" -> { velocityMult = 1.2f; powerMult = pm * 1.5f; }
			case "stellar_nova" -> { velocityMult = 2.0f; powerMult = pm * 1.5f; }
			case "cloud_thunderstrike" -> { velocityMult = 1.2f; powerMult = pm * 1.5f; }
			case "hollow_nullify" -> { powerMult = pm * 1.2f; }
			default -> {} // Use defaults
		}

		// Calculate direction toward target (not just look angle, since AI may not have rotated yet)
		Vec3 dirToTarget = target.getEyePosition().subtract(remnant.getEyePosition()).normalize();

		StrikerProjectileEntity proj = new StrikerProjectileEntity(
				level, remnant, dirToTarget.scale(velocityMult), path, powerMult);
		proj.setAbilityId(def.getId());
		proj.setPos(remnant.getEyePosition().add(dirToTarget.scale(0.5)));
		level.addFreshEntity(proj);
	}

	private static void fireAoeShockwave(RemnantEntity remnant, ServerLevel level,
	                                      float pm, int upgradeLevel) {
		float radius = 5.0f + upgradeLevel * 0.3f;
		AABB area = remnant.getBoundingBox().inflate(radius);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
				e -> e != remnant && e.isAlive() && !(e instanceof RemnantEntity));
		for (LivingEntity e : targets) {
			Vec3 push = e.position().subtract(remnant.position()).normalize().scale(1.5 * pm);
			e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.4, push.z));
			e.hurtServer(level, remnant.damageSources().magic(), 6.0f * pm);
		}
	}

	private static void fireSwordStorm(RemnantEntity remnant, ServerLevel level,
	                                    CradlePlayerData.Path path, float pm, AbilityDefinition def) {
		LivingEntity target = remnant.getTarget();
		if (target == null) return;
		Vec3 dir = target.getEyePosition().subtract(remnant.getEyePosition()).normalize();
		for (int i = -1; i <= 1; i++) {
			Vec3 spreadDir = dir.yRot((float) (i * 0.15)).scale(1.5);
			StrikerProjectileEntity proj = new StrikerProjectileEntity(
					level, remnant, spreadDir, path, pm);
			proj.setAbilityId(def.getId());
			proj.setPos(remnant.getEyePosition().add(dir.scale(0.5)));
			level.addFreshEntity(proj);
		}
	}

	// ══════════════════════════════════════════════════════════════════════
	// ENFORCER — Attribute modifiers and status effects on the remnant
	// ══════════════════════════════════════════════════════════════════════

	/**
	 * Applies per-tick enforcer effects to the remnant.
	 * Mirrors the onEnforcerTick handlers from AbilityDefinitions.
	 */
	public static void tickEnforcer(RemnantEntity remnant, ServerLevel level,
	                                 AbilityDefinition def, int upgradeLevel) {
		float pm = effectivePower(remnant.getPowerLevel(), upgradeLevel);

		switch (def.getId()) {
			// ── Basic Enforcement ──
			case "basic_enforcement" -> {
				ensureModifier(remnant, Attributes.ARMOR, ENFORCER_ARMOR_ID,
						1.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(remnant, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						0.01 * pm, AttributeModifier.Operation.ADD_VALUE);
			}

			// ── Black Flame: Burning Body ──
			case "blackflame_burning_body" -> {
				ensureModifier(remnant, Attributes.ATTACK_DAMAGE, ENFORCER_ATTACK_DAMAGE_ID,
						4.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(remnant, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						0.04 * pm, AttributeModifier.Operation.ADD_VALUE);
				refreshEffect(remnant, MobEffects.FIRE_RESISTANCE, 60, 0);
				// Remnants skip self-damage strain (they're already dead spirits)
			}

			// ── Black Flame: Inferno Form ──
			case "blackflame_inferno_form" -> {
				ensureModifier(remnant, Attributes.ATTACK_DAMAGE, ENFORCER_ATTACK_DAMAGE_ID,
						8.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(remnant, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						0.06 * pm, AttributeModifier.Operation.ADD_VALUE);
				refreshEffect(remnant, MobEffects.FIRE_RESISTANCE, 60, 0);
				// Ignition aura — set nearby enemies on fire
				AABB aura = remnant.getBoundingBox().inflate(3.0);
				List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, aura,
						e -> e != remnant && e.isAlive() && !(e instanceof RemnantEntity));
				for (LivingEntity e : nearby) {
					e.igniteForSeconds(3.0f);
				}
			}

			// ── Endless Sword: Flowing Edge ──
			case "endless_flowing_edge" -> {
				ensureModifier(remnant, Attributes.ATTACK_SPEED, ENFORCER_ATTACK_SPEED_ID,
						1.0 * pm, AttributeModifier.Operation.ADD_VALUE);
			}

			// ── Endless Sword: Thousand Cuts ──
			case "endless_thousand_cuts" -> {
				ensureModifier(remnant, Attributes.ATTACK_SPEED, ENFORCER_ATTACK_SPEED_ID,
						2.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(remnant, Attributes.ATTACK_DAMAGE, ENFORCER_ATTACK_DAMAGE_ID,
						3.0 * pm, AttributeModifier.Operation.ADD_VALUE);
			}

			// ── Stellar Spear: Stellar Alignment ──
			case "stellar_alignment" -> {
				ensureModifier(remnant, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						0.04 * pm, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(remnant, Attributes.KNOCKBACK_RESISTANCE, ENFORCER_KNOCKBACK_RESISTANCE_ID,
						Math.min(0.6 * pm, 1.0), AttributeModifier.Operation.ADD_VALUE);
			}

			// ── Stellar Spear: Lightspeed ──
			case "stellar_lightspeed" -> {
				ensureModifier(remnant, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						0.08 * pm, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(remnant, Attributes.KNOCKBACK_RESISTANCE, ENFORCER_KNOCKBACK_RESISTANCE_ID,
						Math.min(0.9 * pm, 1.0), AttributeModifier.Operation.ADD_VALUE);
				if (remnant.isSprinting()) {
					ensureModifier(remnant, Attributes.ATTACK_DAMAGE, ENFORCER_ATTACK_DAMAGE_ID,
							5.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				}
			}

			// ── Cloud Hammer: Thunderous Weight ──
			case "cloud_thunderous_weight" -> {
				ensureModifier(remnant, Attributes.ARMOR, ENFORCER_ARMOR_ID,
						6.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(remnant, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						-0.03 + (pm - 1.0) * 0.01, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(remnant, Attributes.ATTACK_KNOCKBACK, ENFORCER_KNOCKBACK_RESISTANCE_ID,
						2.0 * pm, AttributeModifier.Operation.ADD_VALUE);
			}

			// ── Cloud Hammer: Living Fortress ──
			case "cloud_living_fortress" -> {
				ensureModifier(remnant, Attributes.ARMOR, ENFORCER_ARMOR_ID,
						12.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(remnant, Attributes.ATTACK_DAMAGE, ENFORCER_ATTACK_DAMAGE_ID,
						4.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(remnant, Attributes.KNOCKBACK_RESISTANCE, ENFORCER_KNOCKBACK_RESISTANCE_ID,
						1.0, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(remnant, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						-0.04 + (pm - 1.0) * 0.01, AttributeModifier.Operation.ADD_VALUE);
				ensureModifier(remnant, Attributes.ATTACK_KNOCKBACK, ENFORCER_KNOCKBACK_ID,
						4.0 * pm, AttributeModifier.Operation.ADD_VALUE);
			}

			// ── Hollow King: Hollow Circulation ──
			case "hollow_circulation" -> {
				refreshEffect(remnant, MobEffects.RESISTANCE, 60, pm >= 1.5f ? 1 : 0);
			}

			// ── Hollow King: Void Body ──
			case "hollow_void_body" -> {
				refreshEffect(remnant, MobEffects.RESISTANCE, 60, 1);
				refreshEffect(remnant, MobEffects.REGENERATION, 60, 0);
				ensureModifier(remnant, Attributes.ARMOR, ENFORCER_ARMOR_ID,
						4.0 * pm, AttributeModifier.Operation.ADD_VALUE);
			}

			default -> {
				// Unknown enforcer — apply generic small armor buff
				ensureModifier(remnant, Attributes.ARMOR, ENFORCER_ARMOR_ID,
						2.0 * pm, AttributeModifier.Operation.ADD_VALUE);
			}
		}
	}

	/**
	 * Removes all enforcer attribute modifiers and effects from the remnant.
	 * Called when an enforcer is deactivated or the remnant runs out of madra.
	 */
	public static void deactivateEnforcer(RemnantEntity remnant, AbilityDefinition def) {
		removeEnforcerModifiers(remnant);

		// Remove ability-specific effects
		switch (def.getId()) {
			case "hollow_circulation" -> remnant.removeEffect(MobEffects.RESISTANCE);
			case "hollow_void_body" -> {
				remnant.removeEffect(MobEffects.RESISTANCE);
				remnant.removeEffect(MobEffects.REGENERATION);
			}
			case "cloud_living_fortress" -> {
				var knockbackInst = remnant.getAttribute(Attributes.ATTACK_KNOCKBACK);
				if (knockbackInst != null) {
					knockbackInst.removeModifier(ENFORCER_KNOCKBACK_ID);
				}
			}
			default -> {}
		}
	}

	// ══════════════════════════════════════════════════════════════════════
	// RULER — Area effects around the remnant
	// ══════════════════════════════════════════════════════════════════════

	/**
	 * Applies ruler area effects to enemies near the remnant.
	 * Called every RULER_EFFECT_INTERVAL ticks (0.5s).
	 */
	public static void tickRuler(RemnantEntity remnant, ServerLevel level,
	                              AbilityDefinition def, int upgradeLevel) {
		float pm = effectivePower(remnant.getPowerLevel(), upgradeLevel);
		float radius = def.getScaledRadius(upgradeLevel);
		AABB area = remnant.getBoundingBox().inflate(radius);
		List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, area,
				e -> e != remnant && e.isAlive() && !(e instanceof RemnantEntity));

		switch (def.getId()) {
			// ── Black Flame: Domain of Ash ──
			case "blackflame_domain_of_ash" -> {
				for (LivingEntity e : enemies) {
					e.igniteForSeconds(2.0f);
					e.hurtServer(level, remnant.damageSources().magic(), 2.0f * pm);
				}
				// Cook nearby items
				CyclingManager.cookNearbyItems(level, area);
			}

			// ── Black Flame: Scorched Earth ──
			case "blackflame_scorched_earth" -> {
				for (LivingEntity e : enemies) {
					e.igniteForSeconds(4.0f);
					e.hurtServer(level, remnant.damageSources().magic(), 4.0f * pm);
				}
				CyclingManager.cookNearbyItems(level, area);
			}

			// ── Endless Sword: Field of Blades ──
			case "endless_field_of_blades" -> {
				float damage = 1.5f * pm;
				for (LivingEntity e : enemies) {
					if (e.getDeltaMovement().horizontalDistance() > 0.01) {
						e.hurtServer(level, remnant.damageSources().magic(), damage);
					}
				}
			}

			// ── Endless Sword: Blade Barrier ──
			case "endless_blade_barrier" -> {
				float damage = 3.0f * pm;
				for (LivingEntity e : enemies) {
					e.hurtServer(level, remnant.damageSources().magic(), damage);
				}
			}

			// ── Stellar Spear: Spear Domain ──
			case "stellar_spear_domain" -> {
				float damage = 2.0f * pm;
				for (LivingEntity e : enemies) {
					Vec3 toRemnant = remnant.position().subtract(e.position()).normalize();
					Vec3 movement = e.getDeltaMovement().normalize();
					if (toRemnant.x * movement.x + toRemnant.z * movement.z > 0.3) {
						e.hurtServer(level, remnant.damageSources().magic(), damage);
					}
				}
			}

			// ── Stellar Spear: Constellation ──
			case "stellar_constellation" -> {
				float damage = 3.0f * pm;
				for (LivingEntity e : enemies) {
					e.hurtServer(level, remnant.damageSources().magic(), damage);
				}
			}

			// ── Cloud Hammer: Gravity Field ──
			case "cloud_gravity_field" -> {
				boolean underlordPlus = pm >= 1.5f;
				int amp = underlordPlus ? 2 : 1;
				for (LivingEntity e : enemies) {
					e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, amp, false, false));
					e.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20, 128, false, false));
				}
			}

			// ── Cloud Hammer: Vortex ──
			case "cloud_vortex" -> {
				for (LivingEntity e : enemies) {
					e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 2, false, false));
					e.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20, 128, false, false));
					// Pull toward remnant
					Vec3 pull = remnant.position().subtract(e.position()).normalize().scale(0.15 * pm);
					e.setDeltaMovement(e.getDeltaMovement().add(pull));
					e.hurtServer(level, remnant.damageSources().magic(), 2.0f * pm);
				}
			}

			// ── Hollow King: Hollow Domain ──
			case "hollow_domain" -> {
				boolean underlordPlus = pm >= 1.5f;
				int weakAmp = underlordPlus ? 1 : 0;
				for (LivingEntity e : enemies) {
					e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, weakAmp, false, false));
				}
				refreshEffect(remnant, MobEffects.RESISTANCE, 20, weakAmp);
			}

			// ── Hollow King: Suppression Field ──
			case "hollow_suppression_field" -> {
				for (LivingEntity e : enemies) {
					e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2, false, false));
					e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
				}
				refreshEffect(remnant, MobEffects.RESISTANCE, 40, 1);
			}

			// ── Universal: Madra Shield ──
			case "universal_madra_shield" -> {
				refreshEffect(remnant, MobEffects.ABSORPTION, 40, (int) Math.min(2 * pm, 4));
			}

			default -> {
				// Unknown ruler — apply generic slowness to enemies
				for (LivingEntity e : enemies) {
					e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 0, false, false));
				}
			}
		}
	}

	/**
	 * Returns the ruler effect interval in ticks.
	 */
	public static int getRulerEffectInterval() {
		return RULER_EFFECT_INTERVAL;
	}

	// ══════════════════════════════════════════════════════════════════════
	// HELPERS
	// ══════════════════════════════════════════════════════════════════════

	/**
	 * Computes the effective ability power multiplier for a given power level
	 * and upgrade level. Mirrors CradlePlayerData.getAbilityPowerMultiplier()
	 * combined with AbilityDefinitions.effectivePower().
	 */
	public static float effectivePower(int powerLevel, int upgradeLevel) {
		return getPowerMultiplier(powerLevel) * (1.0f + 0.05f * (upgradeLevel - 1));
	}

	/**
	 * Maps a remnant's power level to the same ability power multiplier
	 * that a player at the equivalent advancement stage would have.
	 * Power level ordinals match AdvancementStage ordinals:
	 * 0=Foundation, 1=Copper, 2=Iron, 3=Jade, 4=LowGold, ...
	 */
	public static float getPowerMultiplier(int powerLevel) {
		return switch (powerLevel) {
			case 0, 1, 2 -> 1.0f;   // Foundation, Copper, Iron
			case 3 -> 1.05f;        // Jade
			case 4 -> 1.1f;         // Low Gold
			case 5 -> 1.15f;        // High Gold
			case 6 -> 1.25f;        // Truegold
			case 7 -> 1.5f;         // Underlord
			case 8 -> 1.8f;         // Overlord
			case 9 -> 2.0f;         // Archlord
			case 10, 11 -> 2.2f;    // Sage, Herald
			case 12 -> 2.5f;        // Monarch
			default -> 1.0f;
		};
	}

	/**
	 * Parses a path name string to the Path enum.
	 * Defaults to BLACK_FLAME if unknown.
	 */
	private static CradlePlayerData.Path parsePath(String pathName) {
		try {
			return CradlePlayerData.Path.valueOf(pathName);
		} catch (IllegalArgumentException e) {
			return CradlePlayerData.Path.BLACK_FLAME;
		}
	}

	/**
	 * Adds or updates a transient attribute modifier on any LivingEntity.
	 * Entity-aware version of AbilityDefinitions.ensureModifier().
	 */
	private static void ensureModifier(LivingEntity entity, Holder<Attribute> attribute,
	                                    Identifier id, double amount, AttributeModifier.Operation op) {
		var instance = entity.getAttribute(attribute);
		if (instance == null) return;
		AttributeModifier existing = instance.getModifier(id);
		if (existing != null) {
			if (existing.amount() == amount && existing.operation() == op) return;
			instance.removeModifier(id);
		}
		instance.addTransientModifier(new AttributeModifier(id, amount, op));
	}

	/**
	 * Refreshes a mob effect on any LivingEntity, only re-applying if the
	 * existing effect is about to expire.
	 * Entity-aware version of AbilityDefinitions.refreshEffect().
	 */
	private static void refreshEffect(LivingEntity entity,
	                                    net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
	                                    int duration, int amplifier) {
		if (!entity.hasEffect(effect) || entity.getEffect(effect).getDuration() < 10) {
			entity.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false));
		}
	}

	/**
	 * Removes all enforcer attribute modifiers from any LivingEntity.
	 * Entity-aware version of CyclingManager.removeEnforcerModifiers().
	 */
	@SuppressWarnings("unchecked")
	private static void removeEnforcerModifiers(LivingEntity entity) {
		Identifier[] ids = {
				ENFORCER_ATTACK_DAMAGE_ID, ENFORCER_SPEED_ID, ENFORCER_ATTACK_SPEED_ID,
				ENFORCER_ARMOR_ID, ENFORCER_KNOCKBACK_RESISTANCE_ID
		};
		Holder<Attribute>[] attrs = new Holder[]{
				Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED, Attributes.ATTACK_SPEED,
				Attributes.ARMOR, Attributes.KNOCKBACK_RESISTANCE, Attributes.ATTACK_KNOCKBACK
		};
		for (Identifier id : ids) {
			for (Holder<Attribute> attr : attrs) {
				var instance = entity.getAttribute(attr);
				if (instance != null && instance.getModifier(id) != null) {
					instance.removeModifier(id);
				}
			}
		}
	}
}

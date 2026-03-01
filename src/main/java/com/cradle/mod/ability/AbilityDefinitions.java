package com.cradle.mod.ability;

import com.cradle.mod.CradlePlayerData;
import com.cradle.mod.CradlePlayerData.AdvancementStage;
import com.cradle.mod.CradlePlayerData.Path;
import com.cradle.mod.CyclingManager;
import com.cradle.mod.entity.StrikerProjectileEntity;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registers all ability definitions.
 * Contains the behavior lambdas extracted from the old hardcoded switch statements
 * in CradleMod, CyclingManager, and StrikerProjectileEntity.
 */
public final class AbilityDefinitions {

	private AbilityDefinitions() {}

	// ── Shared constants (matching CyclingManager) ────────────────────
	private static final Identifier ENFORCER_ATTACK_DAMAGE_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_attack_damage");
	private static final Identifier ENFORCER_SPEED_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_speed");
	private static final Identifier ENFORCER_ATTACK_SPEED_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_attack_speed");
	private static final Identifier ENFORCER_ARMOR_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_armor");
	private static final Identifier ENFORCER_KNOCKBACK_RESISTANCE_ID = Identifier.fromNamespaceAndPath("cradlemod", "enforcer_knockback_resistance");

	// Burning Body strain tracking
	private static final Map<UUID, Integer> BURNING_BODY_STRAIN_TICKS = new HashMap<>();
	private static final int BURNING_BODY_STRAIN_INTERVAL = 40;
	private static final float BURNING_BODY_STRAIN_DAMAGE = 1.0f;

	// ── Helper methods ────────────────────────────────────────────────

	private static void ensureModifier(ServerPlayer player, Holder<Attribute> attribute,
	                                    Identifier id, double amount, AttributeModifier.Operation op) {
		var instance = player.getAttribute(attribute);
		if (instance == null) return;
		AttributeModifier existing = instance.getModifier(id);
		if (existing != null) {
			if (existing.amount() == amount && existing.operation() == op) return;
			instance.removeModifier(id);
		}
		instance.addTransientModifier(new AttributeModifier(id, amount, op));
	}

	private static void refreshEffect(ServerPlayer player,
	                                    net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
	                                    int duration, int amplifier) {
		if (!player.hasEffect(effect) || player.getEffect(effect).getDuration() < 10) {
			player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false));
		}
	}

	/**
	 * Calculates effective power multiplier including upgrade level scaling and charge boost.
	 * Formula: stageMultiplier * (1.0 + 0.05 * (upgradeLevel - 1)) * chargeMultiplier
	 * The chargeMultiplier is 1.0 for instant taps, up to 3.0 for fully charged strikers.
	 */
	private static float effectivePower(CradlePlayerData data, int upgradeLevel) {
		return data.getAbilityPowerMultiplier() * (1.0f + 0.05f * (upgradeLevel - 1))
				* data.getCurrentChargeMultiplier();
	}

	// ── Registration ──────────────────────────────────────────────────

	public static void registerAll() {
		registerBasicEnforcement();
		registerBlackFlame();
		registerEndlessSword();
		registerStellarSpear();
		registerCloudHammer();
		registerHollowKing();
		// Branch abilities (15 total — 3 per path)
		registerBlackFlameBranches();
		registerEndlessSwordBranches();
		registerStellarSpearBranches();
		registerCloudHammerBranches();
		registerHollowKingBranches();
		// Universal abilities (2 — any path, Underlord+)
		registerUniversals();
	}

	// ── BASIC ENFORCEMENT (Universal starter) ─────────────────────────

	private static void registerBasicEnforcement() {
		AbilityRegistry.register(
			AbilityDefinition.builder("basic_enforcement", "Basic Enforcement", AbilityType.ENFORCER)
				.unlockStage(AdvancementStage.FOUNDATION)
				.description("A fundamental enforcement technique taught to all sacred artists. Strengthens the body with madra.")
				.color(0xFFCCCCCC)
				.baseMadraCost(0.2f) // Low drain per tick
				.baseDamage(0f)
				.onEnforcerTick((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					// Small armor boost and slight speed increase
					ensureModifier(player, Attributes.ARMOR, ENFORCER_ARMOR_ID,
						1.0 * pm, AttributeModifier.Operation.ADD_VALUE);
					ensureModifier(player, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						0.01 * pm, AttributeModifier.Operation.ADD_VALUE);
				})
				.onEnforcerActivate((player, data, def, level) -> {
					player.displayClientMessage(Component.literal("\u00a77Basic Enforcement activated"), true);
				})
				.onEnforcerDeactivate((player, data, def, level) -> {
					CyclingManager.removeEnforcerModifiers(player);
					player.displayClientMessage(Component.literal("\u00a77Basic Enforcement deactivated"), true);
				})
				.build()
		);
	}

	// ── BLACK FLAME ───────────────────────────────────────────────────

	private static void registerBlackFlame() {
		// Enforcer: Burning Body
		AbilityRegistry.register(
			AbilityDefinition.builder("blackflame_burning_body", "Burning Body", AbilityType.ENFORCER)
				.path(Path.BLACK_FLAME)
				.unlockStage(AdvancementStage.COPPER)
				.description("Floods body with blackflame madra. +attack damage, +sprint speed, attacks ignite enemies. Burns the user over time.")
				.color(0xFFFF4400)
				.baseMadraCost(0.5f)
				.baseDamage(4.0f)
				.branchAt(10, "blackflame_inferno_form")
				.onEnforcerTick((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ensureModifier(player, Attributes.ATTACK_DAMAGE, ENFORCER_ATTACK_DAMAGE_ID,
						4.0 * pm, AttributeModifier.Operation.ADD_VALUE);
					ensureModifier(player, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						0.04 * pm, AttributeModifier.Operation.ADD_VALUE);
					refreshEffect(player, MobEffects.FIRE_RESISTANCE, 60, 0);

					// Self-damage strain (Blackflame burns its user)
					int strain = BURNING_BODY_STRAIN_TICKS.getOrDefault(player.getUUID(), 0) + 1;
					BURNING_BODY_STRAIN_TICKS.put(player.getUUID(), strain);
					if (strain >= BURNING_BODY_STRAIN_INTERVAL) {
						BURNING_BODY_STRAIN_TICKS.put(player.getUUID(), 0);
						player.hurtServer(player.level(), player.damageSources().magic(), BURNING_BODY_STRAIN_DAMAGE);
					}
				})
				.onEnforcerActivate((player, data, def, level) -> {
					player.displayClientMessage(Component.literal("\u00a7cBurning Body \u00a7aactivated"), true);
				})
				.onEnforcerDeactivate((player, data, def, level) -> {
					BURNING_BODY_STRAIN_TICKS.remove(player.getUUID());
					CyclingManager.removeEnforcerModifiers(player);
					player.displayClientMessage(Component.literal("\u00a7cBurning Body \u00a7cdeactivated"), true);
				})
				.build()
		);

		// Striker: Blackflame Burst
		AbilityRegistry.register(
			AbilityDefinition.builder("blackflame_burst", "Blackflame Burst", AbilityType.STRIKER)
				.path(Path.BLACK_FLAME)
				.unlockStage(AdvancementStage.COPPER)
				.description("Short-range explosive projectile. High damage in small area, applies burn. High madra cost.")
				.color(0xFFFF4400)
				.baseDamage(7.0f)
				.baseMadraCost(15.0f)
				.baseCooldownMs(2000L)
				.branchAt(10, "blackflame_meteor")
				.onStrikerFire((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ServerLevel slevel = player.level();
					Vec3 look = player.getLookAngle();
					StrikerProjectileEntity proj = new StrikerProjectileEntity(
						slevel, player, look.scale(1.5), data.getChosenPath(), pm);
					proj.setAbilityId(def.getId());
					proj.setPos(player.getEyePosition().add(look.scale(0.5)));
					slevel.addFreshEntity(proj);
				})
				.build()
		);

		// Ruler: Domain of Ash
		AbilityRegistry.register(
			AbilityDefinition.builder("blackflame_domain_of_ash", "Domain of Ash", AbilityType.RULER)
				.path(Path.BLACK_FLAME)
				.unlockStage(AdvancementStage.COPPER)
				.description("Area around player damages enemies over time. Enemies inside burn slowly. Area denial and pressure.")
				.color(0xFFFF4400)
				.baseMadraCost(0.4f)
				.baseRadius(6.0f)
				.baseDamage(2.0f)
				.branchAt(10, "blackflame_scorched_earth")
				.onRulerArea((player, data, def, level, enemies, slevel) -> {
					float pm = effectivePower(data, level);
					float damage = def.getScaledDamage(level) * data.getAbilityPowerMultiplier() / def.getBaseDamage();
					for (LivingEntity e : enemies) {
						e.igniteForSeconds(2.0f);
						e.hurtServer(slevel, player.damageSources().magic(), 2.0f * pm);
					}
					// Cook nearby items
					AABB area = player.getBoundingBox().inflate(def.getScaledRadius(level));
					CyclingManager.cookNearbyItems(slevel, area);
				})
				.build()
		);
	}

	// ── ENDLESS SWORD ─────────────────────────────────────────────────

	private static void registerEndlessSword() {
		// Enforcer: Flowing Edge
		AbilityRegistry.register(
			AbilityDefinition.builder("endless_flowing_edge", "Flowing Edge", AbilityType.ENFORCER)
				.path(Path.ENDLESS_SWORD)
				.unlockStage(AdvancementStage.COPPER)
				.description("+attack speed, reduced attack cooldown. Moving while attacking increases damage. Sustained combat rhythm.")
				.color(0xFFCCCCDD)
				.baseMadraCost(0.5f)
				.branchAt(10, "endless_thousand_cuts")
				.onEnforcerTick((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ensureModifier(player, Attributes.ATTACK_SPEED, ENFORCER_ATTACK_SPEED_ID,
						1.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				})
				.onEnforcerActivate((player, data, def, level) -> {
					player.displayClientMessage(Component.literal("\u00a7fFlowing Edge \u00a7aactivated"), true);
				})
				.onEnforcerDeactivate((player, data, def, level) -> {
					CyclingManager.removeEnforcerModifiers(player);
					player.displayClientMessage(Component.literal("\u00a7fFlowing Edge \u00a7cdeactivated"), true);
				})
				.build()
		);

		// Striker: Endless Slash
		AbilityRegistry.register(
			AbilityDefinition.builder("endless_slash", "Endless Slash", AbilityType.STRIKER)
				.path(Path.ENDLESS_SWORD)
				.unlockStage(AdvancementStage.COPPER)
				.description("Fast horizontal slash projectile. Medium range. Can hit multiple enemies in a line.")
				.color(0xFFCCCCDD)
				.baseDamage(8.0f)
				.baseMadraCost(15.0f)
				.baseCooldownMs(2000L)
				.branchAt(10, "endless_sword_storm")
				.onStrikerFire((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ServerLevel slevel = player.level();
					Vec3 look = player.getLookAngle();
					StrikerProjectileEntity proj = new StrikerProjectileEntity(
						slevel, player, look.scale(1.5), data.getChosenPath(), pm);
					proj.setAbilityId(def.getId());
					proj.setPos(player.getEyePosition().add(look.scale(0.5)));
					slevel.addFreshEntity(proj);
				})
				.build()
		);

		// Ruler: Field of Blades
		AbilityRegistry.register(
			AbilityDefinition.builder("endless_field_of_blades", "Field of Blades", AbilityType.RULER)
				.path(Path.ENDLESS_SWORD)
				.unlockStage(AdvancementStage.COPPER)
				.description("Enemies moving near the player take small repeated damage. Encourages controlling space through movement.")
				.color(0xFFCCCCDD)
				.baseMadraCost(0.4f)
				.baseRadius(6.0f)
				.baseDamage(1.5f)
				.branchAt(10, "endless_blade_barrier")
				.onRulerArea((player, data, def, level, enemies, slevel) -> {
					float pm = effectivePower(data, level);
					float damage = 1.5f * pm;
					for (LivingEntity e : enemies) {
						if (e.getDeltaMovement().horizontalDistance() > 0.01) {
							e.hurtServer(slevel, player.damageSources().magic(), damage);
						}
					}
				})
				.build()
		);
	}

	// ── STELLAR SPEAR ─────────────────────────────────────────────────

	private static void registerStellarSpear() {
		// Enforcer: Stellar Alignment
		AbilityRegistry.register(
			AbilityDefinition.builder("stellar_alignment", "Stellar Alignment", AbilityType.ENFORCER)
				.path(Path.STELLAR_SPEAR)
				.unlockStage(AdvancementStage.COPPER)
				.description("+forward speed, increased reach. Bonus damage while sprinting forward. Reduced knockback taken.")
				.color(0xFFFFDD44)
				.baseMadraCost(0.5f)
				.branchAt(10, "stellar_lightspeed")
				.onEnforcerTick((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ensureModifier(player, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						0.04 * pm, AttributeModifier.Operation.ADD_VALUE);
					ensureModifier(player, Attributes.KNOCKBACK_RESISTANCE, ENFORCER_KNOCKBACK_RESISTANCE_ID,
						Math.min(0.6 * pm, 1.0), AttributeModifier.Operation.ADD_VALUE);
				})
				.onEnforcerActivate((player, data, def, level) -> {
					player.displayClientMessage(Component.literal("\u00a7eStellar Alignment \u00a7aactivated"), true);
				})
				.onEnforcerDeactivate((player, data, def, level) -> {
					CyclingManager.removeEnforcerModifiers(player);
					player.displayClientMessage(Component.literal("\u00a7eStellar Alignment \u00a7cdeactivated"), true);
				})
				.build()
		);

		// Striker: Piercing Star
		AbilityRegistry.register(
			AbilityDefinition.builder("stellar_piercing_star", "Piercing Star", AbilityType.STRIKER)
				.path(Path.STELLAR_SPEAR)
				.unlockStage(AdvancementStage.COPPER)
				.description("Long-range piercing projectile. Passes through enemies. Damage slightly reduced per target hit.")
				.color(0xFFFFDD44)
				.baseDamage(9.0f)
				.baseMadraCost(15.0f)
				.baseCooldownMs(2000L)
				.branchAt(10, "stellar_nova")
				.onStrikerFire((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ServerLevel slevel = player.level();
					Vec3 look = player.getLookAngle();
					StrikerProjectileEntity proj = new StrikerProjectileEntity(
						slevel, player, look.scale(1.5), data.getChosenPath(), pm);
					proj.setAbilityId(def.getId());
					proj.setPos(player.getEyePosition().add(look.scale(0.5)));
					slevel.addFreshEntity(proj);
				})
				.build()
		);

		// Ruler: Spear Domain
		AbilityRegistry.register(
			AbilityDefinition.builder("stellar_spear_domain", "Spear Domain", AbilityType.RULER)
				.path(Path.STELLAR_SPEAR)
				.unlockStage(AdvancementStage.COPPER)
				.description("Enemies moving directly toward the player take damage. Rewards positioning and facing enemies.")
				.color(0xFFFFDD44)
				.baseMadraCost(0.4f)
				.baseRadius(6.0f)
				.baseDamage(2.0f)
				.branchAt(10, "stellar_constellation")
				.onRulerArea((player, data, def, level, enemies, slevel) -> {
					float pm = effectivePower(data, level);
					float damage = 2.0f * pm;
					for (LivingEntity e : enemies) {
						Vec3 toPlayer = player.position().subtract(e.position()).normalize();
						Vec3 movement = e.getDeltaMovement().normalize();
						if (toPlayer.x * movement.x + toPlayer.z * movement.z > 0.3) {
							e.hurtServer(slevel, player.damageSources().magic(), damage);
						}
					}
				})
				.build()
		);
	}

	// ── CLOUD HAMMER ──────────────────────────────────────────────────

	private static void registerCloudHammer() {
		// Enforcer: Thunderous Weight
		AbilityRegistry.register(
			AbilityDefinition.builder("cloud_thunderous_weight", "Thunderous Weight", AbilityType.ENFORCER)
				.path(Path.CLOUD_HAMMER)
				.unlockStage(AdvancementStage.COPPER)
				.description("+armor, +knockback dealt. Reduced movement speed. Charged attacks deal bonus damage. Tank and heavy hitter.")
				.color(0xFF888899)
				.baseMadraCost(0.5f)
				.branchAt(10, "cloud_living_fortress")
				.onEnforcerTick((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ensureModifier(player, Attributes.ARMOR, ENFORCER_ARMOR_ID,
						6.0 * pm, AttributeModifier.Operation.ADD_VALUE);
					ensureModifier(player, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						-0.03 + (pm - 1.0) * 0.01, AttributeModifier.Operation.ADD_VALUE);
					ensureModifier(player, Attributes.ATTACK_KNOCKBACK, ENFORCER_KNOCKBACK_RESISTANCE_ID,
						2.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				})
				.onEnforcerActivate((player, data, def, level) -> {
					player.displayClientMessage(Component.literal("\u00a78Thunderous Weight \u00a7aactivated"), true);
				})
				.onEnforcerDeactivate((player, data, def, level) -> {
					CyclingManager.removeEnforcerModifiers(player);
					player.displayClientMessage(Component.literal("\u00a78Thunderous Weight \u00a7cdeactivated"), true);
				})
				.build()
		);

		// Striker: Falling Hammer
		AbilityRegistry.register(
			AbilityDefinition.builder("cloud_falling_hammer", "Falling Hammer", AbilityType.STRIKER)
				.path(Path.CLOUD_HAMMER)
				.unlockStage(AdvancementStage.COPPER)
				.description("Delayed area strike from above. High knockback. Strong single impact. Area burst damage.")
				.color(0xFF888899)
				.baseDamage(9.0f)
				.baseMadraCost(15.0f)
				.baseCooldownMs(2000L)
				.branchAt(10, "cloud_thunderstrike")
				.onStrikerFire((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ServerLevel slevel = player.level();
					Vec3 look = player.getLookAngle();
					StrikerProjectileEntity proj = new StrikerProjectileEntity(
						slevel, player, look.scale(1.5), data.getChosenPath(), pm);
					proj.setAbilityId(def.getId());
					proj.setPos(player.getEyePosition().add(look.scale(0.5)));
					slevel.addFreshEntity(proj);
				})
				.build()
		);

		// Ruler: Gravity Field
		AbilityRegistry.register(
			AbilityDefinition.builder("cloud_gravity_field", "Gravity Field", AbilityType.RULER)
				.path(Path.CLOUD_HAMMER)
				.unlockStage(AdvancementStage.COPPER)
				.description("Enemies inside area move slower, jump height reduced. Easier to control groups. Crowd control.")
				.color(0xFF888899)
				.baseMadraCost(0.4f)
				.baseRadius(6.0f)
				.branchAt(10, "cloud_vortex")
				.onRulerArea((player, data, def, level, enemies, slevel) -> {
					float pm = effectivePower(data, level);
					boolean underlordPlus = pm >= 1.5f;
					int amp = underlordPlus ? 2 : 1;
					for (LivingEntity e : enemies) {
						e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, amp, false, false));
						e.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20, 128, false, false));
					}
				})
				.build()
		);
	}

	// ── HOLLOW KING ───────────────────────────────────────────────────

	private static void registerHollowKing() {
		// Enforcer: Hollow Circulation
		AbilityRegistry.register(
			AbilityDefinition.builder("hollow_circulation", "Hollow Circulation", AbilityType.ENFORCER)
				.path(Path.HOLLOW_KING)
				.unlockStage(AdvancementStage.COPPER)
				.description("Reduced madra cost for abilities, small passive madra regen, slight damage reduction. Efficiency and control.")
				.color(0xFF220033)
				.baseMadraCost(0.2f) // Lower drain — Hollow King is efficient
				.branchAt(10, "hollow_void_body")
				.onEnforcerTick((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					refreshEffect(player, MobEffects.RESISTANCE, 60, pm >= 1.5f ? 1 : 0);
				})
				.onEnforcerActivate((player, data, def, level) -> {
					player.displayClientMessage(Component.literal("\u00a75Hollow Circulation \u00a7aactivated"), true);
				})
				.onEnforcerDeactivate((player, data, def, level) -> {
					CyclingManager.removeEnforcerModifiers(player);
					player.removeEffect(MobEffects.RESISTANCE);
					player.displayClientMessage(Component.literal("\u00a75Hollow Circulation \u00a7cdeactivated"), true);
				})
				.build()
		);

		// Striker: Empty Palm
		AbilityRegistry.register(
			AbilityDefinition.builder("hollow_empty_palm", "Empty Palm", AbilityType.STRIKER)
				.path(Path.HOLLOW_KING)
				.unlockStage(AdvancementStage.COPPER)
				.description("Short-range shockwave. Moderate damage. Temporarily weakens enemy attacks or armor. Control and disruption.")
				.color(0xFF220033)
				.baseDamage(7.0f)
				.baseMadraCost(15.0f)
				.baseCooldownMs(2000L)
				.branchAt(10, "hollow_nullify")
				.onStrikerFire((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ServerLevel slevel = player.level();
					Vec3 look = player.getLookAngle();
					StrikerProjectileEntity proj = new StrikerProjectileEntity(
						slevel, player, look.scale(1.5), data.getChosenPath(), pm);
					proj.setAbilityId(def.getId());
					proj.setPos(player.getEyePosition().add(look.scale(0.5)));
					slevel.addFreshEntity(proj);
				})
				.build()
		);

		// Ruler: Hollow Domain
		AbilityRegistry.register(
			AbilityDefinition.builder("hollow_domain", "Hollow Domain", AbilityType.RULER)
				.path(Path.HOLLOW_KING)
				.unlockStage(AdvancementStage.COPPER)
				.description("Reduces incoming damage in area, slows enemy ability effects, improves allied madra efficiency. Defensive control.")
				.color(0xFF220033)
				.baseMadraCost(0.4f)
				.baseRadius(6.0f)
				.branchAt(10, "hollow_suppression_field")
				.onRulerArea((player, data, def, level, enemies, slevel) -> {
					float pm = effectivePower(data, level);
					boolean underlordPlus = pm >= 1.5f;
					int weakAmp = underlordPlus ? 1 : 0;
					for (LivingEntity e : enemies) {
						e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, weakAmp, false, false));
					}
					refreshEffect(player, MobEffects.RESISTANCE, 20, weakAmp);
				})
				.build()
		);
	}

	// ══════════════════════════════════════════════════════════════════
	// BRANCH ABILITIES (15 total — unlocked at upgrade level 10 on parent)
	// Each is a more powerful, specialized version of the original ability.
	// ══════════════════════════════════════════════════════════════════

	// ── BLACK FLAME BRANCHES ─────────────────────────────────────────

	private static void registerBlackFlameBranches() {
		// Branch of Burning Body → Inferno Form
		AbilityRegistry.register(
			AbilityDefinition.builder("blackflame_inferno_form", "Inferno Form", AbilityType.ENFORCER)
				.path(Path.BLACK_FLAME)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Burning Body. Massive damage boost, fire immunity, AoE ignition aura. Extreme madra drain and self-damage.")
				.color(0xFFFF2200)
				.baseMadraCost(0.8f)
				.baseDamage(8.0f)
				.onEnforcerTick((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ensureModifier(player, Attributes.ATTACK_DAMAGE, ENFORCER_ATTACK_DAMAGE_ID,
						8.0 * pm, AttributeModifier.Operation.ADD_VALUE);
					ensureModifier(player, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						0.06 * pm, AttributeModifier.Operation.ADD_VALUE);
					refreshEffect(player, MobEffects.FIRE_RESISTANCE, 60, 0);

					// Ignition aura — nearby enemies catch fire
					if (player.level() instanceof ServerLevel slevel) {
						AABB aura = player.getBoundingBox().inflate(3.0);
						List<LivingEntity> nearby = slevel.getEntitiesOfClass(LivingEntity.class, aura,
							e -> e != player && e.isAlive());
						for (LivingEntity e : nearby) {
							e.igniteForSeconds(3.0f);
						}
					}

					// Heavy self-strain
					int strain = BURNING_BODY_STRAIN_TICKS.getOrDefault(player.getUUID(), 0) + 1;
					BURNING_BODY_STRAIN_TICKS.put(player.getUUID(), strain);
					if (strain >= 30) { // Faster self-damage than base
						BURNING_BODY_STRAIN_TICKS.put(player.getUUID(), 0);
						player.hurtServer(player.level(), player.damageSources().magic(), 1.5f);
					}
				})
				.onEnforcerActivate((player, data, def, level) -> {
					player.displayClientMessage(Component.literal("\u00a7c\u00a7lInferno Form \u00a7aactivated"), true);
				})
				.onEnforcerDeactivate((player, data, def, level) -> {
					BURNING_BODY_STRAIN_TICKS.remove(player.getUUID());
					CyclingManager.removeEnforcerModifiers(player);
					player.displayClientMessage(Component.literal("\u00a7c\u00a7lInferno Form \u00a7cdeactivated"), true);
				})
				.build()
		);

		// Branch of Blackflame Burst → Meteor
		AbilityRegistry.register(
			AbilityDefinition.builder("blackflame_meteor", "Meteor", AbilityType.STRIKER)
				.path(Path.BLACK_FLAME)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Blackflame Burst. Massive arcing projectile that creates an explosion on impact. Devastating area damage.")
				.color(0xFFFF2200)
				.baseDamage(14.0f)
				.baseMadraCost(25.0f)
				.baseCooldownMs(4000L)
				.onStrikerFire((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ServerLevel slevel = player.level();
					Vec3 look = player.getLookAngle();
					StrikerProjectileEntity proj = new StrikerProjectileEntity(
						slevel, player, look.scale(1.2), data.getChosenPath(), pm * 1.5f);
					proj.setAbilityId(def.getId());
					proj.setPos(player.getEyePosition().add(look.scale(0.5)));
					slevel.addFreshEntity(proj);
				})
				.build()
		);

		// Branch of Domain of Ash → Scorched Earth
		AbilityRegistry.register(
			AbilityDefinition.builder("blackflame_scorched_earth", "Scorched Earth", AbilityType.RULER)
				.path(Path.BLACK_FLAME)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Domain of Ash. Massive radius, higher damage, enemies inside take increasing burn stacks.")
				.color(0xFFFF2200)
				.baseMadraCost(0.6f)
				.baseRadius(10.0f)
				.baseDamage(4.0f)
				.onRulerArea((player, data, def, level, enemies, slevel) -> {
					float pm = effectivePower(data, level);
					for (LivingEntity e : enemies) {
						e.igniteForSeconds(4.0f);
						e.hurtServer(slevel, player.damageSources().magic(), 4.0f * pm);
					}
					AABB area = player.getBoundingBox().inflate(def.getScaledRadius(level));
					CyclingManager.cookNearbyItems(slevel, area);
				})
				.build()
		);
	}

	// ── ENDLESS SWORD BRANCHES ───────────────────────────────────────

	private static void registerEndlessSwordBranches() {
		// Branch of Flowing Edge → Thousand Cuts
		AbilityRegistry.register(
			AbilityDefinition.builder("endless_thousand_cuts", "Thousand Cuts", AbilityType.ENFORCER)
				.path(Path.ENDLESS_SWORD)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Flowing Edge. Extreme attack speed, each consecutive hit deals increasing damage. Relentless assault.")
				.color(0xFFDDDDFF)
				.baseMadraCost(0.6f)
				.onEnforcerTick((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ensureModifier(player, Attributes.ATTACK_SPEED, ENFORCER_ATTACK_SPEED_ID,
						2.0 * pm, AttributeModifier.Operation.ADD_VALUE);
					ensureModifier(player, Attributes.ATTACK_DAMAGE, ENFORCER_ATTACK_DAMAGE_ID,
						3.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				})
				.onEnforcerActivate((player, data, def, level) -> {
					player.displayClientMessage(Component.literal("\u00a7f\u00a7lThousand Cuts \u00a7aactivated"), true);
				})
				.onEnforcerDeactivate((player, data, def, level) -> {
					CyclingManager.removeEnforcerModifiers(player);
					player.displayClientMessage(Component.literal("\u00a7f\u00a7lThousand Cuts \u00a7cdeactivated"), true);
				})
				.build()
		);

		// Branch of Endless Slash → Sword Storm
		AbilityRegistry.register(
			AbilityDefinition.builder("endless_sword_storm", "Sword Storm", AbilityType.STRIKER)
				.path(Path.ENDLESS_SWORD)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Endless Slash. Fires a rapid barrage of 3 sword projectiles in a spread pattern.")
				.color(0xFFDDDDFF)
				.baseDamage(6.0f)
				.baseMadraCost(30.0f)
				.baseCooldownMs(3500L)
				.onStrikerFire((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ServerLevel slevel = player.level();
					Vec3 look = player.getLookAngle();
					// Fire 3 projectiles in a spread
					for (int i = -1; i <= 1; i++) {
						Vec3 dir = look.yRot((float) (i * 0.15)).scale(1.5);
						StrikerProjectileEntity proj = new StrikerProjectileEntity(
							slevel, player, dir, data.getChosenPath(), pm);
						proj.setAbilityId(def.getId());
						proj.setPos(player.getEyePosition().add(look.scale(0.5)));
						slevel.addFreshEntity(proj);
					}
				})
				.build()
		);

		// Branch of Field of Blades → Blade Barrier
		AbilityRegistry.register(
			AbilityDefinition.builder("endless_blade_barrier", "Blade Barrier", AbilityType.RULER)
				.path(Path.ENDLESS_SWORD)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Field of Blades. All nearby enemies take constant damage regardless of movement. Reflects projectile damage.")
				.color(0xFFDDDDFF)
				.baseMadraCost(0.6f)
				.baseRadius(8.0f)
				.baseDamage(3.0f)
				.onRulerArea((player, data, def, level, enemies, slevel) -> {
					float pm = effectivePower(data, level);
					float damage = 3.0f * pm;
					for (LivingEntity e : enemies) {
						e.hurtServer(slevel, player.damageSources().magic(), damage);
					}
				})
				.build()
		);
	}

	// ── STELLAR SPEAR BRANCHES ───────────────────────────────────────

	private static void registerStellarSpearBranches() {
		// Branch of Stellar Alignment → Lightspeed
		AbilityRegistry.register(
			AbilityDefinition.builder("stellar_lightspeed", "Lightspeed", AbilityType.ENFORCER)
				.path(Path.STELLAR_SPEAR)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Stellar Alignment. Extreme speed, near-total knockback immunity, sprint attacks deal massive bonus damage.")
				.color(0xFFFFEE66)
				.baseMadraCost(0.6f)
				.onEnforcerTick((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ensureModifier(player, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						0.08 * pm, AttributeModifier.Operation.ADD_VALUE);
					ensureModifier(player, Attributes.KNOCKBACK_RESISTANCE, ENFORCER_KNOCKBACK_RESISTANCE_ID,
						Math.min(0.9 * pm, 1.0), AttributeModifier.Operation.ADD_VALUE);
					// Sprint damage bonus
					if (player.isSprinting()) {
						ensureModifier(player, Attributes.ATTACK_DAMAGE, ENFORCER_ATTACK_DAMAGE_ID,
							5.0 * pm, AttributeModifier.Operation.ADD_VALUE);
					}
				})
				.onEnforcerActivate((player, data, def, level) -> {
					player.displayClientMessage(Component.literal("\u00a7e\u00a7lLightspeed \u00a7aactivated"), true);
				})
				.onEnforcerDeactivate((player, data, def, level) -> {
					CyclingManager.removeEnforcerModifiers(player);
					player.displayClientMessage(Component.literal("\u00a7e\u00a7lLightspeed \u00a7cdeactivated"), true);
				})
				.build()
		);

		// Branch of Piercing Star → Nova
		AbilityRegistry.register(
			AbilityDefinition.builder("stellar_nova", "Nova", AbilityType.STRIKER)
				.path(Path.STELLAR_SPEAR)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Piercing Star. Explodes on first impact into a burst of light, dealing massive AoE damage.")
				.color(0xFFFFEE66)
				.baseDamage(16.0f)
				.baseMadraCost(28.0f)
				.baseCooldownMs(4000L)
				.onStrikerFire((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ServerLevel slevel = player.level();
					Vec3 look = player.getLookAngle();
					StrikerProjectileEntity proj = new StrikerProjectileEntity(
						slevel, player, look.scale(2.0), data.getChosenPath(), pm * 1.5f);
					proj.setAbilityId(def.getId());
					proj.setPos(player.getEyePosition().add(look.scale(0.5)));
					slevel.addFreshEntity(proj);
				})
				.build()
		);

		// Branch of Spear Domain → Constellation
		AbilityRegistry.register(
			AbilityDefinition.builder("stellar_constellation", "Constellation", AbilityType.RULER)
				.path(Path.STELLAR_SPEAR)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Spear Domain. All enemies in range take damage. Allies gain speed. Larger radius.")
				.color(0xFFFFEE66)
				.baseMadraCost(0.6f)
				.baseRadius(10.0f)
				.baseDamage(3.0f)
				.onRulerArea((player, data, def, level, enemies, slevel) -> {
					float pm = effectivePower(data, level);
					float damage = 3.0f * pm;
					for (LivingEntity e : enemies) {
						e.hurtServer(slevel, player.damageSources().magic(), damage);
					}
					// Buff allies in range
					AABB area = player.getBoundingBox().inflate(def.getScaledRadius(level));
					List<ServerPlayer> allies = slevel.getEntitiesOfClass(ServerPlayer.class, area,
						p -> p != player && p.isAlive());
					for (ServerPlayer ally : allies) {
						refreshEffect(ally, MobEffects.SPEED, 40, 0);
					}
				})
				.build()
		);
	}

	// ── CLOUD HAMMER BRANCHES ────────────────────────────────────────

	private static void registerCloudHammerBranches() {
		// Branch of Thunderous Weight → Living Fortress
		AbilityRegistry.register(
			AbilityDefinition.builder("cloud_living_fortress", "Living Fortress", AbilityType.ENFORCER)
				.path(Path.CLOUD_HAMMER)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Thunderous Weight. Massive armor and damage, immovable. Knockback immunity. Walking siege engine.")
				.color(0xFFAAAABB)
				.baseMadraCost(0.6f)
				.onEnforcerTick((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ensureModifier(player, Attributes.ARMOR, ENFORCER_ARMOR_ID,
						12.0 * pm, AttributeModifier.Operation.ADD_VALUE);
					ensureModifier(player, Attributes.ATTACK_DAMAGE, ENFORCER_ATTACK_DAMAGE_ID,
						4.0 * pm, AttributeModifier.Operation.ADD_VALUE);
					ensureModifier(player, Attributes.KNOCKBACK_RESISTANCE, ENFORCER_KNOCKBACK_RESISTANCE_ID,
						1.0, AttributeModifier.Operation.ADD_VALUE);
					ensureModifier(player, Attributes.MOVEMENT_SPEED, ENFORCER_SPEED_ID,
						-0.04 + (pm - 1.0) * 0.01, AttributeModifier.Operation.ADD_VALUE);
					ensureModifier(player, Attributes.ATTACK_KNOCKBACK,
						Identifier.fromNamespaceAndPath("cradlemod", "enforcer_knockback"),
						4.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				})
				.onEnforcerActivate((player, data, def, level) -> {
					player.displayClientMessage(Component.literal("\u00a78\u00a7lLiving Fortress \u00a7aactivated"), true);
				})
				.onEnforcerDeactivate((player, data, def, level) -> {
					CyclingManager.removeEnforcerModifiers(player);
					var knockbackInst = player.getAttribute(Attributes.ATTACK_KNOCKBACK);
					if (knockbackInst != null) {
						knockbackInst.removeModifier(Identifier.fromNamespaceAndPath("cradlemod", "enforcer_knockback"));
					}
					player.displayClientMessage(Component.literal("\u00a78\u00a7lLiving Fortress \u00a7cdeactivated"), true);
				})
				.build()
		);

		// Branch of Falling Hammer → Thunderstrike
		AbilityRegistry.register(
			AbilityDefinition.builder("cloud_thunderstrike", "Thunderstrike", AbilityType.STRIKER)
				.path(Path.CLOUD_HAMMER)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Falling Hammer. Calls down lightning at the impact point. Massive knockback and stun.")
				.color(0xFFAAAABB)
				.baseDamage(15.0f)
				.baseMadraCost(28.0f)
				.baseCooldownMs(5000L)
				.onStrikerFire((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ServerLevel slevel = player.level();
					Vec3 look = player.getLookAngle();
					StrikerProjectileEntity proj = new StrikerProjectileEntity(
						slevel, player, look.scale(1.2), data.getChosenPath(), pm * 1.5f);
					proj.setAbilityId(def.getId());
					proj.setPos(player.getEyePosition().add(look.scale(0.5)));
					slevel.addFreshEntity(proj);
				})
				.build()
		);

		// Branch of Gravity Field → Vortex
		AbilityRegistry.register(
			AbilityDefinition.builder("cloud_vortex", "Vortex", AbilityType.RULER)
				.path(Path.CLOUD_HAMMER)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Gravity Field. Pulls enemies toward the center while slowing them. Crushing gravitational pressure.")
				.color(0xFFAAAABB)
				.baseMadraCost(0.6f)
				.baseRadius(8.0f)
				.baseDamage(2.0f)
				.onRulerArea((player, data, def, level, enemies, slevel) -> {
					float pm = effectivePower(data, level);
					for (LivingEntity e : enemies) {
						// Slow
						e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 2, false, false));
						e.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20, 128, false, false));
						// Pull toward player
						Vec3 pull = player.position().subtract(e.position()).normalize().scale(0.15 * pm);
						e.setDeltaMovement(e.getDeltaMovement().add(pull));
						e.hurtServer(slevel, player.damageSources().magic(), 2.0f * pm);
					}
				})
				.build()
		);
	}

	// ── HOLLOW KING BRANCHES ─────────────────────────────────────────

	private static void registerHollowKingBranches() {
		// Branch of Hollow Circulation → Void Body
		AbilityRegistry.register(
			AbilityDefinition.builder("hollow_void_body", "Void Body", AbilityType.ENFORCER)
				.path(Path.HOLLOW_KING)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Hollow Circulation. Near-zero madra cost, strong damage reduction, passive regen. The ultimate efficiency.")
				.color(0xFF330055)
				.baseMadraCost(0.1f)
				.onEnforcerTick((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					refreshEffect(player, MobEffects.RESISTANCE, 60, 1);
					refreshEffect(player, MobEffects.REGENERATION, 60, 0);
					ensureModifier(player, Attributes.ARMOR, ENFORCER_ARMOR_ID,
						4.0 * pm, AttributeModifier.Operation.ADD_VALUE);
				})
				.onEnforcerActivate((player, data, def, level) -> {
					player.displayClientMessage(Component.literal("\u00a75\u00a7lVoid Body \u00a7aactivated"), true);
				})
				.onEnforcerDeactivate((player, data, def, level) -> {
					CyclingManager.removeEnforcerModifiers(player);
					player.removeEffect(MobEffects.RESISTANCE);
					player.removeEffect(MobEffects.REGENERATION);
					player.displayClientMessage(Component.literal("\u00a75\u00a7lVoid Body \u00a7cdeactivated"), true);
				})
				.build()
		);

		// Branch of Empty Palm → Nullify
		AbilityRegistry.register(
			AbilityDefinition.builder("hollow_nullify", "Nullify", AbilityType.STRIKER)
				.path(Path.HOLLOW_KING)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Empty Palm. Shockwave that strips all positive effects from enemies and deals increased damage.")
				.color(0xFF330055)
				.baseDamage(12.0f)
				.baseMadraCost(22.0f)
				.baseCooldownMs(3500L)
				.onStrikerFire((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					ServerLevel slevel = player.level();
					Vec3 look = player.getLookAngle();
					StrikerProjectileEntity proj = new StrikerProjectileEntity(
						slevel, player, look.scale(1.5), data.getChosenPath(), pm * 1.2f);
					proj.setAbilityId(def.getId());
					proj.setPos(player.getEyePosition().add(look.scale(0.5)));
					slevel.addFreshEntity(proj);
				})
				.build()
		);

		// Branch of Hollow Domain → Suppression Field
		AbilityRegistry.register(
			AbilityDefinition.builder("hollow_suppression_field", "Suppression Field", AbilityType.RULER)
				.path(Path.HOLLOW_KING)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("Evolved Hollow Domain. Strong weakness + slowness to enemies. Major damage reduction for allies. Larger radius.")
				.color(0xFF330055)
				.baseMadraCost(0.5f)
				.baseRadius(10.0f)
				.onRulerArea((player, data, def, level, enemies, slevel) -> {
					float pm = effectivePower(data, level);
					for (LivingEntity e : enemies) {
						e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2, false, false));
						e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
					}
					refreshEffect(player, MobEffects.RESISTANCE, 40, 1);
					// Also buff nearby allies
					AABB area = player.getBoundingBox().inflate(def.getScaledRadius(level));
					List<ServerPlayer> allies = slevel.getEntitiesOfClass(ServerPlayer.class, area,
						p -> p != player && p.isAlive());
					for (ServerPlayer ally : allies) {
						refreshEffect(ally, MobEffects.RESISTANCE, 40, 0);
					}
				})
				.build()
		);
	}

	// ══════════════════════════════════════════════════════════════════
	// UNIVERSAL ABILITIES (2 — any path can learn at Underlord+)
	// ══════════════════════════════════════════════════════════════════

	private static void registerUniversals() {
		// Universal Ruler: Madra Shield
		AbilityRegistry.register(
			AbilityDefinition.builder("universal_madra_shield", "Madra Shield", AbilityType.RULER)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("A universal ruler technique. Creates a shield of pure madra that absorbs incoming damage. Any path can learn this.")
				.color(0xFF4488FF)
				.baseMadraCost(0.5f)
				.baseRadius(3.0f)
				.onRulerArea((player, data, def, level, enemies, slevel) -> {
					float pm = effectivePower(data, level);
					// Shield effect: grant absorption hearts
					refreshEffect(player, MobEffects.ABSORPTION, 40, (int) Math.min(2 * pm, 4));
				})
				.build()
		);

		// Universal Striker: Spirit Pulse
		AbilityRegistry.register(
			AbilityDefinition.builder("universal_spirit_pulse", "Spirit Pulse", AbilityType.STRIKER)
				.unlockStage(AdvancementStage.UNDERLORD)
				.description("A universal striker technique. AoE knockback shockwave centered on the player. Any path can learn this.")
				.color(0xFF44DDFF)
				.baseDamage(6.0f)
				.baseMadraCost(18.0f)
				.baseCooldownMs(3000L)
				.onStrikerFire((player, data, def, level) -> {
					float pm = effectivePower(data, level);
					if (player.level() instanceof ServerLevel slevel) {
						// AoE knockback centered on player
						float radius = 5.0f + level * 0.3f;
						AABB area = player.getBoundingBox().inflate(radius);
						List<LivingEntity> targets = slevel.getEntitiesOfClass(LivingEntity.class, area,
							e -> e != player && e.isAlive());
						for (LivingEntity e : targets) {
							Vec3 push = e.position().subtract(player.position()).normalize().scale(1.5 * pm);
							e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.4, push.z));
							e.hurtServer(slevel, player.damageSources().magic(), 6.0f * pm);
						}
					}
				})
				.build()
		);
	}
}

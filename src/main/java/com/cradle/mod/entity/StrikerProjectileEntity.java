package com.cradle.mod.entity;

import com.cradle.mod.CradlePlayerData;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * A single projectile entity used by all five Striker techniques.
 * Behavior varies based on the path stored in synched entity data.
 *
 * Paths:
 * - BLACK_FLAME (Blackflame Burst): Short range, explodes on hit, burn DoT, high damage
 * - ENDLESS_SWORD (Endless Slash): Medium range, pierces through multiple enemies in a line
 * - STELLAR_SPEAR (Piercing Star): Long range, pierces through enemies, slight damage falloff
 * - CLOUD_HAMMER (Falling Hammer): Delayed area strike from above, high knockback
 * - HOLLOW_KING (Empty Palm): Short range shockwave, weakens enemy
 */
public class StrikerProjectileEntity extends AbstractHurtingProjectile {

	// Synched so the client renderer can pick path-specific particles/colors
	private static final EntityDataAccessor<String> PATH =
			SynchedEntityData.defineId(StrikerProjectileEntity.class, EntityDataSerializers.STRING);

	private int ticksAlive = 0;
	private int pierceCount = 0;
	private float baseDamage = 8.0f;
	private float powerMultiplier = 1.0f;
	private String abilityId = ""; // Skill tree ability ID (empty = use old path-based behavior)

	// Max lifetime in ticks before auto-discard (prevents eternal projectiles)
	private static final int MAX_LIFETIME_TICKS = 100; // 5 seconds

	// Required 2-arg constructor for entity type registration
	public StrikerProjectileEntity(EntityType<? extends StrikerProjectileEntity> type, Level level) {
		super(type, level);
	}

	// Server-side spawning constructor
	public StrikerProjectileEntity(Level level, LivingEntity owner, Vec3 movement,
								   CradlePlayerData.Path path, float powerMult) {
		super(CradleEntities.STRIKER_PROJECTILE, owner, movement, level);
		this.entityData.set(PATH, path.name());
		this.powerMultiplier = powerMult;

		// Path-specific base tuning, scaled by powerMultiplier
		switch (path) {
			case BLACK_FLAME -> {
				this.baseDamage = 7.0f * powerMult;
				this.accelerationPower = 0.12;
			}
			case ENDLESS_SWORD -> {
				this.baseDamage = 8.0f * powerMult;
				this.accelerationPower = 0.15;
			}
			case STELLAR_SPEAR -> {
				this.baseDamage = 9.0f * powerMult;
				this.accelerationPower = 0.18;
			}
			case CLOUD_HAMMER -> {
				this.baseDamage = 9.0f * powerMult;
				this.accelerationPower = 0.10;
			}
			case HOLLOW_KING -> {
				this.baseDamage = 7.0f * powerMult;
				this.accelerationPower = 0.08;
			}
			default -> this.accelerationPower = 0.1;
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PATH, "BLACK_FLAME");
	}

	public String getPathName() {
		return this.entityData.get(PATH);
	}

	public String getAbilityId() {
		return abilityId;
	}

	public void setAbilityId(String abilityId) {
		this.abilityId = abilityId != null ? abilityId : "";
	}

	@Override
	public void tick() {
		super.tick();
		ticksAlive++;
		// Hollow King: shorter range (less time alive)
		int maxLife = "HOLLOW_KING".equals(getPathName()) ? 40 : MAX_LIFETIME_TICKS;
		if (ticksAlive > maxLife) {
			discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (!(level() instanceof ServerLevel serverLevel)) return;

		Entity target = result.getEntity();
		Entity owner = getOwner();

		// Don't damage the owner
		if (target == owner) return;

		DamageSource source = owner instanceof LivingEntity livingOwner
				? serverLevel.damageSources().mobProjectile(this, livingOwner)
				: serverLevel.damageSources().magic();

		String path = getPathName();

		switch (path) {
			case "BLACK_FLAME" -> {
				// Explosive burst — big AoE, sets everything on fire
				// Ignite BEFORE damage — ensures cooked food drops (like Fire Aspect)
				target.igniteForSeconds(4.0f);
				target.hurtServer(serverLevel, source, baseDamage);
				// Larger explosion radius for more splash
				AABB blastArea = target.getBoundingBox().inflate(4.0);
				List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(
						LivingEntity.class,
						blastArea,
						e -> e != owner && e != target && e.isAlive()
				);
				for (LivingEntity e : nearby) {
					e.igniteForSeconds(3.0f); // Ignite BEFORE damage
					e.hurtServer(serverLevel, source, baseDamage * 0.6f);
				}
				// Blackflame cooks any raw food items in the blast zone
				com.cradle.mod.CyclingManager.cookNearbyItems(serverLevel, blastArea);
				discard();
			}

			case "ENDLESS_SWORD" -> {
				// Pierce through multiple enemies in a line
				target.hurtServer(serverLevel, source, baseDamage);
				pierceCount++;
				int maxPierce = 3 + (int) ((powerMultiplier - 1.0f) * 4); // 3 at 1.0x, up to 9 at 2.5x
				if (pierceCount >= maxPierce) {
					discard();
				}
				// Don't discard — continue through
			}

			case "STELLAR_SPEAR" -> {
				// Long range piercing, slight damage reduction per hit
				float damage = baseDamage * (1.0f - 0.15f * pierceCount);
				target.hurtServer(serverLevel, source, Math.max(damage, 2.0f));
				pierceCount++;
				int maxPierce = 4 + (int) ((powerMultiplier - 1.0f) * 4); // 4 at 1.0x, up to 10 at 2.5x
				if (pierceCount >= maxPierce) {
					discard();
				}
			}

			case "CLOUD_HAMMER" -> {
				// High damage + massive knockback
				target.hurtServer(serverLevel, source, baseDamage);
				if (target instanceof LivingEntity living) {
					// Launch target upward and backward, scaled by power
					Vec3 knockback = getDeltaMovement().normalize().scale(1.5 + powerMultiplier * 0.5);
					living.push(knockback.x, 0.6, knockback.z);
				}
				discard();
			}

			case "HOLLOW_KING" -> {
				// Moderate damage + weaken enemy (Weakness effect)
				target.hurtServer(serverLevel, source, baseDamage);
				if (target instanceof LivingEntity living) {
					int weakDuration = (int) (100 * powerMultiplier); // 5s at 1.0x, up to 12.5s at 2.5x
					living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weakDuration, 0));
					living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, weakDuration, 0));
				}
				discard();
			}

			default -> {
				target.hurtServer(serverLevel, source, baseDamage);
				discard();
			}
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);
		if (!level().isClientSide()) {
			// Cloud Hammer: area damage on block hit
			if ("CLOUD_HAMMER".equals(getPathName()) && level() instanceof ServerLevel serverLevel) {
				Entity owner = getOwner();
				DamageSource source = owner instanceof LivingEntity livingOwner
						? serverLevel.damageSources().mobProjectile(this, livingOwner)
						: serverLevel.damageSources().magic();
				List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(
						LivingEntity.class,
						getBoundingBox().inflate(3.0),
						e -> e != owner && e.isAlive()
				);
				for (LivingEntity e : nearby) {
					e.hurtServer(serverLevel, source, baseDamage * 0.6f);
					if (e instanceof LivingEntity living) {
						living.push(0, 0.4, 0);
					}
				}
			}
			discard();
		}
	}

	@Override
	protected ParticleOptions getTrailParticle() {
		return switch (getPathName()) {
			case "BLACK_FLAME" -> ParticleTypes.FLAME;
			case "ENDLESS_SWORD" -> ParticleTypes.CRIT;
			case "STELLAR_SPEAR" -> ParticleTypes.END_ROD;
			case "CLOUD_HAMMER" -> ParticleTypes.CLOUD;
			case "HOLLOW_KING" -> ParticleTypes.ENCHANT;
			default -> ParticleTypes.SMOKE;
		};
	}

	@Override
	protected boolean shouldBurn() {
		return "BLACK_FLAME".equals(getPathName());
	}

	@Override
	protected float getInertia() {
		// Stellar Spear maintains speed longer
		if ("STELLAR_SPEAR".equals(getPathName())) return 0.99f;
		return 0.95f;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("Path", getPathName());
		output.putFloat("BaseDamage", baseDamage);
		output.putFloat("PowerMultiplier", powerMultiplier);
		output.putInt("PierceCount", pierceCount);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(PATH, input.getStringOr("Path", "BLACK_FLAME"));
		this.baseDamage = input.getFloatOr("BaseDamage", 8.0f);
		// Migration: old saves had "GoldStage" boolean, new saves use "PowerMultiplier" float
		this.powerMultiplier = input.getFloatOr("PowerMultiplier",
				input.getBooleanOr("GoldStage", false) ? 1.1f : 1.0f);
		this.pierceCount = input.getIntOr("PierceCount", 0);
	}
}

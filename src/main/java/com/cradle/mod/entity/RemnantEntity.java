package com.cradle.mod.entity;

import com.cradle.mod.BreakthroughManager;
import com.cradle.mod.CradleMod;
import com.cradle.mod.CradlePlayerData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * A Remnant is the ghostly spirit left behind when a sacred artist dies.
 * Remnants are hostile to all players and can be absorbed by compatible
 * sacred artists at Jade stage to advance to Lowgold.
 */
public class RemnantEntity extends Monster {

	// ── Synched entity data (visible to client for rendering) ──────────
	private static final EntityDataAccessor<String> REMNANT_PATH =
			SynchedEntityData.defineId(RemnantEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> POWER_LEVEL =
			SynchedEntityData.defineId(RemnantEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<String> SOURCE_MOB_TYPE =
			SynchedEntityData.defineId(RemnantEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Float> RENDER_SCALE =
			SynchedEntityData.defineId(RemnantEntity.class, EntityDataSerializers.FLOAT);

	// ── Server-only fields ────────────────────────────────────────────
	private int ticksAlive = 0;
	private UUID ownerUUID = null; // UUID of the player/entity that spawned this Remnant
	private boolean roaming = false; // True after camp phase — wanders killing hostiles

	// ── Ability AI state (player remnants only) ──────────────────────
	private com.cradle.mod.ability.PlayerLoadout storedLoadout = null;
	private float madraPool = 0f;
	private float maxMadraPool = 0f;
	private RemnantAbilityAI abilityAI = null;

	// ── Absorption channeling state ───────────────────────────────────
	private UUID absorbingPlayerUUID = null;
	private int absorbTicks = 0;
	private Vec3 absorbStartPos = null;

	// ── Constants ─────────────────────────────────────────────────────
	private static final int CAMP_DURATION_TICKS = 6000; // 5 minutes at death spot
	private static final int ABSORB_DURATION_TICKS = 60; // 3 seconds
	private static final double ABSORB_MAX_DISTANCE = 3.0; // Max distance during channeling

	// ── Constructors ──────────────────────────────────────────────────

	public RemnantEntity(EntityType<? extends RemnantEntity> type, Level level) {
		super(type, level);
	}

	// ── Synched data setup ────────────────────────────────────────────

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(REMNANT_PATH, "BLACK_FLAME");
		builder.define(POWER_LEVEL, 1);
		builder.define(SOURCE_MOB_TYPE, "minecraft:zombie");
		builder.define(RENDER_SCALE, 1.0f);
	}

	// ── Getters / Setters ─────────────────────────────────────────────

	public String getRemnantPathName() {
		return this.entityData.get(REMNANT_PATH);
	}

	public void setRemnantPath(CradlePlayerData.Path path) {
		this.entityData.set(REMNANT_PATH, path.name());
	}

	public int getPowerLevel() {
		return this.entityData.get(POWER_LEVEL);
	}

	public void setPowerLevel(int powerLevel) {
		this.entityData.set(POWER_LEVEL, Math.max(1, Math.min(powerLevel, 13)));
	}

	public UUID getOwnerUUID() {
		return ownerUUID;
	}

	public void setOwnerUUID(UUID ownerUUID) {
		this.ownerUUID = ownerUUID;
	}

	public String getSourceMobType() {
		return this.entityData.get(SOURCE_MOB_TYPE);
	}

	public void setSourceMobType(String mobType) {
		this.entityData.set(SOURCE_MOB_TYPE, mobType != null ? mobType : "minecraft:zombie");
		refreshDimensions();
	}

	public float getRenderScale() {
		return this.entityData.get(RENDER_SCALE);
	}

	public void setRenderScale(float scale) {
		this.entityData.set(RENDER_SCALE, Math.max(0.1f, scale));
	}

	public com.cradle.mod.ability.PlayerLoadout getStoredLoadout() { return storedLoadout; }
	public void setStoredLoadout(com.cradle.mod.ability.PlayerLoadout loadout) {
		this.storedLoadout = loadout;
		if (loadout != null) {
			this.abilityAI = new RemnantAbilityAI(this);
		} else {
			this.abilityAI = null;
		}
	}
	public float getMadraPool() { return madraPool; }
	public void setMadraPool(float madra) { this.madraPool = Math.max(0, madra); }
	public float getMaxMadraPool() { return maxMadraPool; }
	public void setMaxMadraPool(float max) { this.maxMadraPool = max; }

	/**
	 * Initialize this Remnant with path, power, owner, and source mob type.
	 */
	public void initRemnant(CradlePlayerData.Path path, int powerLevel, UUID ownerUUID, String sourceMobType) {
		setRemnantPath(path);
		setPowerLevel(powerLevel);
		this.ownerUUID = ownerUUID;
		setSourceMobType(sourceMobType);

		// Scale health based on power level (updated formula)
		float health = 20.0f + (powerLevel * 10.0f);
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
		this.setHealth(health);
		// Attack damage is now calculated dynamically via stage-relative scaling in doHurtTarget
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0);
	}

	/** Backward-compat overload — defaults to zombie source type. */
	public void initRemnant(CradlePlayerData.Path path, int powerLevel, UUID ownerUUID) {
		initRemnant(path, powerLevel, ownerUUID, "minecraft:zombie");
	}

	// ── AI Goals ──────────────────────────────────────────────────────

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

		// Hostile to ALL players
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	// ── Default attributes ────────────────────────────────────────────

	public static AttributeSupplier.Builder createRemnantAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.FOLLOW_RANGE, 24.0)
				.add(Attributes.ARMOR, 0.0);
	}

	// ── Tick ──────────────────────────────────────────────────────────

	@Override
	public void tick() {
		super.tick();

		if (!level().isClientSide()) {
			ticksAlive++;

			// After 5 minutes camped at death location, transition to roaming
			if (!roaming && ticksAlive >= CAMP_DURATION_TICKS) {
				transitionToRoaming();
			}

			// Handle absorption channeling
			tickAbsorption();

			// Tick ability AI for player remnants
			if (abilityAI != null && level() instanceof ServerLevel serverLevel) {
				abilityAI.tick(serverLevel);
			}
		}

		// Client-side: emit path-colored particles
		if (level().isClientSide()) {
			spawnAmbientParticles();
		}
	}

	/**
	 * Transition from camping at the death location to roaming mode.
	 * In roaming mode, the Remnant wanders aimlessly and attacks hostile mobs
	 * instead of players.
	 */
	private void transitionToRoaming() {
		roaming = true;

		// Clear all existing target goals and add hostile mob targeting
		this.targetSelector.removeAllGoals(goal -> true);
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true,
				(target, level) -> !(target instanceof RemnantEntity)));

		// Boost wander speed slightly since they're free-roaming now
		this.goalSelector.removeAllGoals(goal -> goal instanceof WaterAvoidingRandomStrollGoal);
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));

		CradleMod.LOGGER.debug("Remnant {} transitioned to roaming mode", this.getId());
	}

	// ── Absorption channeling ─────────────────────────────────────────

	private void tickAbsorption() {
		if (absorbingPlayerUUID == null) return;

		if (!(level() instanceof ServerLevel serverLevel)) return;

		ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(absorbingPlayerUUID);

		// Cancel if player left, died, or moved too far
		if (player == null || !player.isAlive() || player.distanceTo(this) > ABSORB_MAX_DISTANCE) {
			cancelAbsorption(player);
			return;
		}

		absorbTicks++;

		// Slow down the Remnant during channeling
		this.setDeltaMovement(Vec3.ZERO);
		this.getNavigation().stop();

		// Spawn channeling particles (Remnant -> Player)
		if (absorbTicks % 3 == 0) {
			Vec3 remnantPos = this.position().add(0, 1.0, 0);
			Vec3 playerPos = player.position().add(0, 1.0, 0);
			Vec3 direction = playerPos.subtract(remnantPos).normalize();
			int color = getPathColorARGB();
			for (int i = 0; i < 3; i++) {
				double t = (double) i / 3.0;
				double px = remnantPos.x + direction.x * t * 2.0;
				double py = remnantPos.y + direction.y * t * 2.0;
				double pz = remnantPos.z + direction.z * t * 2.0;
				serverLevel.sendParticles(
						new DustParticleOptions(color, 1.2f),
						px, py, pz, 1, 0.1, 0.1, 0.1, 0.02
				);
			}
		}

		// Show progress in action bar
		int percent = (int) ((float) absorbTicks / ABSORB_DURATION_TICKS * 100);
		player.displayClientMessage(
				Component.literal("\u00A76[Cradle] \u00A7dAbsorbing Remnant... " + percent + "%"), true);

		// Complete absorption
		if (absorbTicks >= ABSORB_DURATION_TICKS) {
			completeAbsorption(player);
		}
	}

	/**
	 * Called when a player right-clicks this Remnant.
	 */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		CradlePlayerData data = CradlePlayerData.getOrCreate(serverPlayer.getUUID());

		// Check if player is at Jade with sufficient level
		if (data.getAdvancementStage() != CradlePlayerData.AdvancementStage.JADE) {
			String msg;
			if (data.getAdvancementStage().ordinal() < CradlePlayerData.AdvancementStage.JADE.ordinal()) {
				msg = "\u00A76[Cradle] \u00A7cYou must reach Jade before you can absorb a Remnant.";
			} else {
				msg = "\u00A76[Cradle] \u00A7cYou have already advanced beyond Jade.";
			}
			serverPlayer.displayClientMessage(Component.literal(msg), true);
			return InteractionResult.CONSUME;
		}

		if (data.getPlayerLevel() < 100) {
			serverPlayer.displayClientMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cYou need level 100 to absorb a Remnant. (Current: " + data.getPlayerLevel() + ")"),
					true);
			return InteractionResult.CONSUME;
		}

		// Check path compatibility
		String remnantPath = getRemnantPathName();
		String playerPath = data.getChosenPath().name();
		if (!remnantPath.equals(playerPath)) {
			serverPlayer.displayClientMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cThis Remnant's madra is incompatible with your Path. You need a " +
							data.getChosenPath().displayName() + " Remnant."), true);
			return InteractionResult.CONSUME;
		}

		// Start or continue channeling
		if (absorbingPlayerUUID == null || !absorbingPlayerUUID.equals(serverPlayer.getUUID())) {
			absorbingPlayerUUID = serverPlayer.getUUID();
			absorbTicks = 0;
			absorbStartPos = serverPlayer.position();

			// Stop targeting this player during absorption
			this.setTarget(null);

			serverPlayer.displayClientMessage(Component.literal(
					"\u00A76[Cradle] \u00A7dBeginning Remnant absorption... hold still for 3 seconds."), true);
		}

		return InteractionResult.CONSUME;
	}

	private void completeAbsorption(ServerPlayer player) {
		CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

		// Set Goldsign based on path
		CradlePlayerData.Goldsign goldsign = CradlePlayerData.getGoldsignForPath(data.getChosenPath());
		data.setGoldsign(goldsign);

		// Broadcast goldsign to all players in the same level
		CradleMod.broadcastGoldsign(player, data);

		// Breakthrough to Low Gold
		BreakthroughManager.performRemnantBreakthrough(player, data);

		// Notify player
		player.displayClientMessage(Component.literal(
				"\u00A76[Cradle] \u00A76" + goldsign.loreDescription()), false);
		player.displayClientMessage(Component.literal(
				"\u00A76[Cradle] \u00A7aYou have advanced to \u00A7eLow Gold \u00A7athrough Remnant absorption!"), false);

		// Spawn burst particles at Remnant location
		if (level() instanceof ServerLevel serverLevel) {
			int color = getPathColorARGB();
			serverLevel.sendParticles(
					new DustParticleOptions(color, 2.0f),
					getX(), getY() + 1.0, getZ(),
					40, 1.0, 1.0, 1.0, 0.1
			);
		}

		// Remove the Remnant
		discard();
	}

	private void cancelAbsorption(ServerPlayer player) {
		if (player != null && player.isAlive()) {
			player.displayClientMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cRemnant absorption interrupted!"), true);
		}
		absorbingPlayerUUID = null;
		absorbTicks = 0;
		absorbStartPos = null;
	}

	// ── Stage-relative damage ────────────────────────────────────────

	@Override
	public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
		float damage = calculateStageDamage(target);
		boolean hit = target.hurtOrSimulate(this.damageSources().mobAttack(this), damage);
		if (hit && target instanceof LivingEntity living) {
			float knockback = 0.5f + (getPowerLevel() * 0.1f);
			living.knockback(knockback, Math.sin(this.getYRot() * Math.PI / 180.0),
					-Math.cos(this.getYRot() * Math.PI / 180.0));
		}
		return hit;
	}

	private float calculateStageDamage(net.minecraft.world.entity.Entity target) {
		int victimStage = 0;
		if (target instanceof ServerPlayer player) {
			CradlePlayerData data = CradlePlayerData.get(player.getUUID());
			if (data != null) {
				victimStage = data.getAdvancementStage().ordinal();
			}
		}
		int stageGap = getPowerLevel() - victimStage;
		if (stageGap >= 0) {
			return 2.0f + (stageGap * 4.5f);
		} else {
			return Math.max(1.0f, 2.0f + (stageGap * 0.5f));
		}
	}

	// ── Damage handling ───────────────────────────────────────────────

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// If the absorbing player takes damage, cancel absorption
		if (absorbingPlayerUUID != null && source.getEntity() instanceof LivingEntity) {
			ServerPlayer absorbingPlayer = level.getServer().getPlayerList().getPlayer(absorbingPlayerUUID);
			if (absorbingPlayer != null) {
				cancelAbsorption(absorbingPlayer);
			}
		}
		return super.hurtServer(level, source, amount);
	}

	// ── Particles ─────────────────────────────────────────────────────

	private void spawnAmbientParticles() {
		int color = getPathColorARGB();
		double x = getX() + (random.nextDouble() - 0.5) * 1.2;
		double y = getY() + random.nextDouble() * 1.8;
		double z = getZ() + (random.nextDouble() - 0.5) * 1.2;
		level().addParticle(
				new DustParticleOptions(color, 0.8f),
				x, y, z,
				0.0, 0.02, 0.0
		);
	}

	/**
	 * Returns the path-specific color as ARGB int for DustParticleOptions.
	 */
	public int getPathColorARGB() {
		return switch (getRemnantPathName()) {
			case "BLACK_FLAME" -> 0xFFFF4400;    // Dark orange/red
			case "ENDLESS_SWORD" -> 0xFFCCCCDE;  // Silver
			case "STELLAR_SPEAR" -> 0xFFFFDE44;  // Gold
			case "CLOUD_HAMMER" -> 0xFF8787CC;   // Pale blue
			case "HOLLOW_KING" -> 0xFFDEDEFF;    // Pale white
			default -> 0xFFB3B3B3;               // Gray
		};
	}

	/**
	 * Returns the path-specific color as Vector3f for the renderer's tint.
	 */
	public Vector3f getPathColor() {
		return switch (getRemnantPathName()) {
			case "BLACK_FLAME" -> new Vector3f(1.0f, 0.27f, 0.0f);    // Dark orange/red
			case "ENDLESS_SWORD" -> new Vector3f(0.8f, 0.8f, 0.87f);  // Silver
			case "STELLAR_SPEAR" -> new Vector3f(1.0f, 0.87f, 0.27f); // Gold
			case "CLOUD_HAMMER" -> new Vector3f(0.53f, 0.53f, 0.8f);  // Pale blue
			case "HOLLOW_KING" -> new Vector3f(0.87f, 0.87f, 1.0f);   // Pale white
			default -> new Vector3f(0.7f, 0.7f, 0.7f);                // Gray
		};
	}

	// ── Dynamic dimensions ───────────────────────────────────────────

	@Override
	public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
		float scale = getRenderScale();
		net.minecraft.world.entity.EntityDimensions base = getMobDimensions(getSourceMobType());
		if (scale != 1.0f) {
			return base.scale(scale);
		}
		return base;
	}

	private static net.minecraft.world.entity.EntityDimensions getMobDimensions(String mobType) {
		return switch (mobType) {
			case "minecraft:spider" -> net.minecraft.world.entity.EntityDimensions.scalable(1.4f, 0.9f);
			case "minecraft:enderman" -> net.minecraft.world.entity.EntityDimensions.scalable(0.6f, 2.9f);
			case "minecraft:creeper" -> net.minecraft.world.entity.EntityDimensions.scalable(0.6f, 1.7f);
			case "minecraft:skeleton" -> net.minecraft.world.entity.EntityDimensions.scalable(0.6f, 1.99f);
			case "minecraft:blaze" -> net.minecraft.world.entity.EntityDimensions.scalable(0.6f, 1.8f);
			case "minecraft:guardian" -> net.minecraft.world.entity.EntityDimensions.scalable(0.85f, 0.85f);
			case "minecraft:phantom" -> net.minecraft.world.entity.EntityDimensions.scalable(0.9f, 0.5f);
			case "minecraft:warden" -> net.minecraft.world.entity.EntityDimensions.scalable(0.9f, 2.9f);
			case "minecraft:ravager" -> net.minecraft.world.entity.EntityDimensions.scalable(1.95f, 2.2f);
			case "minecraft:iron_golem" -> net.minecraft.world.entity.EntityDimensions.scalable(1.4f, 2.7f);
			case "minecraft:cow", "minecraft:pig", "minecraft:sheep" ->
					net.minecraft.world.entity.EntityDimensions.scalable(0.9f, 1.4f);
			case "minecraft:chicken" -> net.minecraft.world.entity.EntityDimensions.scalable(0.4f, 0.7f);
			case "minecraft:wolf" -> net.minecraft.world.entity.EntityDimensions.scalable(0.6f, 0.85f);
			case "minecraft:horse" -> net.minecraft.world.entity.EntityDimensions.scalable(1.4f, 1.6f);
			default -> net.minecraft.world.entity.EntityDimensions.scalable(0.6f, 1.8f);
		};
	}

	// ── Misc overrides ────────────────────────────────────────────────

	@Override
	public boolean isPushable() {
		return false; // Ghosts can't be pushed
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false; // Never despawn from distance — Remnants persist until killed
	}

	public boolean isRoaming() {
		return roaming;
	}

	@Override
	public boolean isOnFire() {
		return false; // Ghosts don't burn
	}

	// ── NBT Persistence ───────────────────────────────────────────────

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("RemnantPath", getRemnantPathName());
		output.putInt("PowerLevel", getPowerLevel());
		output.putInt("TicksAlive", ticksAlive);
		output.putBoolean("Roaming", roaming);
		if (ownerUUID != null) {
			output.putString("OwnerUUID", ownerUUID.toString());
		}
		output.putString("SourceMobType", getSourceMobType());
		output.putFloat("RenderScale", getRenderScale());
		output.putFloat("MadraPool", madraPool);
		output.putFloat("MaxMadraPool", maxMadraPool);
		if (storedLoadout != null) {
			output.store("StoredLoadout", net.minecraft.nbt.CompoundTag.CODEC, storedLoadout.toNbt());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(REMNANT_PATH, input.getStringOr("RemnantPath", "BLACK_FLAME"));
		this.entityData.set(POWER_LEVEL, input.getIntOr("PowerLevel", 1));
		this.ticksAlive = input.getIntOr("TicksAlive", 0);

		String ownerStr = input.getStringOr("OwnerUUID", "");
		if (!ownerStr.isEmpty()) {
			try {
				this.ownerUUID = UUID.fromString(ownerStr);
			} catch (IllegalArgumentException e) {
				this.ownerUUID = null;
			}
		}

		// Restore roaming state — re-apply roaming AI goals if needed
		boolean wasRoaming = input.getBooleanOr("Roaming", false);
		if (wasRoaming) {
			transitionToRoaming();
		}

		// Restore new fields
		this.entityData.set(SOURCE_MOB_TYPE, input.getStringOr("SourceMobType", "minecraft:zombie"));
		this.entityData.set(RENDER_SCALE, input.getFloatOr("RenderScale", 1.0f));
		madraPool = input.getFloatOr("MadraPool", 0f);
		maxMadraPool = input.getFloatOr("MaxMadraPool", 0f);
		input.read("StoredLoadout", net.minecraft.nbt.CompoundTag.CODEC).ifPresent(tag -> {
			setStoredLoadout(com.cradle.mod.ability.PlayerLoadout.fromNbt(tag));
		});

		// Restore scaled attributes from power level (updated formula)
		int power = getPowerLevel();
		float health = 20.0f + (power * 10.0f);
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0);
	}
}

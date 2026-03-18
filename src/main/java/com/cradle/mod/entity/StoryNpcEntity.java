package com.cradle.mod.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.cradle.mod.network.OpenDialoguePayload;

/**
 * Invulnerable, persistent NPC entity for story mode.
 * Right-click opens a dialogue screen via OpenDialoguePayload.
 */
public class StoryNpcEntity extends PathfinderMob {

	private static final EntityDataAccessor<String> NPC_ID =
			SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> NPC_DISPLAY_NAME =
			SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> NPC_PATH =
			SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> NPC_STAGE =
			SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> FACTION =
			SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.STRING);

	public StoryNpcEntity(EntityType<? extends StoryNpcEntity> type, Level level) {
		super(type, level);
		this.setInvulnerable(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(NPC_ID, "");
		builder.define(NPC_DISPLAY_NAME, "NPC");
		builder.define(NPC_PATH, "");
		builder.define(NPC_STAGE, "");
		builder.define(FACTION, "");
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	public static AttributeSupplier.Builder createNpcAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 100.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25);
	}

	// ── Getters/Setters ────────────────────────────────────────────

	public String getNpcId() { return this.entityData.get(NPC_ID); }
	public void setNpcId(String id) { this.entityData.set(NPC_ID, id); }

	public String getNpcDisplayName() { return this.entityData.get(NPC_DISPLAY_NAME); }
	public void setNpcDisplayName(String name) {
		this.entityData.set(NPC_DISPLAY_NAME, name);
		this.setCustomName(Component.literal(name));
		this.setCustomNameVisible(true);
	}

	public String getNpcPath() { return this.entityData.get(NPC_PATH); }
	public void setNpcPath(String path) { this.entityData.set(NPC_PATH, path); }

	public String getNpcStage() { return this.entityData.get(NPC_STAGE); }
	public void setNpcStage(String stage) { this.entityData.set(NPC_STAGE, stage); }

	public String getFaction() { return this.entityData.get(FACTION); }
	public void setFaction(String faction) { this.entityData.set(FACTION, faction); }

	// ── Interaction ────────────────────────────────────────────────

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
			if (player instanceof ServerPlayer serverPlayer) {
				ServerPlayNetworking.send(serverPlayer,
						new OpenDialoguePayload(this.getNpcId(), this.getId()));
			}
		}
		return InteractionResult.SUCCESS;
	}

	// ── Invulnerability & Persistence ──────────────────────────────

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }

	@Override
	public void knockback(double strength, double x, double z) { }

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }

	@Override
	public boolean shouldBeSaved() { return true; }

	// ── NBT Persistence ────────────────────────────────────────────

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("NpcId", getNpcId());
		output.putString("NpcDisplayName", getNpcDisplayName());
		output.putString("NpcPath", getNpcPath());
		output.putString("NpcStage", getNpcStage());
		output.putString("Faction", getFaction());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setNpcId(input.getStringOr("NpcId", ""));
		String name = input.getStringOr("NpcDisplayName", "NPC");
		setNpcDisplayName(name);
		setNpcPath(input.getStringOr("NpcPath", ""));
		setNpcStage(input.getStringOr("NpcStage", ""));
		setFaction(input.getStringOr("Faction", ""));
	}
}

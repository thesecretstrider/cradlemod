package com.cradle.mod.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class CradleEntities {

	public static final ResourceKey<EntityType<?>> STRIKER_PROJECTILE_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE,
					Identifier.fromNamespaceAndPath("cradlemod", "striker_projectile"));

	public static final EntityType<StrikerProjectileEntity> STRIKER_PROJECTILE =
			Registry.register(
					BuiltInRegistries.ENTITY_TYPE,
					STRIKER_PROJECTILE_KEY,
					EntityType.Builder.<StrikerProjectileEntity>of(
									StrikerProjectileEntity::new,
									MobCategory.MISC
							)
							.sized(0.5f, 0.5f)
							.clientTrackingRange(4)
							.updateInterval(10)
							.fireImmune()
							.build(STRIKER_PROJECTILE_KEY)
			);

	// ── Remnant Entity ────────────────────────────────────────────────

	public static final ResourceKey<EntityType<?>> REMNANT_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE,
					Identifier.fromNamespaceAndPath("cradlemod", "remnant"));

	public static final EntityType<RemnantEntity> REMNANT =
			Registry.register(
					BuiltInRegistries.ENTITY_TYPE,
					REMNANT_KEY,
					EntityType.Builder.<RemnantEntity>of(
									RemnantEntity::new,
									MobCategory.MONSTER
							)
							.sized(0.6f, 1.8f)
							.clientTrackingRange(8)
							.updateInterval(3)
							.fireImmune()
							.build(REMNANT_KEY)
			);

	// ── Story NPC Entity ─────────────────────────────────────────────

	public static final ResourceKey<EntityType<?>> STORY_NPC_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE,
					Identifier.fromNamespaceAndPath("cradlemod", "story_npc"));

	public static final EntityType<StoryNpcEntity> STORY_NPC =
			Registry.register(
					BuiltInRegistries.ENTITY_TYPE,
					STORY_NPC_KEY,
					EntityType.Builder.<StoryNpcEntity>of(
									StoryNpcEntity::new,
									MobCategory.CREATURE
							)
							.sized(0.6f, 1.8f)
							.clientTrackingRange(8)
							.updateInterval(3)
							.build(STORY_NPC_KEY)
			);

	public static void register() {
		// Static init triggers entity type registration.
		// Register mob attributes for living entities.
		FabricDefaultAttributeRegistry.register(REMNANT, RemnantEntity.createRemnantAttributes());
		FabricDefaultAttributeRegistry.register(STORY_NPC, StoryNpcEntity.createNpcAttributes());
	}
}

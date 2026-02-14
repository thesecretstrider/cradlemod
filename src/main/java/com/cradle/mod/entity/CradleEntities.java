package com.cradle.mod.entity;

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

	public static void register() {
		// Static init triggers registration
	}
}

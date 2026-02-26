package com.cradle.mod.ability;

import com.cradle.mod.CradlePlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Functional interfaces for ability behavior lambdas.
 * Each ability type has its own handler signature.
 */
public final class AbilityHandlers {

	private AbilityHandlers() {}

	/**
	 * Called every tick while an Enforcer ability is active.
	 * Apply buffs, effects, damage, etc.
	 */
	@FunctionalInterface
	public interface EnforcerTickHandler {
		void tick(ServerPlayer player, CradlePlayerData data, AbilityDefinition def, int upgradeLevel);
	}

	/**
	 * Called when an Enforcer ability is first activated.
	 * Apply initial attribute modifiers, effects, messages.
	 */
	@FunctionalInterface
	public interface EnforcerActivateHandler {
		void activate(ServerPlayer player, CradlePlayerData data, AbilityDefinition def, int upgradeLevel);
	}

	/**
	 * Called when an Enforcer ability is deactivated.
	 * Remove attribute modifiers, effects, clean up.
	 */
	@FunctionalInterface
	public interface EnforcerDeactivateHandler {
		void deactivate(ServerPlayer player, CradlePlayerData data, AbilityDefinition def, int upgradeLevel);
	}

	/**
	 * Called when a Striker ability is fired.
	 * Spawn projectiles, apply instant effects, etc.
	 */
	@FunctionalInterface
	public interface StrikerFireHandler {
		void fire(ServerPlayer player, CradlePlayerData data, AbilityDefinition def, int upgradeLevel);
	}

	/**
	 * Called every RULER_EFFECT_INTERVAL ticks (0.5s) while a Ruler ability is active.
	 * Apply area effects to nearby enemies.
	 */
	@FunctionalInterface
	public interface RulerAreaHandler {
		void applyArea(ServerPlayer player, CradlePlayerData data, AbilityDefinition def,
		               int upgradeLevel, List<LivingEntity> enemies, ServerLevel level);
	}
}

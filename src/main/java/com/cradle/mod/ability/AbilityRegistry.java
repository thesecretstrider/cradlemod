package com.cradle.mod.ability;

import com.cradle.mod.CradlePlayerData;
import com.cradle.mod.CradleMod;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central registry for all ability definitions.
 * Initialized at mod startup via {@link #registerAll()}.
 */
public final class AbilityRegistry {

	private static final Map<String, AbilityDefinition> ABILITIES = new LinkedHashMap<>();

	private AbilityRegistry() {}

	public static void register(AbilityDefinition def) {
		if (ABILITIES.containsKey(def.getId())) {
			CradleMod.LOGGER.warn("Duplicate ability ID: {}", def.getId());
		}
		ABILITIES.put(def.getId(), def);
	}

	public static AbilityDefinition get(String id) {
		return ABILITIES.get(id);
	}

	public static Collection<AbilityDefinition> getAll() {
		return Collections.unmodifiableCollection(ABILITIES.values());
	}

	/**
	 * Get all abilities for a specific path (including universal abilities).
	 */
	public static List<AbilityDefinition> getForPath(CradlePlayerData.Path path) {
		return ABILITIES.values().stream()
			.filter(def -> def.isUniversal() || def.getRequiredPath() == path)
			.collect(Collectors.toList());
	}

	/**
	 * Get all abilities of a specific type for a path.
	 */
	public static List<AbilityDefinition> getByPathAndType(CradlePlayerData.Path path, AbilityType type) {
		return ABILITIES.values().stream()
			.filter(def -> def.getType() == type)
			.filter(def -> def.isUniversal() || def.getRequiredPath() == path)
			.collect(Collectors.toList());
	}

	/**
	 * Get all universal abilities (available to any path).
	 */
	public static List<AbilityDefinition> getUniversal() {
		return ABILITIES.values().stream()
			.filter(AbilityDefinition::isUniversal)
			.collect(Collectors.toList());
	}

	/**
	 * Get the default striker for a path (the first registered striker for that path).
	 */
	public static AbilityDefinition getDefaultStriker(CradlePlayerData.Path path) {
		return ABILITIES.values().stream()
			.filter(def -> def.getType() == AbilityType.STRIKER && def.getRequiredPath() == path)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Get the default enforcer for a path.
	 */
	public static AbilityDefinition getDefaultEnforcer(CradlePlayerData.Path path) {
		return ABILITIES.values().stream()
			.filter(def -> def.getType() == AbilityType.ENFORCER && def.getRequiredPath() == path)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Get the default ruler for a path.
	 */
	public static AbilityDefinition getDefaultRuler(CradlePlayerData.Path path) {
		return ABILITIES.values().stream()
			.filter(def -> def.getType() == AbilityType.RULER && def.getRequiredPath() == path)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Get abilities available for picking at a given stage and path.
	 * Filters by unlock stage and path compatibility.
	 */
	public static List<AbilityDefinition> getAvailableForPick(CradlePlayerData.Path path,
	                                                           AbilityType type,
	                                                           CradlePlayerData.AdvancementStage stage) {
		return ABILITIES.values().stream()
			.filter(def -> def.getType() == type)
			.filter(def -> def.isUniversal() || def.getRequiredPath() == path)
			.filter(def -> stage.ordinal() >= def.getUnlockStage().ordinal())
			.collect(Collectors.toList());
	}

	/**
	 * Register all abilities. Called from CradleMod.onInitialize().
	 */
	public static void registerAll() {
		ABILITIES.clear();
		AbilityDefinitions.registerAll();
		CradleMod.LOGGER.info("Registered {} abilities", ABILITIES.size());
	}
}

package com.cradle.mod.ability;

import com.cradle.mod.CradlePlayerData;

/**
 * Defines a single ability/technique in the skill tree.
 * Each ability has base stats, scaling rates, visual tiers,
 * optional branching, and pluggable behavior lambdas.
 * Built via {@link Builder}.
 */
public final class AbilityDefinition {

	private final String id;
	private final String displayName;
	private final AbilityType type;
	private final CradlePlayerData.Path requiredPath; // null = universal
	private final CradlePlayerData.AdvancementStage unlockStage;
	private final String description;
	private final int color; // ARGB for particles / UI

	// Upgrade system
	private final int maxUpgradeLevel;
	private final int branchLevel;       // 0 = no branch
	private final String branchAbilityId; // null if no branch

	// Base stats (at level 1)
	private final float baseDamage;
	private final float baseMadraCost;   // Per-use (striker) or per-tick (enforcer/ruler)
	private final long baseCooldownMs;   // Striker only
	private final float baseRadius;      // Ruler area radius

	// Per-level scaling
	private final float damagePerLevel;       // +5% per level default
	private final float costReductionPerLevel;   // -2% per level default
	private final float cooldownReductionPerLevel; // -2% per level default
	private final float radiusPerLevel;        // +3% per level default

	// Visual tier thresholds
	private final int visualTier1Level; // default 5
	private final int visualTier2Level; // default 10
	private final int visualTier3Level; // default 15

	// Behavior lambdas
	private final AbilityHandlers.EnforcerTickHandler enforcerTick;
	private final AbilityHandlers.EnforcerActivateHandler enforcerActivate;
	private final AbilityHandlers.EnforcerDeactivateHandler enforcerDeactivate;
	private final AbilityHandlers.StrikerFireHandler strikerFire;
	private final AbilityHandlers.RulerAreaHandler rulerArea;

	private AbilityDefinition(Builder b) {
		this.id = b.id;
		this.displayName = b.displayName;
		this.type = b.type;
		this.requiredPath = b.requiredPath;
		this.unlockStage = b.unlockStage;
		this.description = b.description;
		this.color = b.color;
		this.maxUpgradeLevel = b.maxUpgradeLevel;
		this.branchLevel = b.branchLevel;
		this.branchAbilityId = b.branchAbilityId;
		this.baseDamage = b.baseDamage;
		this.baseMadraCost = b.baseMadraCost;
		this.baseCooldownMs = b.baseCooldownMs;
		this.baseRadius = b.baseRadius;
		this.damagePerLevel = b.damagePerLevel;
		this.costReductionPerLevel = b.costReductionPerLevel;
		this.cooldownReductionPerLevel = b.cooldownReductionPerLevel;
		this.radiusPerLevel = b.radiusPerLevel;
		this.visualTier1Level = b.visualTier1Level;
		this.visualTier2Level = b.visualTier2Level;
		this.visualTier3Level = b.visualTier3Level;
		this.enforcerTick = b.enforcerTick;
		this.enforcerActivate = b.enforcerActivate;
		this.enforcerDeactivate = b.enforcerDeactivate;
		this.strikerFire = b.strikerFire;
		this.rulerArea = b.rulerArea;
	}

	// ── Getters ──────────────────────────────────────────────────────────

	public String getId() { return id; }
	public String getDisplayName() { return displayName; }
	public AbilityType getType() { return type; }
	public CradlePlayerData.Path getRequiredPath() { return requiredPath; }
	public CradlePlayerData.AdvancementStage getUnlockStage() { return unlockStage; }
	public String getDescription() { return description; }
	public int getColor() { return color; }
	public int getMaxUpgradeLevel() { return maxUpgradeLevel; }
	public int getBranchLevel() { return branchLevel; }
	public String getBranchAbilityId() { return branchAbilityId; }
	public boolean hasBranch() { return branchLevel > 0 && branchAbilityId != null; }
	public boolean isUniversal() { return requiredPath == null; }

	public float getBaseDamage() { return baseDamage; }
	public float getBaseMadraCost() { return baseMadraCost; }
	public long getBaseCooldownMs() { return baseCooldownMs; }
	public float getBaseRadius() { return baseRadius; }

	public AbilityHandlers.EnforcerTickHandler getEnforcerTick() { return enforcerTick; }
	public AbilityHandlers.EnforcerActivateHandler getEnforcerActivate() { return enforcerActivate; }
	public AbilityHandlers.EnforcerDeactivateHandler getEnforcerDeactivate() { return enforcerDeactivate; }
	public AbilityHandlers.StrikerFireHandler getStrikerFire() { return strikerFire; }
	public AbilityHandlers.RulerAreaHandler getRulerArea() { return rulerArea; }

	// ── Scaled stat calculations ─────────────────────────────────────────

	/**
	 * Returns damage scaled by upgrade level.
	 * Formula: baseDamage * (1.0 + damagePerLevel * upgradeLevel)
	 */
	public float getScaledDamage(int upgradeLevel) {
		return baseDamage * (1.0f + damagePerLevel * (upgradeLevel - 1));
	}

	/**
	 * Returns madra cost scaled by upgrade level (lower = cheaper).
	 * Formula: baseMadraCost * pow(1.0 - costReduction, upgradeLevel - 1)
	 */
	public float getScaledMadraCost(int upgradeLevel) {
		return baseMadraCost * (float) Math.pow(1.0 - costReductionPerLevel, upgradeLevel - 1);
	}

	/**
	 * Returns cooldown in ms scaled by upgrade level (lower = faster).
	 */
	public long getScaledCooldownMs(int upgradeLevel) {
		return (long) (baseCooldownMs * Math.pow(1.0 - cooldownReductionPerLevel, upgradeLevel - 1));
	}

	/**
	 * Returns radius scaled by upgrade level.
	 */
	public float getScaledRadius(int upgradeLevel) {
		return baseRadius * (1.0f + radiusPerLevel * (upgradeLevel - 1));
	}

	/**
	 * Returns the visual tier (0-3) based on upgrade level.
	 */
	public int getVisualTier(int upgradeLevel) {
		if (upgradeLevel >= visualTier3Level) return 3;
		if (upgradeLevel >= visualTier2Level) return 2;
		if (upgradeLevel >= visualTier1Level) return 1;
		return 0;
	}

	// ── Builder ──────────────────────────────────────────────────────────

	public static Builder builder(String id, String displayName, AbilityType type) {
		return new Builder(id, displayName, type);
	}

	public static final class Builder {
		private final String id;
		private final String displayName;
		private final AbilityType type;

		private CradlePlayerData.Path requiredPath = null;
		private CradlePlayerData.AdvancementStage unlockStage = CradlePlayerData.AdvancementStage.FOUNDATION;
		private String description = "";
		private int color = 0xFFFFFFFF;

		private int maxUpgradeLevel = 20;
		private int branchLevel = 0;
		private String branchAbilityId = null;

		private float baseDamage = 0f;
		private float baseMadraCost = 0f;
		private long baseCooldownMs = 0L;
		private float baseRadius = 0f;

		private float damagePerLevel = 0.05f;       // +5% per level
		private float costReductionPerLevel = 0.02f; // -2% per level
		private float cooldownReductionPerLevel = 0.02f;
		private float radiusPerLevel = 0.03f;        // +3% per level

		private int visualTier1Level = 5;
		private int visualTier2Level = 10;
		private int visualTier3Level = 15;

		private AbilityHandlers.EnforcerTickHandler enforcerTick;
		private AbilityHandlers.EnforcerActivateHandler enforcerActivate;
		private AbilityHandlers.EnforcerDeactivateHandler enforcerDeactivate;
		private AbilityHandlers.StrikerFireHandler strikerFire;
		private AbilityHandlers.RulerAreaHandler rulerArea;

		private Builder(String id, String displayName, AbilityType type) {
			this.id = id;
			this.displayName = displayName;
			this.type = type;
		}

		public Builder path(CradlePlayerData.Path path) { this.requiredPath = path; return this; }
		public Builder unlockStage(CradlePlayerData.AdvancementStage stage) { this.unlockStage = stage; return this; }
		public Builder description(String desc) { this.description = desc; return this; }
		public Builder color(int color) { this.color = color; return this; }

		public Builder maxUpgradeLevel(int max) { this.maxUpgradeLevel = max; return this; }
		public Builder branchAt(int level, String branchId) { this.branchLevel = level; this.branchAbilityId = branchId; return this; }

		public Builder baseDamage(float d) { this.baseDamage = d; return this; }
		public Builder baseMadraCost(float c) { this.baseMadraCost = c; return this; }
		public Builder baseCooldownMs(long cd) { this.baseCooldownMs = cd; return this; }
		public Builder baseRadius(float r) { this.baseRadius = r; return this; }

		public Builder damagePerLevel(float dpl) { this.damagePerLevel = dpl; return this; }
		public Builder costReductionPerLevel(float crpl) { this.costReductionPerLevel = crpl; return this; }
		public Builder cooldownReductionPerLevel(float cdpl) { this.cooldownReductionPerLevel = cdpl; return this; }
		public Builder radiusPerLevel(float rpl) { this.radiusPerLevel = rpl; return this; }

		public Builder visualTiers(int t1, int t2, int t3) {
			this.visualTier1Level = t1;
			this.visualTier2Level = t2;
			this.visualTier3Level = t3;
			return this;
		}

		public Builder onEnforcerTick(AbilityHandlers.EnforcerTickHandler h) { this.enforcerTick = h; return this; }
		public Builder onEnforcerActivate(AbilityHandlers.EnforcerActivateHandler h) { this.enforcerActivate = h; return this; }
		public Builder onEnforcerDeactivate(AbilityHandlers.EnforcerDeactivateHandler h) { this.enforcerDeactivate = h; return this; }
		public Builder onStrikerFire(AbilityHandlers.StrikerFireHandler h) { this.strikerFire = h; return this; }
		public Builder onRulerArea(AbilityHandlers.RulerAreaHandler h) { this.rulerArea = h; return this; }

		public AbilityDefinition build() {
			return new AbilityDefinition(this);
		}
	}
}

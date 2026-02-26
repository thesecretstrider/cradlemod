package com.cradle.mod.ability;

/**
 * The three categories of sacred arts techniques.
 */
public enum AbilityType {
	/** Toggle ability — sustained buff on the player, drains madra per tick. */
	ENFORCER,
	/** One-shot ability — fires a projectile or burst, has cooldown + flat madra cost. */
	STRIKER,
	/** Toggle ability — sustained area effect around the player, drains madra per tick. */
	RULER;

	public String displayName() {
		return switch (this) {
			case ENFORCER -> "Enforcer";
			case STRIKER -> "Striker";
			case RULER -> "Ruler";
		};
	}

	public int color() {
		return switch (this) {
			case ENFORCER -> 0xFFFF6600; // Orange
			case STRIKER -> 0xFFFF2222; // Red
			case RULER -> 0xFF4488FF;   // Blue
		};
	}
}

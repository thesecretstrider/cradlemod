package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client sync packet containing all player Cradle data.
 * Boolean flags are packed into a single int to stay within StreamCodec.composite's
 * 12-field limit while leaving room for future fields.
 *
 * Flags bitmask:
 *   bit 0 (1)  = cycling
 *   bit 1 (2)  = canAdvance
 *   bit 2 (4)  = ironBodyActive
 *   bit 3 (8)  = enforcerActive
 *   bit 4 (16) = rulerActive
 *   bit 5 (32) = hasSage
 *   bit 6 (64) = hasHerald
 *   bit 7 (128) = swordCycling
 *   bit 8 (256) = flying
 *   bit 9 (512) = spiritShiftActive
 *   bits 10-15 (1024-32768) = loadout slot 0-5 active state
 *   bit 16 (65536) = copperSightActive
 *   bits 17-19 = goldsign ordinal (3 bits, 0-5)
 */
public record CradleSyncPayload(
		int level,
		int cyclingXp,
		int xpToNext,
		String path,
		String stage,
		float currentMadra,
		float maxMadra,
		int flags,
		String ironBody,
		float currentWillpower,
		float maxWillpower,
		String icon
) implements CustomPacketPayload {

	// Flag bit constants
	public static final int FLAG_CYCLING         = 1;
	public static final int FLAG_CAN_ADVANCE     = 2;
	public static final int FLAG_IRON_BODY_ACTIVE = 4;
	public static final int FLAG_ENFORCER_ACTIVE  = 8;
	public static final int FLAG_RULER_ACTIVE     = 16;
	public static final int FLAG_HAS_SAGE         = 32;
	public static final int FLAG_HAS_HERALD       = 64;
	public static final int FLAG_SWORD_CYCLING    = 128;
	public static final int FLAG_FLYING           = 256;
	public static final int FLAG_SPIRIT_SHIFT     = 512;
	// Bits 10-15: loadout slot 0-5 active state (for real-time HUD updates)
	public static final int FLAG_SLOT_ACTIVE_BASE  = 1024; // bit 10 = slot 0
	public static final int FLAG_COPPER_SIGHT      = 65536; // bit 16
	// Bits 17-19: goldsign ordinal (3 bits for 6 values: NONE=0, BLACK_FLAME_EYES=1, etc.)
	public static final int GOLDSIGN_SHIFT         = 17;
	public static final int GOLDSIGN_MASK          = 0x7; // 3 bits

	public static final Type<CradleSyncPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "cradle_sync"));

	public static final StreamCodec<ByteBuf, CradleSyncPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, CradleSyncPayload::level,
					ByteBufCodecs.VAR_INT, CradleSyncPayload::cyclingXp,
					ByteBufCodecs.VAR_INT, CradleSyncPayload::xpToNext,
					ByteBufCodecs.STRING_UTF8, CradleSyncPayload::path,
					ByteBufCodecs.STRING_UTF8, CradleSyncPayload::stage,
					ByteBufCodecs.FLOAT, CradleSyncPayload::currentMadra,
					ByteBufCodecs.FLOAT, CradleSyncPayload::maxMadra,
					ByteBufCodecs.VAR_INT, CradleSyncPayload::flags,
					ByteBufCodecs.STRING_UTF8, CradleSyncPayload::ironBody,
					ByteBufCodecs.FLOAT, CradleSyncPayload::currentWillpower,
					ByteBufCodecs.FLOAT, CradleSyncPayload::maxWillpower,
					ByteBufCodecs.STRING_UTF8, CradleSyncPayload::icon,
					CradleSyncPayload::new
			);

	// Convenience accessors for individual flags
	public boolean cycling()         { return (flags & FLAG_CYCLING) != 0; }
	public boolean canAdvance()      { return (flags & FLAG_CAN_ADVANCE) != 0; }
	public boolean ironBodyActive()  { return (flags & FLAG_IRON_BODY_ACTIVE) != 0; }
	public boolean enforcerActive()  { return (flags & FLAG_ENFORCER_ACTIVE) != 0; }
	public boolean rulerActive()     { return (flags & FLAG_RULER_ACTIVE) != 0; }
	public boolean hasSage()         { return (flags & FLAG_HAS_SAGE) != 0; }
	public boolean hasHerald()       { return (flags & FLAG_HAS_HERALD) != 0; }
	public boolean swordCycling()    { return (flags & FLAG_SWORD_CYCLING) != 0; }
	public boolean flying()          { return (flags & FLAG_FLYING) != 0; }
	public boolean spiritShiftActive() { return (flags & FLAG_SPIRIT_SHIFT) != 0; }
	public boolean copperSightActive() { return (flags & FLAG_COPPER_SIGHT) != 0; }
	public int goldsignOrdinal()       { return (flags >> GOLDSIGN_SHIFT) & GOLDSIGN_MASK; }

	/**
	 * Helper to build the flags int from individual booleans.
	 */
	public static int buildFlags(boolean cycling, boolean canAdvance,
								  boolean ironBodyActive, boolean enforcerActive,
								  boolean rulerActive, boolean hasSage, boolean hasHerald,
								  boolean swordCycling, boolean flying,
								  boolean spiritShiftActive) {
		int f = 0;
		if (cycling)         f |= FLAG_CYCLING;
		if (canAdvance)      f |= FLAG_CAN_ADVANCE;
		if (ironBodyActive)  f |= FLAG_IRON_BODY_ACTIVE;
		if (enforcerActive)  f |= FLAG_ENFORCER_ACTIVE;
		if (rulerActive)     f |= FLAG_RULER_ACTIVE;
		if (hasSage)         f |= FLAG_HAS_SAGE;
		if (hasHerald)       f |= FLAG_HAS_HERALD;
		if (swordCycling)    f |= FLAG_SWORD_CYCLING;
		if (flying)          f |= FLAG_FLYING;
		if (spiritShiftActive) f |= FLAG_SPIRIT_SHIFT;
		return f;
	}

	/**
	 * Add loadout slot active flags to an existing flags int.
	 * Slots 0-5 are mapped to bits 10-15.
	 */
	public static int addSlotActiveFlags(int baseFlags, boolean[] slotActive) {
		int f = baseFlags;
		for (int i = 0; i < Math.min(slotActive.length, 6); i++) {
			if (slotActive[i]) f |= (FLAG_SLOT_ACTIVE_BASE << i);
		}
		return f;
	}

	/**
	 * Extract the slot active flags (bits 10-15) as an int suitable for
	 * ClientLoadoutData.updateActiveFlags().
	 */
	public int getSlotActiveFlags() {
		return (flags >> 10) & 0x3F; // 6 bits
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

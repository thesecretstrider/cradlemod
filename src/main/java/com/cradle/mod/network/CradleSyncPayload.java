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
		String ironBody
) implements CustomPacketPayload {

	// Flag bit constants
	public static final int FLAG_CYCLING         = 1;
	public static final int FLAG_CAN_ADVANCE     = 2;
	public static final int FLAG_IRON_BODY_ACTIVE = 4;
	public static final int FLAG_ENFORCER_ACTIVE  = 8;
	public static final int FLAG_RULER_ACTIVE     = 16;
	public static final int FLAG_HAS_SAGE         = 32;
	public static final int FLAG_HAS_HERALD       = 64;

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

	/**
	 * Helper to build the flags int from individual booleans.
	 */
	public static int buildFlags(boolean cycling, boolean canAdvance,
								  boolean ironBodyActive, boolean enforcerActive,
								  boolean rulerActive, boolean hasSage, boolean hasHerald) {
		int f = 0;
		if (cycling)         f |= FLAG_CYCLING;
		if (canAdvance)      f |= FLAG_CAN_ADVANCE;
		if (ironBodyActive)  f |= FLAG_IRON_BODY_ACTIVE;
		if (enforcerActive)  f |= FLAG_ENFORCER_ACTIVE;
		if (rulerActive)     f |= FLAG_RULER_ACTIVE;
		if (hasSage)         f |= FLAG_HAS_SAGE;
		if (hasHerald)       f |= FLAG_HAS_HERALD;
		return f;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

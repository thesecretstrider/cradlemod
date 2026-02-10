package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CradleSyncPayload(
		int level,
		int cyclingXp,
		int xpToNext,
		String path,
		String stage,
		float currentMadra,
		float maxMadra,
		boolean cycling,
		boolean canAdvance
) implements CustomPacketPayload {

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
					ByteBufCodecs.BOOL, CradleSyncPayload::cycling,
					ByteBufCodecs.BOOL, CradleSyncPayload::canAdvance,
					CradleSyncPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

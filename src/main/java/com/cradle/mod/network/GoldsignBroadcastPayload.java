package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client: broadcast a player's goldsign to all nearby players.
 * Sent when a goldsign changes (Remnant absorption) and on player join
 * (so newly joining players see existing goldsigns).
 *
 * playerUuid:      UUID string of the player whose goldsign changed
 * goldsignOrdinal: ordinal value of the Goldsign enum (0 = NONE)
 */
public record GoldsignBroadcastPayload(
		String playerUuid,
		int goldsignOrdinal
) implements CustomPacketPayload {

	public static final Type<GoldsignBroadcastPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "goldsign_broadcast"));

	public static final StreamCodec<ByteBuf, GoldsignBroadcastPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, GoldsignBroadcastPayload::playerUuid,
					ByteBufCodecs.VAR_INT, GoldsignBroadcastPayload::goldsignOrdinal,
					GoldsignBroadcastPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

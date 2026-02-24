package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client: notify target player that they received a duel invite.
 * Carries the challenger's display name and the duel mode (FRIENDLY/COMPETITIVE).
 */
public record DuelInviteReceivedPayload(
		String challengerName,
		String mode  // "FRIENDLY" or "COMPETITIVE"
) implements CustomPacketPayload {

	public static final Type<DuelInviteReceivedPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "duel_invite_received"));

	public static final StreamCodec<ByteBuf, DuelInviteReceivedPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, DuelInviteReceivedPayload::challengerName,
					ByteBufCodecs.STRING_UTF8, DuelInviteReceivedPayload::mode,
					DuelInviteReceivedPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

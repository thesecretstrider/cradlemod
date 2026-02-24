package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client: duel has ended. Carries result info and the receiving
 * player's updated duel stats.
 *
 * winnerName: display name of the winner (or "Draw" for a draw)
 * loserName:  display name of the loser (or "" for a draw)
 * mode:       "FRIENDLY" or "COMPETITIVE"
 * wins/losses/draws: the receiving player's updated duel statistics
 */
public record DuelEndPayload(
		String winnerName,
		String loserName,
		String mode,
		int wins,
		int losses,
		int draws
) implements CustomPacketPayload {

	public static final Type<DuelEndPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "duel_end"));

	public static final StreamCodec<ByteBuf, DuelEndPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, DuelEndPayload::winnerName,
					ByteBufCodecs.STRING_UTF8, DuelEndPayload::loserName,
					ByteBufCodecs.STRING_UTF8, DuelEndPayload::mode,
					ByteBufCodecs.VAR_INT, DuelEndPayload::wins,
					ByteBufCodecs.VAR_INT, DuelEndPayload::losses,
					ByteBufCodecs.VAR_INT, DuelEndPayload::draws,
					DuelEndPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

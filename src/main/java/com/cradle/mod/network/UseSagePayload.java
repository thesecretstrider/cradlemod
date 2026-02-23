package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: uses a Sage Authority command.
 * Sent when the player presses/holds the V key.
 *
 * ability values:
 *   "STOP" = tap V (freezes all nearby hostiles)
 *   "KILL" = hold V for 1 second (targeted massive damage)
 */
public record UseSagePayload(String ability) implements CustomPacketPayload {

	public static final Type<UseSagePayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "use_sage"));

	public static final StreamCodec<ByteBuf, UseSagePayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, UseSagePayload::ability,
					UseSagePayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

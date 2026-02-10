package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: requests an advancement breakthrough.
 * Sent when the player clicks the "Advance" button on the CradleInfoScreen.
 * Has no data — the server checks requirements and performs the advancement.
 */
public record AttemptAdvancePayload() implements CustomPacketPayload {

	public static final Type<AttemptAdvancePayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "attempt_advance"));

	public static final StreamCodec<ByteBuf, AttemptAdvancePayload> STREAM_CODEC =
			StreamCodec.unit(new AttemptAdvancePayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

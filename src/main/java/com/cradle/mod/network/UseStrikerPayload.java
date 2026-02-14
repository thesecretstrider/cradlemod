package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: fires the player's Striker technique.
 * Sent when the player presses the X key.
 */
public record UseStrikerPayload() implements CustomPacketPayload {

	public static final Type<UseStrikerPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "use_striker"));

	public static final StreamCodec<ByteBuf, UseStrikerPayload> STREAM_CODEC =
			StreamCodec.unit(new UseStrikerPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

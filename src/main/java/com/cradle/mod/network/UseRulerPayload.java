package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: toggles the player's Ruler technique on/off.
 * Sent when the player presses the C key.
 */
public record UseRulerPayload() implements CustomPacketPayload {

	public static final Type<UseRulerPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "use_ruler"));

	public static final StreamCodec<ByteBuf, UseRulerPayload> STREAM_CODEC =
			StreamCodec.unit(new UseRulerPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: toggles Herald Spirit Shift on/off.
 * Sent when the player presses the B key.
 */
public record UseHeraldPayload() implements CustomPacketPayload {

	public static final Type<UseHeraldPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "use_herald"));

	public static final StreamCodec<ByteBuf, UseHeraldPayload> STREAM_CODEC =
			StreamCodec.unit(new UseHeraldPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

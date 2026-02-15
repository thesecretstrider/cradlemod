package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: toggles cycling on/off.
 * Sent when the player presses the G key.
 */
public record ToggleCyclingPayload() implements CustomPacketPayload {

	public static final Type<ToggleCyclingPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "toggle_cycling"));

	public static final StreamCodec<ByteBuf, ToggleCyclingPayload> STREAM_CODEC =
			StreamCodec.unit(new ToggleCyclingPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

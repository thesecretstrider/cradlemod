package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: toggles the player's Iron Body buff on/off.
 * Sent when the player presses the P key.
 */
public record ToggleIronBodyPayload() implements CustomPacketPayload {

	public static final Type<ToggleIronBodyPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "toggle_iron_body"));

	public static final StreamCodec<ByteBuf, ToggleIronBodyPayload> STREAM_CODEC =
			StreamCodec.unit(new ToggleIronBodyPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

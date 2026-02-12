package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: toggles the player's Enforcer technique on/off.
 * Sent when the player presses the R key.
 */
public record UseEnforcerPayload() implements CustomPacketPayload {

	public static final Type<UseEnforcerPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "use_enforcer"));

	public static final StreamCodec<ByteBuf, UseEnforcerPayload> STREAM_CODEC =
			StreamCodec.unit(new UseEnforcerPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

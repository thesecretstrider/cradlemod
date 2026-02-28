package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: toggles Copper Sight on/off.
 * Sent when the player presses the H key.
 * Server validates that the player is Copper+ before toggling.
 */
public record ToggleCopperSightPayload() implements CustomPacketPayload {

	public static final Type<ToggleCopperSightPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "toggle_copper_sight"));

	public static final StreamCodec<ByteBuf, ToggleCopperSightPayload> STREAM_CODEC =
			StreamCodec.unit(new ToggleCopperSightPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

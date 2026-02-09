package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client packet: tells the client to open the Path selection screen.
 * Sent when a player joins and has not yet chosen a Path.
 */
public record OpenPathSelectionPayload() implements CustomPacketPayload {

	public static final Type<OpenPathSelectionPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "open_path_selection"));

	public static final StreamCodec<ByteBuf, OpenPathSelectionPayload> STREAM_CODEC =
			StreamCodec.unit(new OpenPathSelectionPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

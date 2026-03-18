package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client packet: tells the client to open the Character Selection screen.
 * Sent when a player joins a Cradle mode world and has not yet chosen a character.
 */
public record OpenCharacterSelectionPayload() implements CustomPacketPayload {

	public static final Type<OpenCharacterSelectionPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "open_character_selection"));

	public static final StreamCodec<ByteBuf, OpenCharacterSelectionPayload> STREAM_CODEC =
			StreamCodec.unit(new OpenCharacterSelectionPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

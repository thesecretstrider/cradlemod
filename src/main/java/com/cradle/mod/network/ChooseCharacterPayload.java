package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: sends the player's chosen character name to the server.
 * Sent when the player clicks a character card in the CharacterSelectionScreen.
 */
public record ChooseCharacterPayload(String characterName) implements CustomPacketPayload {

	public static final Type<ChooseCharacterPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "choose_character"));

	public static final StreamCodec<ByteBuf, ChooseCharacterPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, ChooseCharacterPayload::characterName,
					ChooseCharacterPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

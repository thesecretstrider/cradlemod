package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: sends the player's chosen Path name to the server.
 * Sent when the player clicks a path card in the PathSelectionScreen.
 */
public record ChoosePathPayload(String pathName) implements CustomPacketPayload {

	public static final Type<ChoosePathPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "choose_path"));

	public static final StreamCodec<ByteBuf, ChoosePathPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, ChoosePathPayload::pathName,
					ChoosePathPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

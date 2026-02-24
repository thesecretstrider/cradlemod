package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: sends the player's Icon choice.
 * Sent when the player clicks an Icon on the Icon selection screen.
 * The iconName field should match a CradlePlayerData.Icon enum name.
 */
public record ChooseIconPayload(String iconName) implements CustomPacketPayload {

	public static final Type<ChooseIconPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "choose_icon"));

	public static final StreamCodec<ByteBuf, ChooseIconPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, ChooseIconPayload::iconName,
					ChooseIconPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: sends the player's Sage/Herald choice.
 * Sent when the player clicks "Become Sage" or "Become Herald" on the info screen.
 * The choice field should be "SAGE" or "HERALD".
 */
public record ChooseSageHeraldPayload(String choice) implements CustomPacketPayload {

	public static final Type<ChooseSageHeraldPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "choose_sage_herald"));

	public static final StreamCodec<ByteBuf, ChooseSageHeraldPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, ChooseSageHeraldPayload::choice,
					ChooseSageHeraldPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

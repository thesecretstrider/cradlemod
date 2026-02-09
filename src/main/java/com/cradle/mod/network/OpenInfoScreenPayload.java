package com.cradle.mod.network;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenInfoScreenPayload() implements CustomPacketPayload {

	public static final Type<OpenInfoScreenPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "open_info"));

	public static final StreamCodec<io.netty.buffer.ByteBuf, OpenInfoScreenPayload> STREAM_CODEC =
			StreamCodec.unit(new OpenInfoScreenPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

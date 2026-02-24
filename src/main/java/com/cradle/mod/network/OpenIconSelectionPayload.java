package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client packet: tells the client to open the Icon selection screen.
 * Sent when a player is about to become a Sage (or Monarch from Herald).
 * Carries the player's path name so the client knows which Icons to offer,
 * and a forMonarch flag so the client knows the advancement context.
 */
public record OpenIconSelectionPayload(String pathName, boolean forMonarch) implements CustomPacketPayload {

	public static final Type<OpenIconSelectionPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "open_icon_selection"));

	public static final StreamCodec<ByteBuf, OpenIconSelectionPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, OpenIconSelectionPayload::pathName,
					ByteBufCodecs.BOOL, OpenIconSelectionPayload::forMonarch,
					OpenIconSelectionPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server: swap the ability in the given slot to a new one.
 * Resets upgrade level to 1. Player loses all progress on the old ability.
 */
public record SwapAbilityPayload(int slot, String newAbilityId) implements CustomPacketPayload {

	public static final Type<SwapAbilityPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "swap_ability"));

	public static final StreamCodec<ByteBuf, SwapAbilityPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, SwapAbilityPayload::slot,
					ByteBufCodecs.STRING_UTF8, SwapAbilityPayload::newAbilityId,
					SwapAbilityPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

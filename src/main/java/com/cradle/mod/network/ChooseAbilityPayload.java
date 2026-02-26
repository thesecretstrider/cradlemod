package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server: choose an ability at a stage gate (Copper/Iron/Low Gold).
 * The server validates the ability is legal for the player's path/stage and
 * assigns it to the specified slot.
 *
 * Unlike SwapAbilityPayload, this is for first-time picks during advancement —
 * the slot should be empty, so there's no level reset warning.
 */
public record ChooseAbilityPayload(String abilityId, int slot) implements CustomPacketPayload {

	public static final Type<ChooseAbilityPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "choose_ability"));

	public static final StreamCodec<ByteBuf, ChooseAbilityPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, ChooseAbilityPayload::abilityId,
					ByteBufCodecs.VAR_INT, ChooseAbilityPayload::slot,
					ChooseAbilityPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

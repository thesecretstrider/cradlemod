package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: fires a charged Striker ability.
 * Sent when the player releases a held Striker keybind (Z/X/C/R/F/T).
 * Contains the loadout slot index and how many ticks the key was held.
 * The server clamps chargeTicks to [0, MAX_CHARGE_TICKS] for anti-cheat.
 *
 * For non-Striker abilities, the server falls through to normal handling.
 */
public record UseChargedAbilityPayload(int slot, int chargeTicks) implements CustomPacketPayload {

	public static final Type<UseChargedAbilityPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "use_charged_ability"));

	public static final StreamCodec<ByteBuf, UseChargedAbilityPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, UseChargedAbilityPayload::slot,
					ByteBufCodecs.VAR_INT, UseChargedAbilityPayload::chargeTicks,
					UseChargedAbilityPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

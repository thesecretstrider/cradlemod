package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: activates the ability in the given loadout slot.
 * Sent when the player presses a slot keybind (Z/X/C/R/F/T).
 * The server looks up the ability from the player's loadout and delegates
 * to AbilityExecutor for validation + execution.
 */
public record UseAbilityPayload(int slot) implements CustomPacketPayload {

	public static final Type<UseAbilityPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "use_ability"));

	public static final StreamCodec<ByteBuf, UseAbilityPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, UseAbilityPayload::slot,
					UseAbilityPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

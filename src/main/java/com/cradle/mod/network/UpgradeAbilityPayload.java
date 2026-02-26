package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server: spend 1 upgrade point to level up ability in the given slot.
 */
public record UpgradeAbilityPayload(int slot) implements CustomPacketPayload {

	public static final Type<UpgradeAbilityPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "upgrade_ability"));

	public static final StreamCodec<ByteBuf, UpgradeAbilityPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, UpgradeAbilityPayload::slot,
					UpgradeAbilityPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

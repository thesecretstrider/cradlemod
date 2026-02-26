package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server: branch an ability from sourceSlot into targetSlot.
 * Source ability resets to level 1, branch ability fills target at level 1.
 * Requires source ability at upgrade level >= branchLevel (typically 10).
 */
public record BranchAbilityPayload(int sourceSlot, int targetSlot) implements CustomPacketPayload {

	public static final Type<BranchAbilityPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "branch_ability"));

	public static final StreamCodec<ByteBuf, BranchAbilityPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, BranchAbilityPayload::sourceSlot,
					ByteBufCodecs.VAR_INT, BranchAbilityPayload::targetSlot,
					BranchAbilityPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

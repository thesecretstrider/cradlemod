package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: Tells client to request dialogue from the given NPC.
 */
public record OpenDialoguePayload(String npcId, int entityId) implements CustomPacketPayload {
	public static final Type<OpenDialoguePayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "open_dialogue"));
	public static final StreamCodec<ByteBuf, OpenDialoguePayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, OpenDialoguePayload::npcId,
					ByteBufCodecs.INT, OpenDialoguePayload::entityId,
					OpenDialoguePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

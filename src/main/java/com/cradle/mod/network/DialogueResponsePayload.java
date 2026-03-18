package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DialogueResponsePayload(int entityId, int optionIndex) implements CustomPacketPayload {
    public static final Type<DialogueResponsePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "dialogue_response"));
    public static final StreamCodec<ByteBuf, DialogueResponsePayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT, DialogueResponsePayload::entityId,
            ByteBufCodecs.INT, DialogueResponsePayload::optionIndex,
            DialogueResponsePayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.List;

public record DialogueNodePayload(
    int entityId,
    String speakerName,
    String text,
    List<String> optionLabels,
    boolean hasMore  // false if this is the end of dialogue (all nextNodeIds are null)
) implements CustomPacketPayload {
    public static final Type<DialogueNodePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "dialogue_node"));

    public static final StreamCodec<ByteBuf, DialogueNodePayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT, DialogueNodePayload::entityId,
            ByteBufCodecs.STRING_UTF8, DialogueNodePayload::speakerName,
            ByteBufCodecs.STRING_UTF8, DialogueNodePayload::text,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), DialogueNodePayload::optionLabels,
            ByteBufCodecs.BOOL, DialogueNodePayload::hasMore,
            DialogueNodePayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

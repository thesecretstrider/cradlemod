package com.cradle.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client: full loadout sync.
 * Sent on join and whenever the loadout changes (equip, swap, upgrade, branch).
 *
 * The loadout is encoded as a compact JSON string to avoid the 12-field
 * StreamCodec.composite limit. Active slot flags are packed in an int.
 *
 * JSON format: {"slots":[{"id":"ability_id","lvl":1},null,...],"up":3}
 *
 * Active flags bitmask (bits 0-5 map to slots 0-5):
 *   bit 0 (1)  = slot 0 active
 *   bit 1 (2)  = slot 1 active
 *   bit 2 (4)  = slot 2 active
 *   bit 3 (8)  = slot 3 active
 *   bit 4 (16) = slot 4 active
 *   bit 5 (32) = slot 5 active
 */
public record AbilityLoadoutSyncPayload(
		String loadoutJson,
		int activeFlags
) implements CustomPacketPayload {

	public static final Type<AbilityLoadoutSyncPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("cradlemod", "loadout_sync"));

	public static final StreamCodec<ByteBuf, AbilityLoadoutSyncPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, AbilityLoadoutSyncPayload::loadoutJson,
					ByteBufCodecs.VAR_INT, AbilityLoadoutSyncPayload::activeFlags,
					AbilityLoadoutSyncPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * Build active flags from a boolean array (up to 6 slots).
	 */
	public static int buildActiveFlags(boolean[] slotActive) {
		int flags = 0;
		for (int i = 0; i < Math.min(slotActive.length, 6); i++) {
			if (slotActive[i]) flags |= (1 << i);
		}
		return flags;
	}

	/**
	 * Check if a specific slot is active from the flags.
	 */
	public boolean isSlotActive(int slot) {
		return slot >= 0 && slot < 6 && (activeFlags & (1 << slot)) != 0;
	}
}

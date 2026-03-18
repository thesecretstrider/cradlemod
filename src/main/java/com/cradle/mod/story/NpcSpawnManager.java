package com.cradle.mod.story;

import com.cradle.mod.entity.CradleEntities;
import com.cradle.mod.entity.StoryNpcEntity;
import com.cradle.mod.worldgen.ValleyHeightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Spawns all story NPCs when a Cradle mode world first loads.
 * Tracks whether NPCs have been spawned via world data persistence.
 */
public class NpcSpawnManager {
	private static boolean npcsSpawned = false;

	public static void spawnIfNeeded(ServerLevel level) {
		if (!GameModeManager.isCradleMode() || npcsSpawned) return;

		// Wei clan NPCs
		spawnNpc(level, "elder_whisper", "Elder Whisper", -300, -300, "", "MONARCH", "WEI");
		spawnNpc(level, "wei_shi_jaran", "Wei Shi Jaran", -310, -290, "", "JADE", "WEI");
		spawnNpc(level, "wei_shi_seisha", "Wei Shi Seisha", -290, -310, "", "JADE", "WEI");
		spawnNpc(level, "heaven_glory_elder", "Heaven's Glory Elder", -400, 300, "", "TRUEGOLD", "HEAVENS_GLORY");
		spawnNpc(level, "kral", "Kral", -390, 290, "", "IRON", "HEAVENS_GLORY");

		// Independent NPCs
		spawnNpc(level, "yerin", "Yerin", -100, -100, "ENDLESS_SWORD", "LOWGOLD", "INDEPENDENT");
		spawnNpc(level, "suriel", "Suriel", 0, 0, "", "MONARCH", "ABIDAN");

		npcsSpawned = true;
	}

	private static void spawnNpc(ServerLevel level, String npcId, String displayName,
								 int x, int z, String path, String stage, String faction) {
		int y = ValleyHeightmap.getHeight(x, z) + 1;
		StoryNpcEntity npc = new StoryNpcEntity(CradleEntities.STORY_NPC, level);
		npc.setPos(x + 0.5, y, z + 0.5);
		npc.setNpcId(npcId);
		npc.setNpcDisplayName(displayName);
		npc.setNpcPath(path);
		npc.setNpcStage(stage);
		npc.setFaction(faction);
		level.addFreshEntity(npc);
	}

	// ── Persistence ──────────────────────────────────────────────────

	public static void writeNbt(CompoundTag tag) {
		tag.putBoolean("npcsSpawned", npcsSpawned);
	}

	public static void readNbt(CompoundTag tag) {
		npcsSpawned = tag.getBooleanOr("npcsSpawned", false);
	}

	public static void reset() {
		npcsSpawned = false;
	}
}

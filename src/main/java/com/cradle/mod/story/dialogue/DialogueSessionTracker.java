package com.cradle.mod.story.dialogue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks the current dialogue node for each player-NPC pair.
 */
public class DialogueSessionTracker {
    // key: "playerUUID:npcId" -> current node ID
    private static final Map<String, String> sessions = new HashMap<>();

    private static String key(UUID playerId, String npcId) {
        return playerId.toString() + ":" + npcId;
    }

    public static void setCurrentNode(UUID playerId, String npcId, String nodeId) {
        sessions.put(key(playerId, npcId), nodeId);
    }

    public static String getCurrentNode(UUID playerId, String npcId) {
        return sessions.get(key(playerId, npcId));
    }

    public static void clearSession(UUID playerId, String npcId) {
        sessions.remove(key(playerId, npcId));
    }

    public static void clearAll() {
        sessions.clear();
    }
}

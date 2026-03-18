package com.cradle.mod.story.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DialogueLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("CradleMod");
    private static final Map<String, DialogueTree> DIALOGUE_TREES = new HashMap<>();

    public static void loadAll() {
        DIALOGUE_TREES.clear();
        // Load known dialogue files
        String[] npcIds = {"elder_whisper", "yerin", "wei_shi_jaran", "suriel"};
        for (String npcId : npcIds) {
            loadDialogueFile(npcId);
        }
        LOGGER.info("Loaded {} dialogue trees.", DIALOGUE_TREES.size());
    }

    private static void loadDialogueFile(String npcId) {
        String resourcePath = "/data/cradlemod/dialogue/" + npcId + ".json";
        try (InputStream stream = DialogueLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                LOGGER.debug("No dialogue file found for NPC: {}", npcId);
                return;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();

            String startNodeId = root.get("startNodeId").getAsString();
            Map<String, DialogueNode> nodes = new LinkedHashMap<>();

            JsonObject nodesObj = root.getAsJsonObject("nodes");
            for (Map.Entry<String, JsonElement> entry : nodesObj.entrySet()) {
                String nodeId = entry.getKey();
                JsonObject nodeObj = entry.getValue().getAsJsonObject();

                String speakerName = nodeObj.get("speakerName").getAsString();
                String text = nodeObj.get("text").getAsString();

                List<DialogueOption> options = new ArrayList<>();
                JsonArray optionsArr = nodeObj.getAsJsonArray("options");
                if (optionsArr != null) {
                    for (JsonElement optEl : optionsArr) {
                        JsonObject optObj = optEl.getAsJsonObject();
                        String label = optObj.get("label").getAsString();
                        String nextNodeId = optObj.has("nextNodeId") && !optObj.get("nextNodeId").isJsonNull()
                            ? optObj.get("nextNodeId").getAsString() : null;
                        String requiredFlag = optObj.has("requiredFlag") && !optObj.get("requiredFlag").isJsonNull()
                            ? optObj.get("requiredFlag").getAsString() : null;
                        String setFlag = optObj.has("setFlag") && !optObj.get("setFlag").isJsonNull()
                            ? optObj.get("setFlag").getAsString() : null;
                        String giveItem = optObj.has("giveItem") && !optObj.get("giveItem").isJsonNull()
                            ? optObj.get("giveItem").getAsString() : null;
                        String advanceObjective = optObj.has("advanceObjective") && !optObj.get("advanceObjective").isJsonNull()
                            ? optObj.get("advanceObjective").getAsString() : null;
                        options.add(new DialogueOption(label, nextNodeId, requiredFlag, setFlag, giveItem, advanceObjective));
                    }
                }

                nodes.put(nodeId, new DialogueNode(nodeId, speakerName, text, options));
            }

            DIALOGUE_TREES.put(npcId, new DialogueTree(npcId, startNodeId, nodes));
            LOGGER.info("Loaded dialogue tree for NPC: {} ({} nodes)", npcId, nodes.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load dialogue for NPC: {}", npcId, e);
        }
    }

    public static DialogueTree getTree(String npcId) {
        return DIALOGUE_TREES.get(npcId);
    }

    public static boolean hasTree(String npcId) {
        return DIALOGUE_TREES.containsKey(npcId);
    }
}

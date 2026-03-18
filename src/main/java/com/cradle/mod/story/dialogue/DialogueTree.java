package com.cradle.mod.story.dialogue;

import java.util.Map;

public record DialogueTree(
    String npcId,
    String startNodeId,
    Map<String, DialogueNode> nodes
) {
    public DialogueNode getStartNode() {
        return nodes.get(startNodeId);
    }

    public DialogueNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }
}

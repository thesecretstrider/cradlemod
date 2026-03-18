package com.cradle.mod.story.dialogue;

public record DialogueOption(
    String label,
    String nextNodeId,      // null = end conversation
    String requiredFlag,    // null = always available
    String setFlag,         // null = none
    String giveItem,        // null = none
    String advanceObjective // null = none
) {}

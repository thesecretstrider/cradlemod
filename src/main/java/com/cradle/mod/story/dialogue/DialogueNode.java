package com.cradle.mod.story.dialogue;

import java.util.List;

public record DialogueNode(
    String id,
    String speakerName,
    String text,
    List<DialogueOption> options
) {}

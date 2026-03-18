package com.cradle.mod.entity;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * Render state for Story NPC entities.
 * Holds display and faction data needed for client-side rendering decisions.
 */
public class StoryNpcRenderState extends HumanoidRenderState {
	public String npcDisplayName = "NPC";
	public String npcPath = "";
	public String npcStage = "";
	public String faction = "";
}

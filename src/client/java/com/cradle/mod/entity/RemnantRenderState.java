package com.cradle.mod.entity;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * Render state for Remnant entities.
 * Holds path and power data needed for client-side rendering decisions.
 */
public class RemnantRenderState extends HumanoidRenderState {
	public String remnantPath = "BLACK_FLAME";
	public int powerLevel = 1;
}

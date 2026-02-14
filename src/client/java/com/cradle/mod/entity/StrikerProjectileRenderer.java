package com.cradle.mod.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Renderer for StrikerProjectileEntity. The projectile is invisible itself —
 * players see it via its particle trail (soul fire, crits, end rods, etc.).
 */
public class StrikerProjectileRenderer extends EntityRenderer<StrikerProjectileEntity, EntityRenderState> {

	public StrikerProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}
}

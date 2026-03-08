package com.cradle.mod.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Renders Remnant entities as ghostly, semi-transparent humanoid figures
 * with path-specific color tinting.
 *
 * Uses HumanoidMobRenderer with the standard player model. The ghostly effect
 * is achieved by:
 * - Using entityTranslucent render type for see-through appearance
 * - Applying path-specific color tinting via getModelTint()
 */
public class RemnantRenderer extends HumanoidMobRenderer<RemnantEntity, RemnantRenderState, HumanoidModel<RemnantRenderState>> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("cradlemod", "textures/entity/remnant.png");

	public RemnantRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.3f);
	}

	@Override
	public RemnantRenderState createRenderState() {
		return new RemnantRenderState();
	}

	@Override
	public void extractRenderState(RemnantEntity entity, RemnantRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.remnantPath = entity.getRemnantPathName();
		state.powerLevel = entity.getPowerLevel();
	}

	@Override
	public Identifier getTextureLocation(RemnantRenderState state) {
		return TEXTURE;
	}

	@Override
	protected RenderType getRenderType(RemnantRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		// Always use translucent rendering for ghostly appearance
		return RenderTypes.entityTranslucent(TEXTURE);
	}

	@Override
	protected int getModelTint(RemnantRenderState state) {
		// Return ARGB color with 50% alpha for ghostly transparency + path color
		return getPathColorWithAlpha(state.remnantPath);
	}

	/**
	 * Returns an ARGB color with 50% alpha for the given path.
	 */
	private static int getPathColorWithAlpha(String pathName) {
		return switch (pathName) {
			case "BLACK_FLAME" -> 0x80FF4400;    // Dark orange/red, 50% alpha
			case "ENDLESS_SWORD" -> 0x80CCCCDE;  // Silver, 50% alpha
			case "STELLAR_SPEAR" -> 0x80FFDE44;  // Gold, 50% alpha
			case "CLOUD_HAMMER" -> 0x808787CC;   // Pale blue, 50% alpha
			case "HOLLOW_KING" -> 0x80DEDEFF;    // Pale white, 50% alpha
			default -> 0x80B3B3B3;               // Gray, 50% alpha
		};
	}
}

package com.cradle.mod.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renders Story NPC entities as normal solid humanoid figures
 * using the default Steve player skin.
 */
public class StoryNpcRenderer extends HumanoidMobRenderer<StoryNpcEntity, StoryNpcRenderState, HumanoidModel<StoryNpcRenderState>> {

	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");

	public StoryNpcRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
	}

	@Override
	public StoryNpcRenderState createRenderState() {
		return new StoryNpcRenderState();
	}

	@Override
	public void extractRenderState(StoryNpcEntity entity, StoryNpcRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.npcDisplayName = entity.getNpcDisplayName();
		state.npcPath = entity.getNpcPath();
		state.npcStage = entity.getNpcStage();
		state.faction = entity.getFaction();
	}

	@Override
	public Identifier getTextureLocation(StoryNpcRenderState state) {
		return TEXTURE;
	}
}

package com.cradle.mod.render;

import com.cradle.mod.ClientCradleData;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Renders a goldsign overlay texture on top of any player who has one.
 *
 * Uses the player model's full geometry with a separate overlay texture
 * (e.g., metallic hair for Stellar Spear). The overlay renders using
 * entityTranslucent for future glow/transparency support.
 *
 * Supports multiplayer: uses {@link ClientCradleData#getGoldsignForPlayer}
 * to resolve goldsign ordinals per-player (local via cached data, remote via UUID map).
 */
public class GoldsignFeatureRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {

	/**
	 * Map of goldsign ordinal to overlay texture location.
	 * Ordinals must match CradlePlayerData.Goldsign enum order.
	 * Only goldsigns with overlay textures are included.
	 */
	private static final Map<Integer, Identifier> GOLDSIGN_TEXTURES = Map.of(
			3, Identifier.fromNamespaceAndPath("cradlemod",
					"textures/entity/goldsigns/stellar_spear.png")
			// Future: add other overlay-based goldsigns here
	);

	public GoldsignFeatureRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
					   AvatarRenderState state, float yRot, float xRot) {
		// Per-player goldsign lookup: local player uses cached data, remote players use UUID map
		int goldsignOrdinal = ClientCradleData.getGoldsignForPlayer(state);
		if (goldsignOrdinal == 0) return; // NONE — no goldsign to render

		Identifier texture = GOLDSIGN_TEXTURES.get(goldsignOrdinal);
		if (texture == null) return; // This goldsign type doesn't have an overlay texture

		// Don't render the overlay if the player is invisible
		if (state.isInvisible) return;

		// Render the overlay using the parent player model with the goldsign texture.
		// Uses entityTranslucent render type for future glow/transparency support.
		// Color -1 (0xFFFFFFFF) = fully opaque white, no tinting.
		// order(1) ensures overlay renders after base player skin
		submitNodeCollector.order(1).submitModel(
				this.getParentModel(),
				state,
				poseStack,
				RenderTypes.entityTranslucent(texture),
				packedLight,
				OverlayTexture.NO_OVERLAY,
				-1,           // color: white (no tint)
				null,         // no atlas sprite
				state.outlineColor,
				null          // no crumbling overlay
		);
	}
}

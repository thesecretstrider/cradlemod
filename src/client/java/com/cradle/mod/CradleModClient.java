package com.cradle.mod;

import com.cradle.mod.network.CradleSyncPayload;
import com.cradle.mod.network.OpenInfoScreenPayload;
import com.cradle.mod.network.OpenPathSelectionPayload;
import com.cradle.mod.network.ToggleIronBodyPayload;
import com.cradle.mod.network.UseEnforcerPayload;
import com.cradle.mod.network.UseStrikerPayload;
import com.cradle.mod.network.UseRulerPayload;
import com.cradle.mod.network.ToggleCyclingPayload;
import com.cradle.mod.block.CradleBlocks;
import com.cradle.mod.entity.CradleEntities;
import com.cradle.mod.entity.StrikerProjectileRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class CradleModClient implements ClientModInitializer {
	// (Welcome overlay system removed — replaced by WelcomeScreen)

	// Custom keybind category — appears as its own section in Controls settings
	private static final KeyMapping.Category CRADLE_CATEGORY =
			KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("cradlemod", "cradle"));

	// Keybind: press J to open Sacred Artist Status screen
	private static final KeyMapping INFO_KEYBIND = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(
					"key.cradlemod.info",
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_J,
					CRADLE_CATEGORY
			)
	);

	// Keybind: press P to toggle Iron Body
	private static final KeyMapping IRON_BODY_KEYBIND = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(
					"key.cradlemod.iron_body",
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_P,
					CRADLE_CATEGORY
			)
	);

	// Keybind: press Z to toggle Enforcer technique
	private static final KeyMapping ENFORCER_KEYBIND = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(
					"key.cradlemod.enforcer",
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_Z,
					CRADLE_CATEGORY
			)
	);

	// Keybind: press X to fire Striker technique
	private static final KeyMapping STRIKER_KEYBIND = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(
					"key.cradlemod.striker",
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_X,
					CRADLE_CATEGORY
			)
	);

	// Keybind: press C to toggle Ruler technique
	private static final KeyMapping RULER_KEYBIND = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(
					"key.cradlemod.ruler",
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_C,
					CRADLE_CATEGORY
			)
	);

	// Keybind: press G to toggle cycling
	private static final KeyMapping CYCLING_KEYBIND = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(
					"key.cradlemod.cycling",
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_G,
					CRADLE_CATEGORY
			)
	);

	@Override
	public void onInitializeClient() {
		// ── Register block render layers (cutout for transparency) ──
		BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT,
				CradleBlocks.VITAL_FRUIT_BUSH, CradleBlocks.SPIRIT_FRUIT_BUSH,
				CradleBlocks.BLOODFORGED_CRYSTAL, CradleBlocks.STEELBORN_CRYSTAL,
				CradleBlocks.RAINDROP_CRYSTAL);

		// ── Register entity renderers ────────────────────────────────
		EntityRendererRegistry.register(CradleEntities.STRIKER_PROJECTILE, StrikerProjectileRenderer::new);

		// ── Reset client data when disconnecting ─────────────────────
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientCradleData.reset();
		});

		// ── Networking: receive sync packet ───────────────────────────
		ClientPlayNetworking.registerGlobalReceiver(CradleSyncPayload.TYPE,
				(payload, context) -> {
					// Update client cache (already on client thread via Fabric API)
					ClientCradleData.update(payload);
				}
		);

		// ── Networking: receive open-info-screen packet ──────────────
		ClientPlayNetworking.registerGlobalReceiver(OpenInfoScreenPayload.TYPE,
				(payload, context) -> {
					context.client().execute(() -> {
						context.client().setScreen(new CradleInfoScreen());
					});
				}
		);

		// ── Networking: receive open-path-selection packet ───────────
		// If the player hasn't chosen a path, show the WelcomeScreen first,
		// which leads into the PathSelectionScreen after clicking "Begin Your Journey".
		ClientPlayNetworking.registerGlobalReceiver(OpenPathSelectionPayload.TYPE,
				(payload, context) -> {
					context.client().execute(() -> {
						if (!ClientCradleData.hasChosenPath()) {
							context.client().setScreen(new WelcomeScreen());
						} else {
							context.client().setScreen(new PathSelectionScreen());
						}
					});
				}
		);

		// ── Keybind + Cycling Particles ──────────────────────────────
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (INFO_KEYBIND.consumeClick()) {
				client.setScreen(new CradleInfoScreen());
			}
			while (IRON_BODY_KEYBIND.consumeClick()) {
				ClientPlayNetworking.send(new ToggleIronBodyPayload());
			}
			while (ENFORCER_KEYBIND.consumeClick()) {
				ClientPlayNetworking.send(new UseEnforcerPayload());
			}
			while (STRIKER_KEYBIND.consumeClick()) {
				ClientPlayNetworking.send(new UseStrikerPayload());
			}
			while (RULER_KEYBIND.consumeClick()) {
				ClientPlayNetworking.send(new UseRulerPayload());
			}
			while (CYCLING_KEYBIND.consumeClick()) {
				ClientPlayNetworking.send(new ToggleCyclingPayload());
			}
			CyclingParticleRenderer.tick(client);
		});
	}

}

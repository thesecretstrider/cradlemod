package com.cradle.mod;

import com.cradle.mod.network.CradleSyncPayload;
import com.cradle.mod.network.OpenInfoScreenPayload;
import com.cradle.mod.network.OpenPathSelectionPayload;
import com.cradle.mod.network.ToggleIronBodyPayload;
import com.cradle.mod.network.UseEnforcerPayload;
import com.cradle.mod.network.UseStrikerPayload;
import com.cradle.mod.network.UseRulerPayload;
import com.cradle.mod.network.ToggleCyclingPayload;
import com.cradle.mod.network.UseSagePayload;
import com.cradle.mod.network.UseHeraldPayload;
import com.cradle.mod.network.OpenIconSelectionPayload;
import com.cradle.mod.network.DuelInviteReceivedPayload;
import com.cradle.mod.network.DuelEndPayload;
import com.cradle.mod.block.CradleBlocks;
import com.cradle.mod.entity.CradleEntities;
import com.cradle.mod.entity.StrikerProjectileRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
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

	// Keybind: press V for Sage Authority (tap = Stop, hold 1s = Kill)
	private static final KeyMapping SAGE_KEYBIND = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(
					"key.cradlemod.sage",
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_V,
					CRADLE_CATEGORY
			)
	);

	// Keybind: press B to toggle Herald Spirit Shift
	private static final KeyMapping HERALD_KEYBIND = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(
					"key.cradlemod.herald",
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_B,
					CRADLE_CATEGORY
			)
	);

	// Sage keybind hold detection state
	private static int sageKeyHeldTicks = 0;
	private static boolean sageKeyWasDown = false;

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

		// ── Networking: receive open-icon-selection packet ───────────
		ClientPlayNetworking.registerGlobalReceiver(OpenIconSelectionPayload.TYPE,
				(payload, context) -> {
					context.client().execute(() -> {
						context.client().setScreen(new IconSelectionScreen(payload.pathName(), payload.forMonarch()));
					});
				}
		);

		// ── Networking: receive duel invite notification ────────────
		// Server sends clickable chat messages directly; this handler is for
		// future client-side enhancements (e.g., sound effects, overlay).
		ClientPlayNetworking.registerGlobalReceiver(DuelInviteReceivedPayload.TYPE,
				(payload, context) -> {
					// Chat messages with clickable [Accept] [Decline] are sent server-side
				}
		);

		// ── Networking: receive duel end notification ────────────────
		ClientPlayNetworking.registerGlobalReceiver(DuelEndPayload.TYPE,
				(payload, context) -> {
					// Server already handles titles and chat; this payload can be used
					// for future client-side stat display or duel history screen
				}
		);

		// ── Willpower bar (left side of screen, vertical, blue) ─────
		HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
			if (!ClientCradleData.hasWillpower()) return;

			Minecraft mc = Minecraft.getInstance();
			int screenHeight = mc.getWindow().getGuiScaledHeight();

			// Bar dimensions and position
			int barWidth = 6;
			int barHeight = 60;
			int barX = 4;                                    // 4px from left edge
			int barY = (screenHeight / 2) - (barHeight / 2); // vertically centered

			// Dark background
			graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF222222);

			// Blue fill from bottom, based on willpower ratio
			float ratio = ClientCradleData.maxWillpower > 0
					? ClientCradleData.currentWillpower / ClientCradleData.maxWillpower : 0f;
			int fillHeight = (int) (barHeight * Math.min(1f, ratio));
			if (fillHeight > 0) {
				graphics.fill(barX, barY + barHeight - fillHeight,
						barX + barWidth, barY + barHeight, 0xFF4488FF);
			}

			// 1px highlight border on left edge
			graphics.fill(barX, barY, barX + 1, barY + barHeight, 0x44FFFFFF);

			// "WP" label above the bar
			graphics.drawString(mc.font, "WP", barX - 1, barY - 10, 0xFF6699FF, false);
		});

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

			// Sage Authority: tap V = Stop, hold V for 20 ticks (1 second) = Kill
			boolean sageDown = SAGE_KEYBIND.isDown();
			if (sageDown) {
				sageKeyHeldTicks++;
				if (sageKeyHeldTicks == 20) {
					// Held for 1 second → KILL
					ClientPlayNetworking.send(new UseSagePayload("KILL"));
				}
			} else if (sageKeyWasDown) {
				// Key was released
				if (sageKeyHeldTicks > 0 && sageKeyHeldTicks < 20) {
					// Short tap → STOP
					ClientPlayNetworking.send(new UseSagePayload("STOP"));
				}
				sageKeyHeldTicks = 0;
			}
			sageKeyWasDown = sageDown;
			// Consume any queued clicks so they don't interfere
			while (SAGE_KEYBIND.consumeClick()) { /* consumed */ }

			// Herald Spirit Shift: press B to toggle
			while (HERALD_KEYBIND.consumeClick()) {
				ClientPlayNetworking.send(new UseHeraldPayload());
			}

			CyclingParticleRenderer.tick(client);
		});
	}

}

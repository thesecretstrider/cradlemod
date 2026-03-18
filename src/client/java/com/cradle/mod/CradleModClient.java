package com.cradle.mod;

import com.cradle.mod.network.CradleSyncPayload;
import com.cradle.mod.network.GoldsignBroadcastPayload;
import com.cradle.mod.network.OpenCharacterSelectionPayload;
import com.cradle.mod.network.OpenInfoScreenPayload;
import com.cradle.mod.network.OpenPathSelectionPayload;
import com.cradle.mod.network.ToggleIronBodyPayload;
import com.cradle.mod.network.ToggleCyclingPayload;
import com.cradle.mod.network.ToggleCopperSightPayload;
import com.cradle.mod.network.UseSagePayload;
import com.cradle.mod.network.UseHeraldPayload;
import com.cradle.mod.network.OpenIconSelectionPayload;
import com.cradle.mod.network.DuelInviteReceivedPayload;
import com.cradle.mod.network.DuelEndPayload;
import com.cradle.mod.network.AbilityLoadoutSyncPayload;
import com.cradle.mod.network.UseAbilityPayload;
import com.cradle.mod.network.UseChargedAbilityPayload;
import com.cradle.mod.render.GoldsignFeatureRenderer;
import com.cradle.mod.block.CradleBlocks;
import com.cradle.mod.entity.CradleEntities;
import com.cradle.mod.entity.RemnantRenderer;
import com.cradle.mod.entity.StoryNpcRenderer;
import com.cradle.mod.entity.StrikerProjectileRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

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

	// ── Ability Slot Keybinds (6 slots, new skill tree system) ───────
	// Slots 0-5 mapped to Z/X/C/R/F/T. Also sends old payloads for backward compat.
	private static final int[] SLOT_KEYS = {
			GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C,
			GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_T
	};
	private static final String[] SLOT_NAMES = {
			"key.cradlemod.slot0", "key.cradlemod.slot1", "key.cradlemod.slot2",
			"key.cradlemod.slot3", "key.cradlemod.slot4", "key.cradlemod.slot5"
	};
	private static final KeyMapping[] SLOT_KEYBINDS = new KeyMapping[6];
	static {
		for (int i = 0; i < 6; i++) {
			SLOT_KEYBINDS[i] = KeyBindingHelper.registerKeyBinding(
					new KeyMapping(SLOT_NAMES[i], InputConstants.Type.KEYSYM, SLOT_KEYS[i], CRADLE_CATEGORY));
		}
	}

	// Keybind: press K to open Skill Tree screen
	private static final KeyMapping SKILL_TREE_KEYBIND = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(
					"key.cradlemod.skill_tree",
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_K,
					CRADLE_CATEGORY
			)
	);

	// Keybind: press H to toggle Copper Sight (Copper+ only)
	private static final KeyMapping COPPER_SIGHT_KEYBIND = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(
					"key.cradlemod.copper_sight",
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_H,
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

	// Striker charge-up state (per slot)
	private static final int[] slotChargeTicks = new int[6];
	private static final boolean[] slotWasDown = new boolean[6];
	private static final int MAX_CHARGE_TICKS = 60;
	private static final float PROGRESSIVE_DRAIN_FRACTION = 0.5f;

	@Override
	public void onInitializeClient() {
		// ── Register block render layers (cutout for transparency) ──
		BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT,
				CradleBlocks.VITAL_FRUIT_BUSH, CradleBlocks.SPIRIT_FRUIT_BUSH,
				CradleBlocks.BLOODFORGED_CRYSTAL, CradleBlocks.STEELBORN_CRYSTAL,
				CradleBlocks.RAINDROP_CRYSTAL);

		// ── Register entity renderers ────────────────────────────────
		EntityRendererRegistry.register(CradleEntities.STRIKER_PROJECTILE, StrikerProjectileRenderer::new);
		EntityRendererRegistry.register(CradleEntities.REMNANT, RemnantRenderer::new);
		EntityRendererRegistry.register(CradleEntities.STORY_NPC, StoryNpcRenderer::new);

		// ── Register goldsign feature renderer on player renderers ───
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
				(entityType, entityRenderer, registrationHelper, context) -> {
					if (entityRenderer instanceof AvatarRenderer<?> avatarRenderer) {
						registrationHelper.register(new GoldsignFeatureRenderer(avatarRenderer));
					}
				}
		);

		// ── Reset client data when disconnecting ─────────────────────
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientCradleData.reset();
			ClientLoadoutData.reset();
		});

		// ── Networking: receive sync packet ───────────────────────────
		ClientPlayNetworking.registerGlobalReceiver(CradleSyncPayload.TYPE,
				(payload, context) -> {
					// Update client cache (already on client thread via Fabric API)
					ClientCradleData.update(payload);
					// Update loadout slot active flags from the tick-level sync
					ClientLoadoutData.updateActiveFlags(payload.getSlotActiveFlags());
				}
		);

		// ── Networking: receive loadout sync packet ──────────────────
		ClientPlayNetworking.registerGlobalReceiver(AbilityLoadoutSyncPayload.TYPE,
				(payload, context) -> {
					ClientLoadoutData.update(payload);
				}
		);

		// ── Networking: receive goldsign broadcast packet ─────────────
		ClientPlayNetworking.registerGlobalReceiver(GoldsignBroadcastPayload.TYPE,
				(payload, context) -> {
					ClientCradleData.setRemotePlayerGoldsign(
							UUID.fromString(payload.playerUuid()), payload.goldsignOrdinal());
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

		// ── Networking: receive open-character-selection packet ──────
		ClientPlayNetworking.registerGlobalReceiver(OpenCharacterSelectionPayload.TYPE,
				(payload, context) -> {
					context.client().execute(() -> {
						context.client().setScreen(new CharacterSelectionScreen());
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
		// Registered BEFORE chat layer so chat text renders on top
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				net.minecraft.resources.Identifier.fromNamespaceAndPath("cradlemod", "willpower_bar"),
				(graphics, deltaTracker) -> {
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

		// ── Ability Slot Bar HUD ─────────────────────────────────────
		// Registered BEFORE chat layer so chat text renders on top of ability icons
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				net.minecraft.resources.Identifier.fromNamespaceAndPath("cradlemod", "ability_slot_bar"),
				AbilitySlotBarRenderer::render);

		// ── Keybind + Cycling Particles ──────────────────────────────
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (INFO_KEYBIND.consumeClick()) {
				client.setScreen(new CradleInfoScreen());
			}
			while (IRON_BODY_KEYBIND.consumeClick()) {
				ClientPlayNetworking.send(new ToggleIronBodyPayload());
			}

			// Ability slot keybinds (Z/X/C/R/F/T → slots 0-5)
			// Strikers use hold-to-charge; Enforcer/Ruler keep instant toggle
			for (int i = 0; i < 6; i++) {
				String abilityId = ClientLoadoutData.getAbilityId(i);
				boolean isStriker = isStrikerAbility(abilityId);

				if (isStriker && abilityId != null) {
					// ── STRIKER: Hold-to-charge mechanic ──
					boolean keyDown = SLOT_KEYBINDS[i].isDown();

					if (keyDown) {
						if (!slotWasDown[i]) {
							// Key just pressed — start charging (skip if on cooldown)
							if (ClientLoadoutData.isOnCooldown(i)) {
								slotWasDown[i] = true; // Mark down but don't charge
								slotChargeTicks[i] = -1; // Sentinel: on cooldown
							} else {
								slotChargeTicks[i] = 0;
								ClientLoadoutData.startCharging(i);
							}
						}

						// Increment charge (skip if cooldown sentinel)
						if (slotChargeTicks[i] >= 0) {
							if (slotChargeTicks[i] < MAX_CHARGE_TICKS) {
								slotChargeTicks[i]++;
							}
							ClientLoadoutData.updateChargeTick(i, slotChargeTicks[i]);

							// Progressive madra drain (visual only — server validates)
							drainChargeMadra(abilityId);

							// Auto-fire if madra runs out
							if (ClientCradleData.currentMadra <= 0 && slotChargeTicks[i] > 0) {
								fireChargedAbility(i);
							}
						}
					} else if (slotWasDown[i]) {
						// Key released — fire at current charge level
						if (slotChargeTicks[i] >= 0) {
							fireChargedAbility(i);
						} else {
							// Was on cooldown — just reset
							slotChargeTicks[i] = 0;
						}
					}
					slotWasDown[i] = keyDown;

					// Consume queued clicks so they don't interfere
					while (SLOT_KEYBINDS[i].consumeClick()) { /* consumed */ }
				} else {
					// ── NON-STRIKER (Enforcer/Ruler): Instant toggle ──
					while (SLOT_KEYBINDS[i].consumeClick()) {
						ClientPlayNetworking.send(new UseAbilityPayload(i));
					}
					// Reset any stale charge state
					slotChargeTicks[i] = 0;
					slotWasDown[i] = false;
					ClientLoadoutData.stopCharging(i);
				}
			}

			// K key: open Skill Tree screen
			while (SKILL_TREE_KEYBIND.consumeClick()) {
				client.setScreen(new SkillTreeScreen());
			}

			// H key: toggle Copper Sight (Copper+ only)
			while (COPPER_SIGHT_KEYBIND.consumeClick()) {
				if (ClientCradleData.isCopper()) {
					ClientPlayNetworking.send(new ToggleCopperSightPayload());
				}
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
			AuraParticleRenderer.tick(client);
		});
	}

	/**
	 * Check if an ability ID is a striker type (for cooldown display).
	 * Uses the same Sets defined in SkillTreeScreen.
	 */
	private static boolean isStrikerAbility(String id) {
		if (id == null) return false;
		return SkillTreeScreen.STRIKER_IDS.contains(id);
	}

	/**
	 * Get approximate cooldown for a striker ability (for visual display).
	 * Must match the values in AbilityDefinitions on the server.
	 */
	private static long getStrikerCooldownMs(String id) {
		if (id == null) return 2000L;
		return switch (id) {
			// Base strikers: 2 seconds
			case "blackflame_burst", "endless_slash", "stellar_piercing_star",
				 "cloud_falling_hammer", "hollow_empty_palm" -> 2000L;
			// Branch strikers: longer cooldowns
			case "blackflame_meteor" -> 4000L;
			case "endless_sword_storm" -> 3500L;
			case "stellar_nova" -> 4000L;
			case "cloud_thunderstrike" -> 5000L;
			case "hollow_nullify" -> 3500L;
			// Universal striker
			case "universal_spirit_pulse" -> 3000L;
			default -> 2000L;
		};
	}

	// ── Striker charge-up helpers ────────────────────────────────────

	/**
	 * Fire a charged striker ability and clean up charge state.
	 */
	private static void fireChargedAbility(int slot) {
		int chargeTicks = slotChargeTicks[slot];
		ClientPlayNetworking.send(new UseChargedAbilityPayload(slot, chargeTicks));

		// Start visual cooldown
		String abilityId = ClientLoadoutData.getAbilityId(slot);
		if (abilityId != null) {
			long cooldownMs = getStrikerCooldownMs(abilityId);
			ClientLoadoutData.startCooldown(slot, cooldownMs);
		}

		// Reset charge state
		slotChargeTicks[slot] = 0;
		slotWasDown[slot] = false;
		ClientLoadoutData.stopCharging(slot);
	}

	/**
	 * Drain a small amount of madra per tick while charging (client-side visual only).
	 * Server validates the real cost on fire.
	 */
	private static void drainChargeMadra(String abilityId) {
		float baseCost = getApproxBaseMadraCost(abilityId);
		float drainPerTick = (baseCost * PROGRESSIVE_DRAIN_FRACTION) / MAX_CHARGE_TICKS;
		ClientCradleData.currentMadra = Math.max(0, ClientCradleData.currentMadra - drainPerTick);
	}

	/**
	 * Approximate base madra cost for client-side drain preview.
	 * Must roughly match server AbilityDefinition values.
	 */
	private static float getApproxBaseMadraCost(String id) {
		if (id == null) return 15.0f;
		return switch (id) {
			case "blackflame_burst", "endless_slash", "stellar_piercing_star",
				 "cloud_falling_hammer", "hollow_empty_palm" -> 15.0f;
			case "blackflame_meteor" -> 25.0f;
			case "endless_sword_storm" -> 30.0f;
			case "stellar_nova" -> 28.0f;
			case "cloud_thunderstrike" -> 28.0f;
			case "hollow_nullify" -> 22.0f;
			case "universal_spirit_pulse" -> 18.0f;
			default -> 15.0f;
		};
	}
}

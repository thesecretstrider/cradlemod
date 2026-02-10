package com.cradle.mod;

import com.cradle.mod.network.CradleSyncPayload;
import com.cradle.mod.network.OpenInfoScreenPayload;
import com.cradle.mod.network.OpenPathSelectionPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class CradleModClient implements ClientModInitializer {
	private static final String DEFAULT_WELCOME_TEXT = "Welcome sacred artist, you awaken in the world of Cradle";
	private static final Path WELCOME_TEXT_PATH = Path.of("cradlemod-welcome.txt");
	private static final Path WELCOME_SEEN_PATH = FabricLoader.getInstance().getConfigDir().resolve("cradlemod-welcome-seen.properties");
	private static final Properties WELCOME_SEEN = loadWelcomeSeen();

	private static final int FADE_IN_TICKS = 20;
	private static final int STAY_TICKS = 100;
	private static final int FADE_OUT_TICKS = 20;
	private static final int TOTAL_TICKS = FADE_IN_TICKS + STAY_TICKS + FADE_OUT_TICKS;

	private static int welcomeTicksRemaining = 0;
	private static String welcomeMessage = "";

	// Keybind: press J to open Sacred Artist Status screen
	private static final KeyMapping INFO_KEYBIND = KeyBindingHelper.registerKeyBinding(
			new KeyMapping(
					"key.cradlemod.info",
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_J,
					KeyMapping.Category.MISC
			)
	);

	@Override
	public void onInitializeClient() {
		// ── Welcome message ───────────────────────────────────────────
		ClientPlayConnectionEvents.JOIN.register(new ClientPlayConnectionEvents.Join() {
			@Override
			public void onPlayReady(ClientPacketListener handler, PacketSender sender, Minecraft client) {
				showWelcomeMessageOncePerWorld(client);
			}
		});

		// ── Reset client data when disconnecting ─────────────────────
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientCradleData.reset();
		});

		HudRenderCallback.EVENT.register(CradleModClient::renderWelcomeOverlay);

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
		ClientPlayNetworking.registerGlobalReceiver(OpenPathSelectionPayload.TYPE,
				(payload, context) -> {
					context.client().execute(() -> {
						context.client().setScreen(new PathSelectionScreen());
					});
				}
		);

		// ── Keybind + Cycling Particles ──────────────────────────────
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (INFO_KEYBIND.consumeClick()) {
				client.setScreen(new CradleInfoScreen());
			}
			CyclingParticleRenderer.tick(client);
		});
	}

	// ── Welcome overlay rendering ─────────────────────────────────────

	private static final float TEXT_SCALE = 4.0f;

	private static void renderWelcomeOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		if (welcomeTicksRemaining <= 0) {
			return;
		}

		welcomeTicksRemaining--;

		float alpha;
		int elapsed = TOTAL_TICKS - welcomeTicksRemaining;
		if (elapsed < FADE_IN_TICKS) {
			alpha = (float) elapsed / FADE_IN_TICKS;
		} else if (welcomeTicksRemaining < FADE_OUT_TICKS) {
			alpha = (float) welcomeTicksRemaining / FADE_OUT_TICKS;
		} else {
			alpha = 1.0f;
		}

		int alphaInt = Mth.clamp((int) (alpha * 255), 0, 255);
		if (alphaInt <= 4) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		Font font = client.font;
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();

		int maxTextWidth = (int) (screenWidth / TEXT_SCALE) - 10;
		Component text = Component.literal(welcomeMessage);

		java.util.List<FormattedCharSequence> lines = font.split(text, maxTextWidth);
		int lineHeight = font.lineHeight + 2;
		int totalTextHeight = lines.size() * lineHeight;

		float blockTopY = (screenHeight - totalTextHeight * TEXT_SCALE) / 2.0f;

		int color = 0xFFFFFF | (alphaInt << 24);

		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(0, blockTopY);
		guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE);

		int y = 0;
		for (FormattedCharSequence line : lines) {
			int lineWidth = font.width(line);
			float x = (screenWidth / TEXT_SCALE - lineWidth) / 2.0f;
			guiGraphics.drawString(font, line, (int) x, y, color, true);
			y += lineHeight;
		}

		guiGraphics.pose().popMatrix();
	}

	// ── Welcome message logic ─────────────────────────────────────────

	private static void showWelcomeMessageOncePerWorld(Minecraft client) {
		client.execute(new Runnable() {
			@Override
			public void run() {
				String worldKey = worldKey(client);
				if (worldKey == null) {
					return;
				}

				if (WELCOME_SEEN.containsKey(worldKey)) {
					return;
				}

				WELCOME_SEEN.setProperty(worldKey, "true");
				saveWelcomeSeen();

				welcomeMessage = welcomeText();
				welcomeTicksRemaining = TOTAL_TICKS;
			}
		});
	}

	private static String worldKey(Minecraft client) {
		ServerData server = client.getCurrentServer();
		if (server != null) {
			return "server:" + server.ip;
		}

		IntegratedServer integratedServer = client.getSingleplayerServer();
		if (integratedServer != null) {
			return "singleplayer:" + integratedServer.getWorldData().getLevelName();
		}

		return "unknown";
	}

	private static String welcomeText() {
		try {
			if (Files.exists(WELCOME_TEXT_PATH)) {
				String text = Files.readString(WELCOME_TEXT_PATH, StandardCharsets.UTF_8).trim();
				if (!text.isEmpty()) {
					return text;
				}
			}
		} catch (IOException ignored) {
			// Fall back to the default message if the file can't be read.
		}

		return DEFAULT_WELCOME_TEXT;
	}

	private static Properties loadWelcomeSeen() {
		Properties properties = new Properties();
		if (!Files.exists(WELCOME_SEEN_PATH)) {
			return properties;
		}

		try (InputStream inputStream = Files.newInputStream(WELCOME_SEEN_PATH)) {
			properties.load(inputStream);
		} catch (IOException ignored) {
			// If the file can't be read, treat it as empty.
		}

		return properties;
	}

	private static void saveWelcomeSeen() {
		try {
			Files.createDirectories(WELCOME_SEEN_PATH.getParent());
		} catch (IOException ignored) {
			return;
		}

		try (OutputStream outputStream = Files.newOutputStream(WELCOME_SEEN_PATH)) {
			WELCOME_SEEN.store(outputStream, "Cradle mod welcome message state");
		} catch (IOException ignored) {
			// Best-effort; if saving fails, the message may show again next join.
		}
	}
}

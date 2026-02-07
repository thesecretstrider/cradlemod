package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ExampleModClient implements ClientModInitializer {
	private static final String DEFAULT_WELCOME_TEXT = "Welcome sacred artist, you awaken in the world of Cradle";
	private static final Path WELCOME_TEXT_PATH = Path.of("modid-welcome.txt");
	private static final Path WELCOME_SEEN_PATH = FabricLoader.getInstance().getConfigDir().resolve("modid-welcome-seen.properties");
	private static final Properties WELCOME_SEEN = loadWelcomeSeen();

	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.JOIN.register(new ClientPlayConnectionEvents.Join() {
			@Override
			public void onPlayReady(ClientPacketListener handler, PacketSender sender, Minecraft client) {
				showWelcomeMessageOncePerWorld(client);
			}
		});
	}

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

				Component welcome = Component.literal(welcomeText());
				client.gui.setTitle(welcome);
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

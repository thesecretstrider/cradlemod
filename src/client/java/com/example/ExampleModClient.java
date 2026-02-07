package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExampleModClient implements ClientModInitializer {
	private static final String DEFAULT_WELCOME_TEXT = "Welcome sacred artist, you awaken in the world of Cradle";
	private static final Path WELCOME_TEXT_PATH = Path.of("modid-welcome.txt");
	private static final int WELCOME_MESSAGE_TICKS = 20 * 10;

	private static int welcomeTicksRemaining = 0;
	private static String welcomeTextCached = DEFAULT_WELCOME_TEXT;

	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.JOIN.register(new ClientPlayConnectionEvents.Join() {
			@Override
			public void onPlayReady(ClientPacketListener handler, PacketSender sender, Minecraft client) {
				startWelcomeMessage(client);
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register(new ClientPlayConnectionEvents.Disconnect() {
			@Override
			public void onPlayDisconnect(ClientPacketListener handler, Minecraft client) {
				welcomeTicksRemaining = 0;
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
			@Override
			public void onEndTick(Minecraft client) {
				if (welcomeTicksRemaining <= 0 || client.player == null) {
					return;
				}

				client.player.displayClientMessage(Component.literal(welcomeTextCached), true);
				welcomeTicksRemaining--;
			}
		});
	}

	private static void startWelcomeMessage(Minecraft client) {
		client.execute(new Runnable() {
			@Override
			public void run() {
				welcomeTextCached = welcomeText();
				welcomeTicksRemaining = WELCOME_MESSAGE_TICKS;
			}
		});
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
}

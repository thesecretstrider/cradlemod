package com.example;

import net.fabricmc.api.ClientModInitializer;
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

	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.JOIN.register(new ClientPlayConnectionEvents.Join() {
			@Override
			public void onPlayReady(ClientPacketListener handler, PacketSender sender, Minecraft client) {
				showWelcomeMessage(client);
			}
		});
	}

	private static void showWelcomeMessage(Minecraft client) {
		client.execute(new Runnable() {
			@Override
			public void run() {
				if (client.player != null) {
					client.player.displayClientMessage(Component.literal(welcomeText()), true);
				}
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

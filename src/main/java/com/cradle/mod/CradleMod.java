package com.cradle.mod;

import com.cradle.mod.block.CradleBlocks;
import com.cradle.mod.item.CradleItems;
import com.cradle.mod.worldgen.CradleLootTables;
import com.cradle.mod.worldgen.CradleWorldGen;
import com.cradle.mod.network.AttemptAdvancePayload;
import com.cradle.mod.network.ChoosePathPayload;
import com.cradle.mod.network.CradleSyncPayload;
import com.cradle.mod.network.OpenInfoScreenPayload;
import com.cradle.mod.network.OpenPathSelectionPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CradleMod implements ModInitializer {
	public static final String MOD_ID = "cradlemod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final String DATA_FILE_NAME = "cradlemod_playerdata.dat";

	@Override
	public void onInitialize() {
		LOGGER.info("Cradle Mod initialized!");

		// Register custom items and blocks
		CradleItems.register();
		CradleBlocks.register();

		// Register worldgen (bush spawning) and loot table modifications (Spirit Stone in chests)
		CradleWorldGen.register();
		CradleLootTables.register();

		// Register networking packets (server -> client)
		PayloadTypeRegistry.playS2C().register(CradleSyncPayload.TYPE, CradleSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(OpenInfoScreenPayload.TYPE, OpenInfoScreenPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(OpenPathSelectionPayload.TYPE, OpenPathSelectionPayload.STREAM_CODEC);

		// Register networking packets (client -> server)
		PayloadTypeRegistry.playC2S().register(ChoosePathPayload.TYPE, ChoosePathPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(AttemptAdvancePayload.TYPE, AttemptAdvancePayload.STREAM_CODEC);

		// Send initial data sync when a player joins, and open path selection if needed
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
			ServerPlayNetworking.send(player, CyclingManager.createSyncPayload(player, data));

			// If the player hasn't chosen a path yet, open the selection screen
			if (!data.hasChosenPath()) {
				ServerPlayNetworking.send(player, new OpenPathSelectionPayload());
			}
		});

		// Handle path selection from the client
		ServerPlayNetworking.registerGlobalReceiver(ChoosePathPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			// Validate: player hasn't already chosen
			if (data.hasChosenPath()) {
				return;
			}

			// Validate: path name is a real Path enum value and not UNSET
			CradlePlayerData.Path path;
			try {
				path = CradlePlayerData.Path.valueOf(payload.pathName());
			} catch (IllegalArgumentException e) {
				return; // Invalid path name — ignore
			}
			if (path == CradlePlayerData.Path.UNSET) {
				return;
			}

			// Set the path
			data.setChosenPath(path);

			// Sync updated data to client
			ServerPlayNetworking.send(player, CyclingManager.createSyncPayload(player, data));

			// Send confirmation chat message
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7fYou have chosen the \u00A7e" + path.displayName() + "\u00A7f!"
			));
		});

		// Handle advancement attempt from the client (player clicked "Advance" button)
		ServerPlayNetworking.registerGlobalReceiver(AttemptAdvancePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			boolean success = BreakthroughManager.attemptBreakthrough(player, data);
			if (!success) {
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7cYou do not meet the requirements to advance."
				));
			}

			// Always re-sync so the client updates (button disappears, stage changes, etc.)
			ServerPlayNetworking.send(player, CyclingManager.createSyncPayload(player, data));
		});

		// Load player data when the server starts
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			Path dataFile = server.getWorldPath(LevelResource.ROOT).resolve(DATA_FILE_NAME);
			if (Files.exists(dataFile)) {
				try {
					CompoundTag root = NbtIo.readCompressed(dataFile, NbtAccounter.unlimitedHeap());
					CradlePlayerData.loadAll(root);
					LOGGER.info("Loaded Cradle player data for {} players.", CradlePlayerData.getAll().size());
				} catch (IOException e) {
					LOGGER.error("Failed to load Cradle player data!", e);
				}
			} else {
				LOGGER.info("No existing Cradle player data found, starting fresh.");
			}
		});

		// Save player data when the server stops, then clear in-memory cache
		// so stale data doesn't leak into the next world in the same MC session
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			Path dataFile = server.getWorldPath(LevelResource.ROOT).resolve(DATA_FILE_NAME);
			try {
				CompoundTag root = CradlePlayerData.saveAll();
				NbtIo.writeCompressed(root, dataFile);
				LOGGER.info("Saved Cradle player data for {} players.", CradlePlayerData.getAll().size());
			} catch (IOException e) {
				LOGGER.error("Failed to save Cradle player data!", e);
			}
			// Clear in-memory data so it doesn't carry over to the next world
			CradlePlayerData.clearAll();
		});

		// Register the cycling tick handler — runs every server tick (20x per second)
		ServerTickEvents.END_SERVER_TICK.register(CyclingManager::onServerTick);

		// Register /cycle commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			CycleCommand.register(dispatcher);
		});
	}
}

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
import com.cradle.mod.network.ToggleIronBodyPayload;
import com.cradle.mod.network.UseEnforcerPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CradleMod implements ModInitializer {
	public static final String MOD_ID = "cradlemod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final String DATA_FILE_NAME = "cradlemod_playerdata.dat";

	// Bloodforged Iron Body heal cooldown: 30 seconds (600 ticks)
	private static final long BLOODFORGED_COOLDOWN_MS = 30_000;
	private static final Map<UUID, Long> bloodforgedCooldowns = new HashMap<>();

	// Auto-save every 30 seconds (600 ticks at 20 tps)
	// Frequent saves protect against MC being closed without clean shutdown
	private static final int AUTO_SAVE_INTERVAL_TICKS = 600;
	private static int ticksSinceLastSave = 0;

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
		PayloadTypeRegistry.playC2S().register(ToggleIronBodyPayload.TYPE, ToggleIronBodyPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(UseEnforcerPayload.TYPE, UseEnforcerPayload.STREAM_CODEC);

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

			// Save immediately — path choice is critical data
			autoSave(player.level().getServer());

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
			} else {
				// Save immediately — breakthrough is critical data
				autoSave(player.level().getServer());
			}

			// Always re-sync so the client updates (button disappears, stage changes, etc.)
			ServerPlayNetworking.send(player, CyclingManager.createSyncPayload(player, data));
		});

		// Handle Iron Body activation from the client (player pressed P)
		// Bloodforged: instant heal burst (not a toggle)
		// Steelborn/Raindrop: toggle on/off
		ServerPlayNetworking.registerGlobalReceiver(ToggleIronBodyPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			if (data.getIronBody() == CradlePlayerData.IronBody.NONE) {
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7cYou don't have an Iron Body."
				));
				return;
			}

			if (data.getIronBody() == CradlePlayerData.IronBody.BLOODFORGED) {
				// Instant Health II burst with 30s cooldown
				long now = System.currentTimeMillis();
				Long lastUse = bloodforgedCooldowns.get(player.getUUID());
				if (lastUse != null && now - lastUse < BLOODFORGED_COOLDOWN_MS) {
					long remainingMs = BLOODFORGED_COOLDOWN_MS - (now - lastUse);
					int remainingSec = (int) Math.ceil(remainingMs / 1000.0);
					player.sendSystemMessage(Component.literal(
							"\u00A76[Cradle] \u00A7cBloodforged heal on cooldown! \u00A7e" + remainingSec + "s \u00A7cremaining."
					));
					return;
				}
				bloodforgedCooldowns.put(player.getUUID(), now);
				player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
						net.minecraft.world.effect.MobEffects.INSTANT_HEALTH, 1, 1, false, false));
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7c\u2764 Bloodforged Iron Body pulses with healing energy!"
				));
			} else {
				// Steelborn / Raindrop are toggles
				boolean newState = !data.isIronBodyActive();
				data.setIronBodyActive(newState);

				if (newState) {
					player.sendSystemMessage(Component.literal(
							"\u00A76[Cradle] \u00A7a" + data.getIronBody().displayName() +
									" Iron Body activated."
					));
				} else {
					player.sendSystemMessage(Component.literal(
							"\u00A76[Cradle] \u00A77Iron Body deactivated."
					));
				}
			}

			ServerPlayNetworking.send(player, CyclingManager.createSyncPayload(player, data));
		});

		// Handle Enforcer technique activation from the client (player pressed R)
		// Toggles the Enforcer technique on/off. Requires Copper stage or higher.
		ServerPlayNetworking.registerGlobalReceiver(UseEnforcerPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			// Must have chosen a path
			if (!data.hasChosenPath() || data.getChosenPath() == CradlePlayerData.Path.UNSET) {
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7cYou haven't chosen a path yet."
				));
				return;
			}

			// Must be at least Copper stage to use Enforcer
			if (data.getAdvancementStage().ordinal() < CradlePlayerData.AdvancementStage.COPPER.ordinal()) {
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7cEnforcer techniques require Copper stage or higher."
				));
				return;
			}

			// Toggle
			if (data.isEnforcerActive()) {
				CyclingManager.deactivateEnforcer(player, data);
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A77Enforcer technique deactivated."
				));
			} else {
				// Check if player has Madra
				if (data.getCurrentMadra() <= 0) {
					player.sendSystemMessage(Component.literal(
							"\u00A76[Cradle] \u00A7cNot enough Madra to activate Enforcer technique!"
					));
					return;
				}

				CyclingManager.activateEnforcer(player, data);

				// Send path-specific activation message
				String techniqueName = switch (data.getChosenPath()) {
					case BLACK_FLAME -> "Burning Body";
					case ENDLESS_SWORD -> "Flowing Edge";
					case STELLAR_SPEAR -> "Stellar Alignment";
					case CLOUD_HAMMER -> "Thunderous Weight";
					case HOLLOW_KING -> "Hollow Circulation";
					default -> "Enforcer Technique";
				};
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7a" + techniqueName + " activated!"
				));
			}

			ServerPlayNetworking.send(player, CyclingManager.createSyncPayload(player, data));
		});

		// Load player data BEFORE players can join (SERVER_STARTING fires before
		// any connection is accepted, unlike SERVER_STARTED which can race with JOIN)
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			ticksSinceLastSave = 0;
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
			autoSave(server);
			LOGGER.info("Saved Cradle player data for {} players on shutdown.", CradlePlayerData.getAll().size());
			// Clear in-memory data so it doesn't carry over to the next world
			CradlePlayerData.clearAll();
		});

		// Register the cycling tick handler — runs every server tick (20x per second)
		ServerTickEvents.END_SERVER_TICK.register(CyclingManager::onServerTick);

		// Crystal spawn manager — checks weather conditions for Iron Body crystal spawns
		ServerTickEvents.END_SERVER_TICK.register(CrystalSpawnManager::onServerTick);

		// Bloodforged crystal — chance to spawn when a player kills a mob
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, attacker, killed, damageSource) -> {
			CrystalSpawnManager.onEntityKilled(level, attacker, killed);
		});

		// Auto-save player data periodically (every 5 minutes)
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ticksSinceLastSave++;
			if (ticksSinceLastSave >= AUTO_SAVE_INTERVAL_TICKS) {
				ticksSinceLastSave = 0;
				autoSave(server);
			}
		});

		// Clean up player state and save when a player disconnects
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.getPlayer();
			CradlePlayerData data = CradlePlayerData.get(player.getUUID());
			if (data != null) {
				// Deactivate enforcer so attribute modifiers are cleaned up
				if (data.isEnforcerActive()) {
					CyclingManager.deactivateEnforcer(player, data);
				}
				// Deactivate iron body toggle
				data.setIronBodyActive(false);
				// Stop cycling
				if (data.isActivelyCycling()) {
					CyclingManager.stopCycling(player, data);
				}
			}
			autoSave(server);
		});

		// Register /cycle commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			CycleCommand.register(dispatcher);
		});
	}

	/**
	 * Saves all player data to the world folder.
	 * Called periodically (every 30s), on player disconnect, on key events, and on server stop.
	 */
	public static void autoSave(MinecraftServer server) {
		if (CradlePlayerData.getAll().isEmpty()) {
			return; // Nothing to save
		}
		Path dataFile = server.getWorldPath(LevelResource.ROOT).resolve(DATA_FILE_NAME);
		try {
			CompoundTag root = CradlePlayerData.saveAll();
			NbtIo.writeCompressed(root, dataFile);
		} catch (IOException e) {
			LOGGER.error("Failed to auto-save Cradle player data!", e);
		}
	}
}

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
import com.cradle.mod.network.UseStrikerPayload;
import com.cradle.mod.network.UseRulerPayload;
import com.cradle.mod.network.ToggleCyclingPayload;
import com.cradle.mod.network.ChooseSageHeraldPayload;
import com.cradle.mod.entity.CradleEntities;
import com.cradle.mod.entity.StrikerProjectileEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

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
	private static final long BLOODFORGED_COOLDOWN_MS = 30_000;
	private static final long STRIKER_COOLDOWN_MS = 2_000;
	private static final float STRIKER_MADRA_COST = 15.0f;
	private static final int AUTO_SAVE_INTERVAL_TICKS = 600; // 30 seconds

	private static final Map<UUID, Long> bloodforgedCooldowns = new HashMap<>();
	private static final Map<UUID, Long> strikerCooldowns = new HashMap<>();
	private static int ticksSinceLastSave = 0;

	@Override
	public void onInitialize() {
		LOGGER.info("Cradle Mod initialized!");

		// Register custom items and blocks
		CradleItems.register();
		CradleBlocks.register();

		// Register custom entity types
		CradleEntities.register();

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
		PayloadTypeRegistry.playC2S().register(UseStrikerPayload.TYPE, UseStrikerPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(UseRulerPayload.TYPE, UseRulerPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ToggleCyclingPayload.TYPE, ToggleCyclingPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ChooseSageHeraldPayload.TYPE, ChooseSageHeraldPayload.STREAM_CODEC);

		// Send initial data sync when a player joins, and open path selection if needed
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
			sync(player, data);
			// Grant flight if the player's stage qualifies
			if (data.canFly()) {
				CyclingManager.enableFlight(player, data);
			}
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

			autoSave(player.level().getServer());
			sync(player, data);

			// Path-specific lore
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A76You have chosen the \u00A7e" + path.displayName() + "\u00A76!"
			));

			String pathLore = switch (path) {
				case BLACK_FLAME -> "The destroyer's path. Blackflame madra burns through all defenses.";
				case ENDLESS_SWORD -> "The swordsman's path. Your spirit sharpens into a weapon beyond steel.";
				case STELLAR_SPEAR -> "The piercing path. Light and precision guide your every strike.";
				case CLOUD_HAMMER -> "The juggernaut's path. You carry the weight of storms in your fists.";
				case HOLLOW_KING -> "The purist's path. Your madra is unaspected — versatile and unyielding.";
				default -> "";
			};
			if (!pathLore.isEmpty()) {
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7d\u00A7o" + pathLore
				));
			}
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A77Begin cycling (G) to strengthen your madra channels."
			));
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7a\u2694 Striker technique unlocked! Press X to fire."
			));
		});

		// Handle advancement attempt (player clicked "Advance" button)
		ServerPlayNetworking.registerGlobalReceiver(AttemptAdvancePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			if (BreakthroughManager.attemptBreakthrough(player, data)) {
				autoSave(player.level().getServer());
			} else {
				player.displayClientMessage(Component.literal(
						"\u00A7cYou do not meet the requirements to advance."), true);
			}
			sync(player, data);
		});

		// Handle Sage/Herald choice (player clicked "Become Sage" or "Become Herald")
		ServerPlayNetworking.registerGlobalReceiver(ChooseSageHeraldPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			if (data.getAdvancementStage() != CradlePlayerData.AdvancementStage.ARCHLORD) {
				player.displayClientMessage(Component.literal("\u00A7cYou must be at Archlord to make this choice."), true);
				return;
			}
			if (data.getPlayerLevel() < 350) {
				player.displayClientMessage(Component.literal("\u00A7cYou must reach Level 350 to advance beyond Archlord."), true);
				return;
			}
			if (data.hasSage() || data.hasHerald()) {
				player.displayClientMessage(Component.literal("\u00A7cYou have already made your choice."), true);
				return;
			}

			switch (payload.choice()) {
				case "SAGE" -> {
					data.setHasSage(true);
					data.setAdvancementStage(CradlePlayerData.AdvancementStage.SAGE);
					player.sendSystemMessage(Component.literal(
							"\u00A76[Cradle] \u00A7b\u2728 You have touched the Way and become a Sage! \u2728"));
					player.sendSystemMessage(Component.literal(
							"\u00A76[Cradle] \u00A7b\u00A7oThe Icon appears above you. Reality itself acknowledges your authority."));
				}
				case "HERALD" -> {
					data.setHasHerald(true);
					data.setAdvancementStage(CradlePlayerData.AdvancementStage.HERALD);
					player.sendSystemMessage(Component.literal(
							"\u00A76[Cradle] \u00A7d\u2728 Your spirit merges with your body. You are now a Herald! \u2728"));
					player.sendSystemMessage(Component.literal(
							"\u00A76[Cradle] \u00A7d\u00A7oYour flesh transcends mortality. You are reborn in the image of your spirit."));
				}
				default -> { return; }
			}

			autoSave(player.level().getServer());
			sync(player, data);
		});

		// Handle cycling toggle (G key)
		ServerPlayNetworking.registerGlobalReceiver(ToggleCyclingPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			if (data.isActivelyCycling()) {
				CyclingManager.stopCycling(player, data);
				player.displayClientMessage(Component.literal("\u00A7fYou stop cycling."), true);
			} else {
				if (!CyclingManager.canCycleWhileUsingAbilities(data)
						&& (data.isEnforcerActive() || data.isRulerActive())) {
					player.displayClientMessage(Component.literal(
							"\u00A7cYou can't cycle while a technique is active!"), true);
					sync(player, data);
					return;
				}
				CyclingManager.startCycling(player, data);
				player.displayClientMessage(Component.literal(
						"\u00A7fYou begin cycling. Madra flows through you..."), true);
			}
			sync(player, data);
		});

		// Handle Iron Body (P key). Bloodforged = instant heal; Steelborn/Raindrop = toggle.
		ServerPlayNetworking.registerGlobalReceiver(ToggleIronBodyPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			if (data.getIronBody() == CradlePlayerData.IronBody.NONE) {
				player.displayClientMessage(Component.literal("\u00A7cYou don't have an Iron Body."), true);
				return;
			}

			if (data.getIronBody() == CradlePlayerData.IronBody.BLOODFORGED) {
				long now = System.currentTimeMillis();
				Long lastUse = bloodforgedCooldowns.get(player.getUUID());
				if (lastUse != null && now - lastUse < BLOODFORGED_COOLDOWN_MS) {
					int remainingSec = (int) Math.ceil((BLOODFORGED_COOLDOWN_MS - (now - lastUse)) / 1000.0);
					player.displayClientMessage(Component.literal(
							"\u00A7cBloodforged on cooldown! \u00A7e" + remainingSec + "s \u00A7cremaining."), true);
					return;
				}
				bloodforgedCooldowns.put(player.getUUID(), now);
				player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
						net.minecraft.world.effect.MobEffects.INSTANT_HEALTH, 1, 1, false, false));
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7c\u2764 Bloodforged Iron Body pulses with healing energy!"));
			} else {
				boolean newState = !data.isIronBodyActive();
				data.setIronBodyActive(newState);
				player.sendSystemMessage(Component.literal(newState
						? "\u00A76[Cradle] \u00A7a" + data.getIronBody().displayName() + " Iron Body activated."
						: "\u00A76[Cradle] \u00A77Iron Body deactivated."));
			}
			sync(player, data);
		});

		// Handle Enforcer technique (Z key toggle). Requires Copper+.
		ServerPlayNetworking.registerGlobalReceiver(UseEnforcerPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			if (requiresPath(player, data)) return;
			if (requiresStage(player, data, CradlePlayerData.AdvancementStage.COPPER, "Enforcer")) return;

			if (data.isEnforcerActive()) {
				CyclingManager.deactivateEnforcer(player, data);
				player.sendSystemMessage(Component.literal("\u00A76[Cradle] \u00A77Enforcer technique deactivated."));
			} else {
				if (data.getCurrentMadra() <= 0) {
					player.displayClientMessage(Component.literal("\u00A7cNot enough Madra!"), true);
					return;
				}
				disruptCyclingIfNeeded(player, data);
				CyclingManager.activateEnforcer(player, data);

				String name = getEnforcerName(data.getChosenPath());
				player.sendSystemMessage(Component.literal("\u00A76[Cradle] \u00A7a" + name + " activated!"));
			}
			sync(player, data);
		});

		// Handle Striker technique (X key). Available at Foundation. Costs Madra + has cooldown.
		ServerPlayNetworking.registerGlobalReceiver(UseStrikerPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			if (requiresPath(player, data)) return;

			// Cooldown check (scaled by advancement stage)
			long now = System.currentTimeMillis();
			Long lastUse = strikerCooldowns.get(player.getUUID());
			long effectiveCooldown = (long) (STRIKER_COOLDOWN_MS * data.getCooldownMultiplier());
			if (lastUse != null && now - lastUse < effectiveCooldown) {
				long remainingMs = effectiveCooldown - (now - lastUse);
				double remainingSec = Math.ceil(remainingMs / 100.0) / 10.0;
				player.displayClientMessage(Component.literal(
						"\u00A7cStriker on cooldown! \u00A7e" + String.format("%.1f", remainingSec) + "s \u00A7cremaining."
				), true);
				return;
			}

			// Madra cost (graduated discount by stage)
			float cost = STRIKER_MADRA_COST * data.getMadraCostMultiplier();
			if (data.getCurrentMadra() < cost) {
				player.displayClientMessage(Component.literal(
						"\u00A7cNot enough Madra! Need \u00A7e" + String.format("%.0f", cost) + "\u00A7c."
				), true);
				return;
			}

			disruptCyclingIfNeeded(player, data);

			// Deduct Madra, set cooldown, fire projectile
			data.setCurrentMadra(data.getCurrentMadra() - cost);
			strikerCooldowns.put(player.getUUID(), now);

			ServerLevel serverLevel = (ServerLevel) player.level();
			Vec3 look = player.getLookAngle();
			StrikerProjectileEntity projectile = new StrikerProjectileEntity(
					serverLevel, player, look, data.getChosenPath(), data.getAbilityPowerMultiplier());
			projectile.setPos(
					player.getX() + look.x * 0.5,
					player.getEyeY() - 0.1,
					player.getZ() + look.z * 0.5);
			Projectile.spawnProjectileUsingShoot(
					projectile, serverLevel, ItemStack.EMPTY,
					look.x, look.y, look.z, 1.5f, 0.0f);

			sync(player, data);
		});

		// Handle Ruler technique (C key toggle). Requires Copper+.
		ServerPlayNetworking.registerGlobalReceiver(UseRulerPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			if (requiresPath(player, data)) return;
			if (requiresStage(player, data, CradlePlayerData.AdvancementStage.COPPER, "Ruler")) return;

			if (data.isRulerActive()) {
				data.setRulerActive(false);
				player.sendSystemMessage(Component.literal("\u00A76[Cradle] \u00A77Ruler technique deactivated."));
			} else {
				if (data.getCurrentMadra() <= 0) {
					player.displayClientMessage(Component.literal("\u00A7cNot enough Madra!"), true);
					return;
				}
				disruptCyclingIfNeeded(player, data);
				data.setRulerActive(true);

				String name = getRulerName(data.getChosenPath());
				player.sendSystemMessage(Component.literal("\u00A76[Cradle] \u00A7a" + name + " activated!"));
			}
			sync(player, data);
		});

		// Sword-stabbing cycling: right-click soft block with sword (Endless Sword / Stellar Spear)
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
			if (!player.getItemInHand(hand).is(ItemTags.SWORDS)) return InteractionResult.PASS;

			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
			if (!data.isSwordPath()) return InteractionResult.PASS;
			if (!CyclingManager.isSoftBlock(world.getBlockState(hitResult.getBlockPos()))) return InteractionResult.PASS;

			if (data.isSwordCycling()) {
				data.setSwordCycling(false);
				player.displayClientMessage(Component.literal("\u00A77You pull your blade free."), true);
			} else {
				data.setSwordCycling(true);
				if (!data.isActivelyCycling()) {
					CyclingManager.startCycling(sp, data);
					sp.displayClientMessage(Component.literal(
							"\u00A7fYou drive your blade into the earth and begin cycling."), true);
				} else {
					player.displayClientMessage(Component.literal(
							"\u00A7bYour blade channels the earth's aura. Cycling intensifies."), true);
				}
			}
			sync(sp, data);
			return InteractionResult.SUCCESS;
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

		// Revelation trial manager — checks trial progress (spirit kills, distance leash)
		ServerTickEvents.END_SERVER_TICK.register(RevelationTrialManager::onServerTick);

		// Mob kill: grant combat XP + chance to spawn Bloodforged crystal
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, attacker, killed, damageSource) -> {
			if (attacker instanceof ServerPlayer player) {
				CyclingManager.grantCombatXp(player, killed);
				sync(player, CradlePlayerData.getOrCreate(player.getUUID()));
			}
			CrystalSpawnManager.onEntityKilled(level, attacker, killed);
		});

		// Player death — fail active revelation trial if the player dies
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			RevelationTrialManager.onEntityDeath(entity);
		});

		// Auto-save player data periodically (every 5 minutes)
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ticksSinceLastSave++;
			if (ticksSinceLastSave >= AUTO_SAVE_INTERVAL_TICKS) {
				ticksSinceLastSave = 0;
				autoSave(server);
			}
		});

		// Clean up player state and save on disconnect
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.getPlayer();
			CradlePlayerData data = CradlePlayerData.get(player.getUUID());
			if (data != null) {
				if (data.isEnforcerActive()) CyclingManager.deactivateEnforcer(player, data);
				data.setRulerActive(false);
				data.setIronBodyActive(false);
				if (data.isActivelyCycling()) CyclingManager.stopCycling(player, data);
				if (data.isUnderlordFlying()) CyclingManager.disableFlight(player, data);
			}
			RevelationTrialManager.cancelTrial(player.getUUID());
			autoSave(server);
		});

		// Register /cycle commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			CycleCommand.register(dispatcher);
		});
	}

	// ── Technique name lookups ────────────────────────────────────────

	private static String getEnforcerName(CradlePlayerData.Path path) {
		return switch (path) {
			case BLACK_FLAME -> "Burning Body";
			case ENDLESS_SWORD -> "Flowing Edge";
			case STELLAR_SPEAR -> "Stellar Alignment";
			case CLOUD_HAMMER -> "Thunderous Weight";
			case HOLLOW_KING -> "Hollow Circulation";
			default -> "Enforcer Technique";
		};
	}

	private static String getRulerName(CradlePlayerData.Path path) {
		return switch (path) {
			case BLACK_FLAME -> "Domain of Ash";
			case ENDLESS_SWORD -> "Field of Blades";
			case STELLAR_SPEAR -> "Spear Domain";
			case CLOUD_HAMMER -> "Gravity Field";
			case HOLLOW_KING -> "Hollow Domain";
			default -> "Ruler Technique";
		};
	}

	// ── Shared validation helpers ────────────────────────────────────

	/** Returns true (and sends action-bar error) if the player hasn't chosen a path. */
	private static boolean requiresPath(ServerPlayer player, CradlePlayerData data) {
		if (!data.hasChosenPath() || data.getChosenPath() == CradlePlayerData.Path.UNSET) {
			player.displayClientMessage(Component.literal("\u00A7cYou haven't chosen a path yet."), true);
			return true;
		}
		return false;
	}

	/** Returns true (and sends action-bar error) if the player is below the required stage. */
	private static boolean requiresStage(ServerPlayer player, CradlePlayerData data,
										  CradlePlayerData.AdvancementStage minStage, String techniqueName) {
		if (data.getAdvancementStage().ordinal() < minStage.ordinal()) {
			player.displayClientMessage(Component.literal(
					"\u00A7c" + techniqueName + " techniques require " + minStage.displayName() + " stage or higher."
			), true);
			return true;
		}
		return false;
	}

	/**
	 * If cycling is active and the player can't multi-task at their stage,
	 * stops cycling and warns them. Returns true if cycling was disrupted.
	 */
	private static boolean disruptCyclingIfNeeded(ServerPlayer player, CradlePlayerData data) {
		if (data.isActivelyCycling() && !CyclingManager.canCycleWhileUsingAbilities(data)) {
			CyclingManager.stopCycling(player, data);
			player.displayClientMessage(Component.literal(
					"\u00A7eYour cycling is disrupted by the technique!"
			), true);
			return true;
		}
		return false;
	}

	/** Sends a sync packet to the client. */
	private static void sync(ServerPlayer player, CradlePlayerData data) {
		ServerPlayNetworking.send(player, CyclingManager.createSyncPayload(player, data));
	}

	// ── Persistence ──────────────────────────────────────────────────

	public static void autoSave(MinecraftServer server) {
		if (CradlePlayerData.getAll().isEmpty()) return;
		Path dataFile = server.getWorldPath(LevelResource.ROOT).resolve(DATA_FILE_NAME);
		try {
			NbtIo.writeCompressed(CradlePlayerData.saveAll(), dataFile);
		} catch (IOException e) {
			LOGGER.error("Failed to auto-save Cradle player data!", e);
		}
	}
}

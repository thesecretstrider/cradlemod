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
import com.cradle.mod.network.UseSagePayload;
import com.cradle.mod.network.UseHeraldPayload;
import com.cradle.mod.network.OpenIconSelectionPayload;
import com.cradle.mod.network.ChooseIconPayload;
import com.cradle.mod.network.DuelInviteReceivedPayload;
import com.cradle.mod.network.DuelEndPayload;
import com.cradle.mod.entity.CradleEntities;
import com.cradle.mod.entity.StrikerProjectileEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;

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
	private static final long SAGE_STOP_COOLDOWN_MS = 12_000;   // 12 seconds base
	private static final long SAGE_KILL_COOLDOWN_MS = 20_000;   // 20 seconds base
	private static final float SAGE_STOP_WILLPOWER_COST = 25.0f;
	private static final float SAGE_KILL_WILLPOWER_COST = 40.0f;
	private static final int AUTO_SAVE_INTERVAL_TICKS = 600; // 30 seconds

	private static final Map<UUID, Long> bloodforgedCooldowns = new HashMap<>();
	private static final Map<UUID, Long> strikerCooldowns = new HashMap<>();
	private static final Map<String, Long> sageAbilityCooldowns = new HashMap<>();
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
		PayloadTypeRegistry.playS2C().register(OpenIconSelectionPayload.TYPE, OpenIconSelectionPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(DuelInviteReceivedPayload.TYPE, DuelInviteReceivedPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(DuelEndPayload.TYPE, DuelEndPayload.STREAM_CODEC);

		// Register networking packets (client -> server)
		PayloadTypeRegistry.playC2S().register(ChoosePathPayload.TYPE, ChoosePathPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(AttemptAdvancePayload.TYPE, AttemptAdvancePayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ToggleIronBodyPayload.TYPE, ToggleIronBodyPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(UseEnforcerPayload.TYPE, UseEnforcerPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(UseStrikerPayload.TYPE, UseStrikerPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(UseRulerPayload.TYPE, UseRulerPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ToggleCyclingPayload.TYPE, ToggleCyclingPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ChooseSageHeraldPayload.TYPE, ChooseSageHeraldPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(UseSagePayload.TYPE, UseSagePayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(UseHeraldPayload.TYPE, UseHeraldPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ChooseIconPayload.TYPE, ChooseIconPayload.STREAM_CODEC);

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
					// Don't instantly become Sage — must choose an Icon first!
					// Send the Icon selection screen to the client.
					ServerPlayNetworking.send(player,
							new OpenIconSelectionPayload(data.getChosenPath().name(), false));
					return; // Don't save/sync yet — wait for ChooseIconPayload
				}
				case "HERALD" -> {
					// Don't instantly become Herald — start the Remnant fight!
					// The player must defeat their own Remnant (Iron Golem boss) to merge
					// body and spirit and become a Herald.
					RevelationTrialManager.startHeraldTrial(player, data);
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

		// Handle Sage Authority (V key). Requires hasSage. Costs Willpower + has cooldown.
		ServerPlayNetworking.registerGlobalReceiver(UseSagePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			// Must be a Sage (or Monarch)
			if (!data.hasSage()) {
				player.displayClientMessage(Component.literal(
						"\u00A7cYou have not touched an Icon. Only Sages may wield Authority."), true);
				return;
			}

			String ability = payload.ability();
			float willpowerCost;
			long baseCooldown;

			switch (ability) {
				case "STOP" -> {
					willpowerCost = SAGE_STOP_WILLPOWER_COST;
					baseCooldown = SAGE_STOP_COOLDOWN_MS;
				}
				case "KILL" -> {
					willpowerCost = SAGE_KILL_WILLPOWER_COST;
					baseCooldown = SAGE_KILL_COOLDOWN_MS;
				}
				default -> { return; }
			}

			// Monarch gets 30% willpower discount
			if (data.hasSage() && data.hasHerald()) {
				willpowerCost *= 0.7f;
			}

			// Cooldown check (keyed per ability type per player)
			long now = System.currentTimeMillis();
			String cooldownKey = player.getUUID() + ":" + ability;
			Long lastUse = sageAbilityCooldowns.get(cooldownKey);
			long effectiveCooldown = (long) (baseCooldown * data.getCooldownMultiplier());
			if (lastUse != null && now - lastUse < effectiveCooldown) {
				long remainingMs = effectiveCooldown - (now - lastUse);
				double remainingSec = Math.ceil(remainingMs / 100.0) / 10.0;
				player.displayClientMessage(Component.literal(
						"\u00A7cAuthority on cooldown! \u00A7e" + String.format("%.1f", remainingSec) + "s"), true);
				return;
			}

			// Willpower check
			if (data.getCurrentWillpower() < willpowerCost) {
				player.displayClientMessage(Component.literal(
						"\u00A7cNot enough Willpower! Need \u00A7b" + String.format("%.0f", willpowerCost)), true);
				return;
			}

			disruptCyclingIfNeeded(player, data);

			// Deduct willpower, set cooldown
			data.setCurrentWillpower(data.getCurrentWillpower() - willpowerCost);
			sageAbilityCooldowns.put(cooldownKey, now);

			ServerLevel serverLevel = (ServerLevel) player.level();

			if ("STOP".equals(ability)) {
				// ── Authority: STOP ──
				// Freeze all hostile entities within 10 blocks for 3-5 seconds
				float duration = 3.0f + (data.getAbilityPowerMultiplier() - 1.0f) * 2.0f;
				int durationTicks = (int) (duration * 20);
				AABB area = player.getBoundingBox().inflate(10.0);
				java.util.List<LivingEntity> hostiles = serverLevel.getEntitiesOfClass(
						LivingEntity.class, area,
						e -> e != player && e.isAlive() && isHostile(e, player));

				int frozenCount = 0;
				for (LivingEntity e : hostiles) {
					// Sage vs Sage counter: if target is a Sage player, they can resist
					if (e instanceof ServerPlayer targetPlayer) {
						CradlePlayerData targetData = CradlePlayerData.getOrCreate(targetPlayer.getUUID());
						if (targetData.hasSage() && targetData.getCurrentWillpower() >= 15.0f) {
							targetData.setCurrentWillpower(targetData.getCurrentWillpower() - 15.0f);
							targetPlayer.sendSystemMessage(Component.literal(
									"\u00A76[Cradle] \u00A7bYou resist " + player.getName().getString() + "'s Authority!"));
							player.sendSystemMessage(Component.literal(
									"\u00A76[Cradle] \u00A7c" + targetPlayer.getName().getString() + " countered your Stop!"));
							sync(targetPlayer, targetData);
							continue;
						}
					}
					// Apply freeze: Slowness 127 (practically frozen) + Mining Fatigue
					e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
							net.minecraft.world.effect.MobEffects.SLOWNESS, durationTicks, 127, false, false));
					e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
							net.minecraft.world.effect.MobEffects.MINING_FATIGUE, durationTicks, 5, false, false));
					frozenCount++;
				}

				// Particles: ring of blue soul flames around player
				for (int i = 0; i < 36; i++) {
					double angle = Math.toRadians(i * 10);
					double px = player.getX() + Math.cos(angle) * 10.0;
					double pz = player.getZ() + Math.sin(angle) * 10.0;
					serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
							px, player.getY() + 0.5, pz, 2, 0.1, 0.3, 0.1, 0.01);
				}

				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7b\u2728 \"" + getSageWord(data) + "\" \u2014 "
								+ frozenCount + " entities frozen by your Authority."));

			} else {
				// ── Authority: KILL ──
				// Targeted: entity the player is looking at within 20 blocks
				Vec3 eyePos = player.getEyePosition();
				Vec3 lookVec = player.getLookAngle();
				Vec3 endPos = eyePos.add(lookVec.scale(20.0));
				AABB searchBox = player.getBoundingBox().inflate(20.0);

				EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
						player, eyePos, endPos, searchBox,
						e -> e instanceof LivingEntity && e.isAlive() && e != player, 20.0 * 20.0);

				if (hitResult == null || !(hitResult.getEntity() instanceof LivingEntity target)) {
					player.displayClientMessage(Component.literal("\u00A7cNo target in sight."), true);
					// Refund willpower since ability didn't fire
					data.setCurrentWillpower(data.getCurrentWillpower() + willpowerCost);
					sageAbilityCooldowns.remove(cooldownKey);
					sync(player, data);
					return;
				}

				// Sage vs Sage counter
				if (target instanceof ServerPlayer targetPlayer) {
					CradlePlayerData targetData = CradlePlayerData.getOrCreate(targetPlayer.getUUID());
					if (targetData.hasSage() && targetData.getCurrentWillpower() >= 25.0f) {
						targetData.setCurrentWillpower(targetData.getCurrentWillpower() - 25.0f);
						targetPlayer.sendSystemMessage(Component.literal(
								"\u00A76[Cradle] \u00A7bYou deflect " + player.getName().getString() + "'s killing intent!"));
						player.sendSystemMessage(Component.literal(
								"\u00A76[Cradle] \u00A7c" + targetPlayer.getName().getString() + " blocked your Authority!"));
						sync(targetPlayer, targetData);
						sync(player, data);
						return;
					}
				}

				// Deal massive true damage (bypasses armor via MAGIC damage source)
				float damage = 20.0f * data.getAbilityPowerMultiplier();
				target.hurtServer(serverLevel, player.damageSources().magic(), damage);

				// Particles: line of soul flames from player to target
				Vec3 dir = target.position().subtract(player.position()).normalize();
				double dist = player.distanceTo(target);
				for (double d = 0; d < dist; d += 0.5) {
					serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
							player.getX() + dir.x * d,
							player.getEyeY() - 0.3 + dir.y * d,
							player.getZ() + dir.z * d,
							1, 0.05, 0.05, 0.05, 0.0);
				}

				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7b\u2620 Your Authority strikes " + target.getName().getString()
								+ " for \u00A7c" + String.format("%.0f", damage) + " \u00A7bdamage."));
			}

			sync(player, data);
		});

		// Handle Herald Spirit Shift (B key). Requires hasHerald. Costs Willpower/tick.
		ServerPlayNetworking.registerGlobalReceiver(UseHeraldPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			if (!data.hasHerald()) {
				player.displayClientMessage(Component.literal(
						"\u00A7cYou have not merged with your Remnant. Only Heralds may shift forms."), true);
				return;
			}

			if (data.isSpiritShiftActive()) {
				// Toggle OFF
				CyclingManager.deactivateSpiritShift(player, data);
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7dYour spirit merges back with your body."));
			} else {
				// Toggle ON
				if (data.getCurrentWillpower() <= 0) {
					player.displayClientMessage(Component.literal(
							"\u00A7cNot enough Willpower to shift forms!"), true);
					return;
				}
				disruptCyclingIfNeeded(player, data);
				data.setSpiritShiftActive(true);
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7d\u2728 Your spirit separates from your flesh. You walk between worlds."));
			}
			sync(player, data);
		});

		// Handle Icon selection (from IconSelectionScreen). Validates and performs Sage or Monarch advancement.
		ServerPlayNetworking.registerGlobalReceiver(ChooseIconPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

			// Must be at Archlord (choosing Sage) or Herald (choosing Monarch via Icon)
			CradlePlayerData.AdvancementStage stage = data.getAdvancementStage();
			boolean isArchlord = stage == CradlePlayerData.AdvancementStage.ARCHLORD;
			boolean isHerald = stage == CradlePlayerData.AdvancementStage.HERALD;
			if (!isArchlord && !isHerald) {
				player.displayClientMessage(Component.literal("\u00A7cYou cannot choose an Icon at this stage."), true);
				return;
			}

			// Must not already have an Icon
			if (data.getChosenIcon() != CradlePlayerData.Icon.NONE) {
				player.displayClientMessage(Component.literal("\u00A7cYou have already manifested an Icon."), true);
				return;
			}

			// Validate the Icon name
			CradlePlayerData.Icon icon;
			try {
				icon = CradlePlayerData.Icon.valueOf(payload.iconName());
			} catch (IllegalArgumentException e) {
				return; // Invalid icon name — ignore
			}
			if (icon == CradlePlayerData.Icon.NONE) {
				return;
			}

			// Validate the Icon is available for this path
			java.util.List<CradlePlayerData.Icon> available = CradlePlayerData.getAvailableIcons(data.getChosenPath());
			if (!available.contains(icon)) {
				player.displayClientMessage(Component.literal("\u00A7cThat Icon is not available to your path."), true);
				return;
			}

			// Set the chosen icon
			data.setChosenIcon(icon);

			// Spawn the Icon particle formation in the sky — visible to all nearby players
			IconParticleDisplay.spawnIconDisplay(player, icon);

			if (isHerald) {
				// Herald choosing Icon → becomes Monarch
				data.setHasSage(true);
				data.setAdvancementStage(CradlePlayerData.AdvancementStage.MONARCH);
				data.setCurrentWillpower(data.getMaxWillpower());
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7b\u2728 The " + icon.displayName() + " Icon burns in your soul. Reality acknowledges you. \u2728"));
				BreakthroughManager.triggerMonarchWorldEvent(player, data);
			} else {
				// Archlord choosing Icon → becomes Sage
				data.setHasSage(true);
				data.setAdvancementStage(CradlePlayerData.AdvancementStage.SAGE);
				data.setCurrentWillpower(data.getMaxWillpower());
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7b\u2728 The " + icon.displayName() + " Icon manifests above you. The Way itself recognizes your soul. \u2728"));
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7b\u00A7oYou are reborn as a Sage. Authority flows through you."));
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7bPress V to command Authority. Tap for Stop, hold for Kill."));
			}

			autoSave(player.level().getServer());
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
			DuelManager.clearAll();
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

		// Duel manager — handles invite expiry, countdown, fight state
		ServerTickEvents.END_SERVER_TICK.register(DuelManager::onServerTick);

		// Mob kill: grant combat XP + chance to spawn Bloodforged crystal
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, attacker, killed, damageSource) -> {
			if (attacker instanceof ServerPlayer player) {
				CyclingManager.grantCombatXp(player, killed);
				sync(player, CradlePlayerData.getOrCreate(player.getUUID()));
			}
			CrystalSpawnManager.onEntityKilled(level, attacker, killed);
		});

		// Duel system: prevent death in friendly duels (must cancel BEFORE death happens)
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
			if (entity instanceof ServerPlayer player) {
				return DuelManager.shouldAllowDeath(player);
			}
			return true; // allow death for non-players
		});

		// Player death — fail active revelation trial if the player dies
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			RevelationTrialManager.onEntityDeath(entity);
			// Handle competitive duel deaths + deactivate Spirit Shift on player death
			if (entity instanceof ServerPlayer deadPlayer) {
				DuelManager.onPlayerDeath(deadPlayer, deadPlayer.level().getServer());
				CradlePlayerData deadData = CradlePlayerData.get(deadPlayer.getUUID());
				if (deadData != null && deadData.isSpiritShiftActive()) {
					CyclingManager.deactivateSpiritShift(deadPlayer, deadData);
				}
			}
		});

		// Re-initialize player state after death+respawn.
		// Minecraft creates a NEW ServerPlayer object on respawn — the old one is discarded.
		// JOIN does NOT re-fire on respawn, so we must explicitly clean up transient state
		// and re-grant flight/sync on the new entity.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			CradlePlayerData data = CradlePlayerData.get(newPlayer.getUUID());
			if (data == null) return;

			// Reset all transient ability states — player starts fresh after death
			// (The old entity's attribute modifiers are gone; just clear the data flags)
			data.setEnforcerActive(false);
			data.setRulerActive(false);
			data.setIronBodyActive(false);
			data.setActivelyCycling(false);
			data.setSwordCycling(false);
			data.setUnderlordFlying(false);
			data.setSpiritShiftActive(false);

			// Re-grant flight capability if stage qualifies (sets mayfly on the NEW entity)
			if (data.canFly()) {
				CyclingManager.enableFlight(newPlayer, data);
			}

			// Sync all data to the new player entity so the client knows the current state
			sync(newPlayer, data);
		});

		// Cloud Hammer: wind cushions falls — Slow Falling applied when falling fast with madra
		// (handled in CyclingManager.onServerTick via checkCloudHammerFallCushion)

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
				if (data.isSpiritShiftActive()) CyclingManager.deactivateSpiritShift(player, data);
			}
			RevelationTrialManager.cancelTrial(player.getUUID());
			DuelManager.onPlayerDisconnect(player.getUUID(), server);
			autoSave(server);
		});

		// Register /cycle commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			CycleCommand.register(dispatcher);
			DuelCommand.register(dispatcher);
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

	/** Returns true if the entity is hostile toward the given player. */
	private static boolean isHostile(LivingEntity entity, Player player) {
		if (entity instanceof net.minecraft.world.entity.monster.Monster) return true;
		if (entity instanceof Mob mob && mob.getTarget() == player) return true;
		// Hostile wolves (from dreadbeast mixin) target players
		if (entity instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf && !wolf.isTame()) return true;
		return false;
	}

	/** Returns the Sage's Authority word based on their path — flavor only. */
	private static String getSageWord(CradlePlayerData data) {
		return switch (data.getChosenPath()) {
			case HOLLOW_KING -> "Stop";
			case BLACK_FLAME -> "Burn";
			case ENDLESS_SWORD -> "Cut";
			case STELLAR_SPEAR -> "Pierce";
			case CLOUD_HAMMER -> "Fall";
			default -> "Stop";
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

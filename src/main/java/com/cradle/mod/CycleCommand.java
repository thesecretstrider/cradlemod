package com.cradle.mod;

import com.cradle.mod.ability.AbilityDefinition;
import com.cradle.mod.ability.AbilityRegistry;
import com.cradle.mod.ability.PlayerLoadout;
import com.cradle.mod.network.AbilityLoadoutSyncPayload;
import com.cradle.mod.network.OpenInfoScreenPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/**
 * Registers the /cycle command with subcommands:
 *   /cycle start       - begin active cycling (must stay still, glowing outline)
 *   /cycle stop        - stop active cycling
 *   /cycle info        - open the Sacred Artist Status GUI screen
 *   /cycle setlevel    - debug: set player level
 *   /cycle setstage    - debug: set advancement stage
 *   /cycle setpath     - debug: set chosen path
 *   /cycle setmadra    - debug: set current madra
 */
public final class CycleCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("cycle")

				// /cycle start
				.then(Commands.literal("start").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

					if (data.isActivelyCycling()) {
						ctx.getSource().sendFailure(Component.literal("You are already cycling!"));
						return 0;
					}

					CyclingManager.startCycling(player, data);
					ctx.getSource().sendSuccess(
							() -> Component.literal("§6[Cradle] §fYou begin cycling. Madra flows through you..."),
							false
					);
					return 1;
				}))

				// /cycle stop
				.then(Commands.literal("stop").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());

					if (!data.isActivelyCycling()) {
						ctx.getSource().sendFailure(Component.literal("You are not cycling."));
						return 0;
					}

					CyclingManager.stopCycling(player, data);
					ctx.getSource().sendSuccess(
							() -> Component.literal("§6[Cradle] §fYou stop cycling."),
							false
					);
					return 1;
				}))

				// /cycle info — opens the GUI screen on the client
				.then(Commands.literal("info").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					ServerPlayNetworking.send(player, new OpenInfoScreenPayload());
					return 1;
				}))

				// /cycle setlevel <number>
				.then(Commands.literal("setlevel")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.argument("level", IntegerArgumentType.integer(0))
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
									int level = IntegerArgumentType.getInteger(ctx, "level");

									data.setPlayerLevel(level);
									data.setCyclingXp(0);
									ctx.getSource().sendSuccess(
											() -> Component.literal("§6[Cradle] §fLevel set to §e" + level),
											true
									);
									return 1;
								})))

				// /cycle setstage <stage>
				.then(Commands.literal("setstage")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.argument("stage", StringArgumentType.word())
								.suggests((ctx, builder) -> {
									for (CradlePlayerData.AdvancementStage stage : CradlePlayerData.AdvancementStage.values()) {
										builder.suggest(stage.name());
									}
									return builder.buildFuture();
								})
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
									String stageName = StringArgumentType.getString(ctx, "stage");

									try {
										CradlePlayerData.AdvancementStage stage =
												CradlePlayerData.AdvancementStage.valueOf(stageName.toUpperCase());
										data.setAdvancementStage(stage);

										// Set Sage/Herald flags to match the stage so branching logic works correctly
										switch (stage) {
											case SAGE -> {
												data.setHasSage(true);
												if (data.getCurrentWillpower() <= 0)
													data.setCurrentWillpower(data.getMaxWillpower());
											}
											case HERALD -> data.setHasHerald(true);
											case MONARCH -> {
												data.setHasSage(true);
												data.setHasHerald(true);
												if (data.getCurrentWillpower() <= 0)
													data.setCurrentWillpower(data.getMaxWillpower());
											}
											default -> {
												// Resetting to a stage below Sage/Herald clears the flags
												if (stage.ordinal() < CradlePlayerData.AdvancementStage.SAGE.ordinal()) {
													data.setHasSage(false);
													data.setHasHerald(false);
												}
											}
										}

										ctx.getSource().sendSuccess(
												() -> Component.literal("§6[Cradle] §fStage set to §e" + stage.displayName()),
												true
										);
										return 1;
									} catch (IllegalArgumentException e) {
										ctx.getSource().sendFailure(Component.literal("Unknown stage: " + stageName));
										return 0;
									}
								})))

				// /cycle setpath <path>
				.then(Commands.literal("setpath")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.argument("path", StringArgumentType.word())
								.suggests((ctx, builder) -> {
									for (CradlePlayerData.Path path : CradlePlayerData.Path.values()) {
										if (path != CradlePlayerData.Path.UNSET) {
											builder.suggest(path.name());
										}
									}
									return builder.buildFuture();
								})
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
									String pathName = StringArgumentType.getString(ctx, "path");

									try {
										CradlePlayerData.Path path =
												CradlePlayerData.Path.valueOf(pathName.toUpperCase());
										data.setChosenPath(path);
										ctx.getSource().sendSuccess(
												() -> Component.literal("§6[Cradle] §fPath set to §e" + path.displayName()),
												true
										);
										return 1;
									} catch (IllegalArgumentException e) {
										ctx.getSource().sendFailure(Component.literal("Unknown path: " + pathName));
										return 0;
									}
								})))

				// /cycle setmadra <amount>
				.then(Commands.literal("setmadra")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.argument("amount", IntegerArgumentType.integer(0))
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
									int amount = IntegerArgumentType.getInteger(ctx, "amount");

									data.setCurrentMadra(amount);
									ctx.getSource().sendSuccess(
											() -> Component.literal("§6[Cradle] §fMadra set to §e" + String.format("%.1f", data.getCurrentMadra())),
											true
									);
									return 1;
								})))

				// /cycle setironbody <type>
				.then(Commands.literal("setironbody")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.argument("type", StringArgumentType.word())
								.suggests((ctx, builder) -> {
									for (CradlePlayerData.IronBody body : CradlePlayerData.IronBody.values()) {
										builder.suggest(body.name());
									}
									return builder.buildFuture();
								})
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
									String typeName = StringArgumentType.getString(ctx, "type");

									try {
										CradlePlayerData.IronBody body =
												CradlePlayerData.IronBody.valueOf(typeName.toUpperCase());
										data.setIronBody(body);
										if (body == CradlePlayerData.IronBody.NONE) {
											data.setIronBodyActive(false);
										}
										ctx.getSource().sendSuccess(
												() -> Component.literal("§6[Cradle] §fIron Body set to §e" + body.displayName()),
												true
										);
										return 1;
									} catch (IllegalArgumentException e) {
										ctx.getSource().sendFailure(Component.literal("Unknown Iron Body type: " + typeName));
										return 0;
									}
								})))

				// /cycle setwillpower <amount>
				.then(Commands.literal("setwillpower")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.argument("amount", IntegerArgumentType.integer(0))
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
									int amount = IntegerArgumentType.getInteger(ctx, "amount");

									data.setCurrentWillpower(amount);
									ctx.getSource().sendSuccess(
											() -> Component.literal("§6[Cradle] §fWillpower set to §e" + String.format("%.1f", data.getCurrentWillpower())),
											true
									);
									return 1;
								})))

				// /cycle setskillpoints <amount> — set upgrade points
				.then(Commands.literal("setskillpoints")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.argument("amount", IntegerArgumentType.integer(0))
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
									int amount = IntegerArgumentType.getInteger(ctx, "amount");
									data.getLoadout().setUpgradePoints(amount);
									syncLoadout(player, data);
									ctx.getSource().sendSuccess(
											() -> Component.literal("§6[Cradle] §fUpgrade points set to §e" + amount),
											true
									);
									return 1;
								})))

				// /cycle setability <slot> <abilityId> [level] — force equip an ability
				.then(Commands.literal("setability")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.argument("slot", IntegerArgumentType.integer(0, 5))
								.then(Commands.argument("abilityId", StringArgumentType.word())
										// Without level — defaults to 1
										.executes(ctx -> {
											ServerPlayer player = ctx.getSource().getPlayerOrException();
											CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
											int slot = IntegerArgumentType.getInteger(ctx, "slot");
											String abilityId = StringArgumentType.getString(ctx, "abilityId");

											AbilityDefinition def = AbilityRegistry.get(abilityId);
											if (def == null) {
												ctx.getSource().sendFailure(Component.literal("§cUnknown ability: " + abilityId));
												return 0;
											}

											data.getLoadout().equipAbility(slot, abilityId);
											syncLoadout(player, data);
											ctx.getSource().sendSuccess(
													() -> Component.literal("§6[Cradle] §fSet slot §e" + slot + "§f to §a" + def.getDisplayName() + "§f (level 1)"),
													true
											);
											return 1;
										})
										// With level
										.then(Commands.argument("level", IntegerArgumentType.integer(1, 20))
												.executes(ctx -> {
													ServerPlayer player = ctx.getSource().getPlayerOrException();
													CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
													int slot = IntegerArgumentType.getInteger(ctx, "slot");
													String abilityId = StringArgumentType.getString(ctx, "abilityId");
													int level = IntegerArgumentType.getInteger(ctx, "level");

													AbilityDefinition def = AbilityRegistry.get(abilityId);
													if (def == null) {
														ctx.getSource().sendFailure(Component.literal("§cUnknown ability: " + abilityId));
														return 0;
													}

													data.getLoadout().equipAbility(slot, abilityId, level);
													syncLoadout(player, data);
													ctx.getSource().sendSuccess(
															() -> Component.literal("§6[Cradle] §fSet slot §e" + slot + "§f to §a" + def.getDisplayName() + "§f (level " + level + ")"),
															true
													);
													return 1;
												})))))

				// /cycle resetloadout — clear all slots + refund SP
				.then(Commands.literal("resetloadout")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							CradlePlayerData data = CradlePlayerData.getOrCreate(player.getUUID());
							PlayerLoadout loadout = data.getLoadout();

							// Count current upgrade levels to refund
							int totalLevels = 0;
							for (int i = 0; i < PlayerLoadout.MAX_SLOTS; i++) {
								if (loadout.hasAbility(i)) {
									totalLevels += loadout.getUpgradeLevel(i) - 1; // Level 1 is free
									loadout.clearSlot(i);
								}
							}
							loadout.addUpgradePoints(totalLevels);
							syncLoadout(player, data);
							final int refunded = totalLevels;
							final int totalPoints = loadout.getUpgradePoints();
							ctx.getSource().sendSuccess(
									() -> Component.literal("§6[Cradle] §fLoadout reset. §e" + refunded + "§f upgrade points refunded. Total: §e" + totalPoints),
									true
							);
							return 1;
						}))
		);
	}

	/**
	 * Sync loadout data to client (utility for debug commands).
	 */
	private static void syncLoadout(ServerPlayer player, CradlePlayerData data) {
		PlayerLoadout loadout = data.getLoadout();
		boolean[] slotActive = new boolean[PlayerLoadout.MAX_SLOTS];
		for (int i = 0; i < PlayerLoadout.MAX_SLOTS; i++) {
			slotActive[i] = loadout.isSlotActive(i);
		}
		ServerPlayNetworking.send(player, new AbilityLoadoutSyncPayload(
				loadout.toJsonString(),
				AbilityLoadoutSyncPayload.buildActiveFlags(slotActive)
		));
	}
}

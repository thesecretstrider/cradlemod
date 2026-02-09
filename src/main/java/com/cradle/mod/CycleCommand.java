package com.cradle.mod;

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
 *   /cycle start       - begin active cycling (sit down, must stay still)
 *   /cycle stop        - stop active cycling (stand up)
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
							() -> Component.literal("§6[Cradle] §fYou sit down and begin cycling. Madra flows through you..."),
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
							() -> Component.literal("§6[Cradle] §fYou stand up and stop cycling."),
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
		);
	}
}

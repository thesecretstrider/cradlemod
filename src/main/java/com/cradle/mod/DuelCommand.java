package com.cradle.mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers the /duel command with subcommands:
 *   /duel invite <player> [friendly|competitive] - challenge another player
 *   /duel confirm     - confirm terrain modification for pending invite
 *   /duel cancel      - cancel pending confirmation
 *   /duel accept      - accept a pending duel invite
 *   /duel decline     - decline a pending duel invite
 *   /duel forfeit     - forfeit active duel (available after 2 minutes)
 *   /duel stats       - show own duel record
 *   /duel stats <player> - show another player's record
 */
public final class DuelCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("duel")

				// /duel invite <player> [friendly|competitive]
				.then(Commands.literal("invite")
						.then(Commands.argument("target", EntityArgument.player())
								// Default mode: friendly
								.executes(ctx -> {
									ServerPlayer challenger = ctx.getSource().getPlayerOrException();
									ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
									return handleInvite(challenger, target, DuelManager.DuelMode.FRIENDLY);
								})
								// Explicit mode argument
								.then(Commands.argument("mode", StringArgumentType.word())
										.suggests((ctx, builder) -> {
											builder.suggest("friendly");
											builder.suggest("competitive");
											return builder.buildFuture();
										})
										.executes(ctx -> {
											ServerPlayer challenger = ctx.getSource().getPlayerOrException();
											ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
											String modeStr = StringArgumentType.getString(ctx, "mode");
											DuelManager.DuelMode mode = switch (modeStr.toLowerCase()) {
												case "competitive" -> DuelManager.DuelMode.COMPETITIVE;
												default -> DuelManager.DuelMode.FRIENDLY;
											};
											return handleInvite(challenger, target, mode);
										})
								)
						)
				)

				// /duel confirm
				.then(Commands.literal("confirm").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					return DuelManager.confirmInvite(player, player.level().getServer()) ? 1 : 0;
				}))

				// /duel cancel
				.then(Commands.literal("cancel").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					DuelManager.cancelConfirmation(player);
					return 1;
				}))

				// /duel accept
				.then(Commands.literal("accept").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					return DuelManager.acceptInvite(player, player.level().getServer()) ? 1 : 0;
				}))

				// /duel decline
				.then(Commands.literal("decline").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					DuelManager.declineInvite(player);
					return 1;
				}))

				// /duel forfeit
				.then(Commands.literal("forfeit").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					return DuelManager.forfeitDuel(player, player.level().getServer()) ? 1 : 0;
				}))

				// /duel stats (own) or /duel stats <player>
				.then(Commands.literal("stats")
						.executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							return showStats(ctx.getSource(), player);
						})
						.then(Commands.argument("target", EntityArgument.player())
								.executes(ctx -> {
									ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
									return showStats(ctx.getSource(), target);
								})
						)
				)
		);
	}

	private static int handleInvite(ServerPlayer challenger, ServerPlayer target,
									DuelManager.DuelMode mode) {
		return DuelManager.sendInvite(challenger, target, mode, challenger.level().getServer()) ? 1 : 0;
	}

	private static int showStats(CommandSourceStack source, ServerPlayer target) {
		CradlePlayerData data = CradlePlayerData.getOrCreate(target.getUUID());
		String name = target.getName().getString();

		// Check if the viewer is looking at their own stats
		boolean isSelf = false;
		try {
			ServerPlayer viewer = source.getPlayerOrException();
			isSelf = viewer.getUUID().equals(target.getUUID());
		} catch (Exception ignored) { }

		String header = isSelf ? "Your" : name + "'s";

		source.sendSuccess(() -> Component.literal(
				"\u00A76[Cradle] \u00A7e" + header + " Duel Record:"
		), false);
		source.sendSuccess(() -> Component.literal(
				"\u00A76  \u00A7aWins: \u00A7f" + data.getDuelWins()
						+ "  \u00A7cLosses: \u00A7f" + data.getDuelLosses()
						+ "  \u00A77Draws: \u00A7f" + data.getDuelDraws()
		), false);

		int total = data.getDuelWins() + data.getDuelLosses() + data.getDuelDraws();
		if (total > 0) {
			double winRate = (data.getDuelWins() * 100.0) / total;
			source.sendSuccess(() -> Component.literal(
					"\u00A76  \u00A77Win rate: \u00A7e" + String.format("%.1f%%", winRate)
							+ " \u00A77(" + total + " duels)"
			), false);
		}

		return 1;
	}
}

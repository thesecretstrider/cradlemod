package com.cradle.mod;

import com.cradle.mod.network.CradleSyncPayload;
import com.cradle.mod.network.DuelEndPayload;
import com.cradle.mod.network.DuelInviteReceivedPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the duel system: invitations, arena construction/restoration,
 * fight state, death handling, and stat tracking.
 *
 * Duel flow:
 * 1. Challenger: /duel invite <target> [mode]  →  stores in PENDING_CONFIRMATIONS
 * 2. Challenger: /duel confirm  →  moves to PENDING_INVITES, notifies target
 * 3. Target: /duel accept  →  builds arena, teleports, starts countdown
 * 4. Countdown: 3... 2... 1... FIGHT! (players frozen during countdown)
 * 5. Fight until death, forfeit (2min+), or disconnect
 * 6. End: restore arena terrain, teleport back, update stats
 */
public final class DuelManager {

	// ── Constants ──────────────────────────────────────────────────────

	private static final int ARENA_RADIUS = 25;           // 50 block diameter
	private static final int WALL_HEIGHT = 4;
	private static final int CLEAR_HEIGHT = 10;            // air above floor (plenty of room for combat + flight)
	private static final int COUNTDOWN_TICKS = 60;         // 3 seconds
	private static final long CONFIRM_EXPIRE_TICKS = 600;  // 30 seconds
	private static final long INVITE_EXPIRE_TICKS = 1200;  // 60 seconds
	private static final long FORFEIT_DELAY_TICKS = 2400;  // 2 minutes = 120 seconds
	private static final int SPAWN_OFFSET = 12;            // players placed 12 blocks from center

	// ── Enums ──────────────────────────────────────────────────────────

	public enum DuelMode {
		FRIENDLY, COMPETITIVE;

		public String displayName() {
			return this == FRIENDLY ? "Friendly" : "Competitive";
		}
	}

	public enum DuelState {
		COUNTDOWN,  // 3-second countdown, players frozen
		FIGHTING,   // active combat
		ENDING      // duel just ended, waiting for next tick to process
	}

	// ── Inner classes ──────────────────────────────────────────────────

	public static class DuelInvitation {
		public final UUID challengerId;
		public final UUID targetId;
		public final DuelMode mode;
		public final long createdAtTick;

		public DuelInvitation(UUID challengerId, UUID targetId, DuelMode mode, long createdAtTick) {
			this.challengerId = challengerId;
			this.targetId = targetId;
			this.mode = mode;
			this.createdAtTick = createdAtTick;
		}
	}

	public static class DuelMatch {
		public final UUID player1;
		public final UUID player2;
		public final DuelMode mode;
		public final long startTick;
		public final BlockPos arenaCenter;
		public final ServerLevel level;
		public final Map<BlockPos, BlockState> savedBlocks;
		public final Vec3 player1OriginalPos;
		public final Vec3 player2OriginalPos;
		public DuelState state;
		public int countdownTicksRemaining;
		public long fightStartTick;
		public boolean forfeitNotified; // whether the "forfeit available" message was sent

		// Deferred end state (set by ALLOW_DEATH, processed next tick)
		public UUID pendingWinner;
		public String endReason;

		public DuelMatch(UUID p1, UUID p2, DuelMode mode, long startTick,
						 BlockPos arenaCenter, ServerLevel level,
						 Vec3 p1Pos, Vec3 p2Pos) {
			this.player1 = p1;
			this.player2 = p2;
			this.mode = mode;
			this.startTick = startTick;
			this.arenaCenter = arenaCenter;
			this.level = level;
			this.savedBlocks = new HashMap<>();
			this.player1OriginalPos = p1Pos;
			this.player2OriginalPos = p2Pos;
			this.state = DuelState.COUNTDOWN;
			this.countdownTicksRemaining = COUNTDOWN_TICKS;
			this.fightStartTick = 0;
			this.forfeitNotified = false;
			this.pendingWinner = null;
			this.endReason = null;
		}

		public UUID getOpponent(UUID playerId) {
			return player1.equals(playerId) ? player2 : player1;
		}

		public Vec3 getOriginalPos(UUID playerId) {
			return player1.equals(playerId) ? player1OriginalPos : player2OriginalPos;
		}
	}

	// ── Static state ──────────────────────────────────────────────────

	// Pending confirmations: challenger UUID → invitation (awaiting /duel confirm)
	private static final Map<UUID, DuelInvitation> PENDING_CONFIRMATIONS = new ConcurrentHashMap<>();

	// Pending invites: target UUID → invitation (awaiting /duel accept)
	private static final Map<UUID, DuelInvitation> PENDING_INVITES = new ConcurrentHashMap<>();

	// Active duels: player UUID → match (BOTH players mapped to the SAME DuelMatch)
	private static final Map<UUID, DuelMatch> ACTIVE_DUELS = new ConcurrentHashMap<>();

	// ── Public API ────────────────────────────────────────────────────

	public static boolean isInDuel(UUID playerId) {
		return ACTIVE_DUELS.containsKey(playerId);
	}

	public static boolean hasPendingInvite(UUID targetId) {
		return PENDING_INVITES.containsKey(targetId);
	}

	/**
	 * Step 1: Challenger initiates a duel. Validates conditions and stores
	 * in PENDING_CONFIRMATIONS (challenger must /duel confirm to actually send).
	 * Returns true if the confirmation prompt was sent.
	 */
	public static boolean sendInvite(ServerPlayer challenger, ServerPlayer target,
									 DuelMode mode, MinecraftServer server) {
		UUID challengerId = challenger.getUUID();
		UUID targetId = target.getUUID();

		// Self-duel check
		if (challengerId.equals(targetId)) {
			challenger.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cYou cannot duel yourself."));
			return false;
		}

		// Stage check: both must be Lowgold+
		CradlePlayerData challengerData = CradlePlayerData.getOrCreate(challengerId);
		CradlePlayerData targetData = CradlePlayerData.getOrCreate(targetId);

		if (challengerData.getAdvancementStage().ordinal() < CradlePlayerData.AdvancementStage.LOW_GOLD.ordinal()) {
			challenger.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cYou must be at least Low Gold to duel."));
			return false;
		}
		if (targetData.getAdvancementStage().ordinal() < CradlePlayerData.AdvancementStage.LOW_GOLD.ordinal()) {
			challenger.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7c" + target.getName().getString() + " must be at least Low Gold to duel."));
			return false;
		}

		// Active duel check
		if (isInDuel(challengerId)) {
			challenger.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cYou are already in a duel."));
			return false;
		}
		if (isInDuel(targetId)) {
			challenger.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7c" + target.getName().getString() + " is already in a duel."));
			return false;
		}

		// Trial check
		if (RevelationTrialManager.isInTrial(challengerId)) {
			challenger.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cYou cannot duel during a revelation trial."));
			return false;
		}
		if (RevelationTrialManager.isInTrial(targetId)) {
			challenger.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7c" + target.getName().getString() + " is in a revelation trial."));
			return false;
		}

		// Pending invite check (target already has one)
		if (hasPendingInvite(targetId)) {
			challenger.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7c" + target.getName().getString() + " already has a pending duel invite."));
			return false;
		}

		// Pending confirmation check (challenger already has one)
		if (PENDING_CONFIRMATIONS.containsKey(challengerId)) {
			challenger.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cYou already have a pending duel confirmation. Use /duel confirm or /duel cancel."));
			return false;
		}

		// Store in confirmations
		DuelInvitation invite = new DuelInvitation(challengerId, targetId, mode, server.getTickCount());
		PENDING_CONFIRMATIONS.put(challengerId, invite);

		// Warn about terrain modification
		challenger.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A7e\u26A0 An arena will be built at the accept location, temporarily modifying terrain."));
		challenger.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A77Terrain will be restored after the duel ends."));
		challenger.sendSystemMessage(Component.literal("\u00A76[Cradle] ")
				.append(Component.literal("\u00A7a[Confirm]").withStyle(
						Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/duel confirm"))))
				.append(Component.literal(" "))
				.append(Component.literal("\u00A7c[Cancel]").withStyle(
						Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/duel cancel")))));

		return true;
	}

	/**
	 * Step 2: Challenger confirms. Moves invite to PENDING_INVITES and notifies target.
	 */
	public static boolean confirmInvite(ServerPlayer challenger, MinecraftServer server) {
		DuelInvitation invite = PENDING_CONFIRMATIONS.remove(challenger.getUUID());
		if (invite == null) {
			challenger.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cNo pending duel confirmation to confirm."));
			return false;
		}

		// Re-validate target is still online
		ServerPlayer target = server.getPlayerList().getPlayer(invite.targetId);
		if (target == null) {
			challenger.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cTarget player is no longer online."));
			return false;
		}

		// Store in invites (keyed by target UUID)
		PENDING_INVITES.put(invite.targetId, invite);

		// Notify challenger
		challenger.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A7aDuel invite sent to " + target.getName().getString()
						+ " (" + invite.mode.displayName() + "). Waiting for response..."));

		// Notify target with clickable chat messages (server-side sendSystemMessage supports ClickEvent)
		String modeDisplay = invite.mode == DuelMode.COMPETITIVE ? "\u00A7ccompetitive" : "\u00A7afriendly";
		target.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A7e" + challenger.getName().getString()
						+ " \u00A76challenges you to a " + modeDisplay + " \u00A76duel!"));
		target.sendSystemMessage(
				Component.literal("\u00A76[Cradle] ")
						.append(Component.literal("\u00A7a\u00A7l[Accept]").withStyle(
								Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/duel accept"))))
						.append(Component.literal(" "))
						.append(Component.literal("\u00A7c\u00A7l[Decline]").withStyle(
								Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/duel decline")))));

		// Also send S2C payload (for any future client-side handling)
		ServerPlayNetworking.send(target,
				new DuelInviteReceivedPayload(challenger.getName().getString(), invite.mode.name()));

		return true;
	}

	/**
	 * Cancel a pending confirmation (challenger decided not to send invite).
	 */
	public static void cancelConfirmation(ServerPlayer challenger) {
		DuelInvitation invite = PENDING_CONFIRMATIONS.remove(challenger.getUUID());
		if (invite != null) {
			challenger.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A77Duel invitation cancelled."));
		} else {
			challenger.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cNo pending duel confirmation to cancel."));
		}
	}

	/**
	 * Step 3: Target accepts the invite. Builds arena and starts duel.
	 */
	public static boolean acceptInvite(ServerPlayer target, MinecraftServer server) {
		DuelInvitation invite = PENDING_INVITES.remove(target.getUUID());
		if (invite == null) {
			target.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cYou don't have a pending duel invite."));
			return false;
		}

		ServerPlayer challenger = server.getPlayerList().getPlayer(invite.challengerId);
		if (challenger == null) {
			target.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cThe challenger is no longer online."));
			return false;
		}

		// Re-validate conditions (may have changed since invite was sent)
		if (isInDuel(invite.challengerId) || isInDuel(target.getUUID())) {
			target.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cOne of the players is already in a duel."));
			return false;
		}
		if (RevelationTrialManager.isInTrial(invite.challengerId) || RevelationTrialManager.isInTrial(target.getUUID())) {
			target.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cOne of the players is in a revelation trial."));
			return false;
		}

		// Disrupt cycling for both players if active
		CradlePlayerData challengerData = CradlePlayerData.getOrCreate(invite.challengerId);
		CradlePlayerData targetData = CradlePlayerData.getOrCreate(target.getUUID());
		if (challengerData.isActivelyCycling()) {
			CyclingManager.stopCycling(challenger, challengerData);
		}
		if (targetData.isActivelyCycling()) {
			CyclingManager.stopCycling(target, targetData);
		}

		// Start the duel!
		startDuel(challenger, target, invite.mode, server);
		return true;
	}

	/**
	 * Decline a pending invite.
	 */
	public static void declineInvite(ServerPlayer target) {
		DuelInvitation invite = PENDING_INVITES.remove(target.getUUID());
		if (invite == null) {
			target.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cYou don't have a pending duel invite."));
			return;
		}

		target.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A77You declined the duel invite."));

		// Notify challenger if online
		MinecraftServer server = target.level().getServer();
		if (server != null) {
			ServerPlayer challenger = server.getPlayerList().getPlayer(invite.challengerId);
			if (challenger != null) {
				challenger.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7c" + target.getName().getString() + " declined your duel invite."));
			}
		}
	}

	/**
	 * Forfeit an active duel. Only available after 2 minutes of fighting.
	 */
	public static boolean forfeitDuel(ServerPlayer player, MinecraftServer server) {
		DuelMatch match = ACTIVE_DUELS.get(player.getUUID());
		if (match == null) {
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cYou are not in a duel."));
			return false;
		}

		if (match.state != DuelState.FIGHTING) {
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cThe duel hasn't started fighting yet."));
			return false;
		}

		// Check 2-minute delay
		long ticksElapsed = server.getTickCount() - match.fightStartTick;
		if (ticksElapsed < FORFEIT_DELAY_TICKS) {
			long remainingTicks = FORFEIT_DELAY_TICKS - ticksElapsed;
			int remainingSec = (int) Math.ceil(remainingTicks / 20.0);
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7cForfeit is not yet available. " + remainingSec + "s remaining."));
			return false;
		}

		UUID winnerId = match.getOpponent(player.getUUID());
		endDuel(match, winnerId, player.getUUID(), server, "forfeit");
		return true;
	}

	// ── Duel lifecycle ────────────────────────────────────────────────

	/**
	 * Start a duel between two players. Builds arena, teleports, begins countdown.
	 */
	private static void startDuel(ServerPlayer p1, ServerPlayer p2,
								  DuelMode mode, MinecraftServer server) {
		// Arena center: midpoint between the two players (on the ground)
		BlockPos arenaCenter = new BlockPos(
				(int) Math.round((p1.getX() + p2.getX()) / 2.0),
				(int) Math.round(Math.min(p1.getY(), p2.getY())),
				(int) Math.round((p1.getZ() + p2.getZ()) / 2.0));

		ServerLevel level = (ServerLevel) p1.level();

		DuelMatch match = new DuelMatch(
				p1.getUUID(), p2.getUUID(), mode, server.getTickCount(),
				arenaCenter, level,
				p1.position(), p2.position());

		// Save terrain and build arena
		saveAndBuildArena(match, level);

		// Store match for both players
		ACTIVE_DUELS.put(p1.getUUID(), match);
		ACTIVE_DUELS.put(p2.getUUID(), match);

		// Teleport players to opposite sides of arena
		double spawnY = arenaCenter.getY() + 1.0;
		p1.teleportTo(level,
				arenaCenter.getX() - SPAWN_OFFSET + 0.5, spawnY,
				arenaCenter.getZ() + 0.5, Set.of(),
				0, 0, true);
		p2.teleportTo(level,
				arenaCenter.getX() + SPAWN_OFFSET + 0.5, spawnY,
				arenaCenter.getZ() + 0.5, Set.of(),
				180, 0, true);

		// Freeze players during countdown (high-level slowness + mining fatigue)
		int freezeDuration = COUNTDOWN_TICKS + 10; // slight buffer
		for (ServerPlayer p : List.of(p1, p2)) {
			p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, freezeDuration, 127, false, false, false));
			p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, freezeDuration, 5, false, false, false));
			p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, freezeDuration, 128, false, false, false));
		}

		// Send initial countdown title "3"
		sendCountdownTitle(p1, p2, "\u00A7e\u00A7l3");

		// Notify both players
		String modeStr = mode == DuelMode.COMPETITIVE ? "\u00A7c" + mode.displayName() : "\u00A7a" + mode.displayName();
		for (ServerPlayer p : List.of(p1, p2)) {
			p.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7e\u2694 " + modeStr + " \u00A7eduel begins!"));
		}

		CradleMod.LOGGER.info("Duel started: {} vs {} ({})",
				p1.getName().getString(), p2.getName().getString(), mode.displayName());
	}

	/**
	 * End a duel. Handles mode-specific outcomes, stat updates, arena restoration.
	 */
	private static void endDuel(DuelMatch match, UUID winnerId, UUID loserId,
								MinecraftServer server, String reason) {
		// Remove both players from active duels
		ACTIVE_DUELS.remove(match.player1);
		ACTIVE_DUELS.remove(match.player2);

		ServerPlayer winner = server.getPlayerList().getPlayer(winnerId);
		ServerPlayer loser = server.getPlayerList().getPlayer(loserId);

		String winnerName = winner != null ? winner.getName().getString() : "Unknown";
		String loserName = loser != null ? loser.getName().getString() : "Unknown";

		// Mode-specific end handling
		if (match.mode == DuelMode.FRIENDLY) {
			// Friendly: set loser to 1HP, heal winner, clear negative effects
			if (loser != null) {
				loser.setHealth(1.0f);
				clearCombatEffects(loser);
			}
			if (winner != null) {
				winner.setHealth(winner.getMaxHealth());
				clearCombatEffects(winner);
			}
		} else {
			// Competitive: winner healed, loser has already died (or will die if forfeit)
			if (winner != null) {
				winner.setHealth(winner.getMaxHealth());
				clearCombatEffects(winner);
			}
			if (loser != null && loser.isAlive() && "forfeit".equals(reason)) {
				// Forfeit in competitive: kill the loser
				loser.hurtServer((ServerLevel) loser.level(),
						loser.damageSources().generic(), Float.MAX_VALUE);
			}
		}

		// Update stats
		CradlePlayerData winnerData = CradlePlayerData.getOrCreate(winnerId);
		CradlePlayerData loserData = CradlePlayerData.getOrCreate(loserId);
		winnerData.setDuelWins(winnerData.getDuelWins() + 1);
		loserData.setDuelLosses(loserData.getDuelLosses() + 1);

		// Teleport players back to original positions (if online and alive)
		if (winner != null && winner.isAlive()) {
			Vec3 origPos = match.getOriginalPos(winnerId);
			winner.teleportTo(match.level,
					origPos.x, origPos.y, origPos.z, Set.of(),
					winner.getYRot(), winner.getXRot(), true);
		}
		if (loser != null && loser.isAlive()) {
			Vec3 origPos = match.getOriginalPos(loserId);
			loser.teleportTo(match.level,
					origPos.x, origPos.y, origPos.z, Set.of(),
					loser.getYRot(), loser.getXRot(), true);
		}

		// Restore arena terrain
		restoreArena(match, match.level);

		// Send title to both players
		Component winTitle = Component.literal("\u00A7a\u00A7l" + winnerName + " WINS!");
		for (ServerPlayer p : List.of(winner, loser)) {
			if (p != null) {
				p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 10));
				p.connection.send(new ClientboundSetTitleTextPacket(winTitle));
			}
		}

		// Send DuelEndPayload to both players
		if (winner != null) {
			ServerPlayNetworking.send(winner, new DuelEndPayload(
					winnerName, loserName, match.mode.name(),
					winnerData.getDuelWins(), winnerData.getDuelLosses(), winnerData.getDuelDraws()));
		}
		if (loser != null) {
			ServerPlayNetworking.send(loser, new DuelEndPayload(
					winnerName, loserName, match.mode.name(),
					loserData.getDuelWins(), loserData.getDuelLosses(), loserData.getDuelDraws()));
		}

		// Chat announcements
		String resultMsg = "\u00A76[Cradle] \u00A7e\u2694 " + winnerName + " \u00A7edefeated \u00A7c"
				+ loserName + " \u00A7ein a " + match.mode.displayName() + " duel! (" + reason + ")";
		for (ServerPlayer p : List.of(winner, loser)) {
			if (p != null) {
				p.sendSystemMessage(Component.literal(resultMsg));
			}
		}

		// Sync both players
		if (winner != null) {
			ServerPlayNetworking.send(winner, CyclingManager.createSyncPayload(winner, winnerData));
		}
		if (loser != null) {
			ServerPlayNetworking.send(loser, CyclingManager.createSyncPayload(loser, loserData));
		}

		// Auto-save
		CradleMod.autoSave(server);

		CradleMod.LOGGER.info("Duel ended: {} defeated {} ({}, {})",
				winnerName, loserName, match.mode.displayName(), reason);
	}

	// ── Arena construction/restoration ─────────────────────────────────

	/**
	 * Saves all blocks in the arena region and builds the arena.
	 */
	private static void saveAndBuildArena(DuelMatch match, ServerLevel level) {
		int cx = match.arenaCenter.getX();
		int cy = match.arenaCenter.getY();
		int cz = match.arenaCenter.getZ();

		// Save all blocks in the arena region
		for (int dx = -ARENA_RADIUS; dx <= ARENA_RADIUS; dx++) {
			for (int dz = -ARENA_RADIUS; dz <= ARENA_RADIUS; dz++) {
				if (!isInsideCircle(dx, dz, ARENA_RADIUS)) continue;

				for (int dy = -1; dy <= CLEAR_HEIGHT + WALL_HEIGHT; dy++) {
					BlockPos pos = new BlockPos(cx + dx, cy + dy, cz + dz);
					match.savedBlocks.put(pos, level.getBlockState(pos));
				}
			}
		}

		CradleMod.LOGGER.info("Saved {} blocks for duel arena at ({}, {}, {})",
				match.savedBlocks.size(), cx, cy, cz);

		// Build the arena
		BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState barrier = Blocks.BARRIER.defaultBlockState();

		for (int dx = -ARENA_RADIUS; dx <= ARENA_RADIUS; dx++) {
			for (int dz = -ARENA_RADIUS; dz <= ARENA_RADIUS; dz++) {
				if (!isInsideCircle(dx, dz, ARENA_RADIUS)) continue;

				boolean onEdge = isOnEdge(dx, dz, ARENA_RADIUS);

				// Sub-floor (Y-1): solid support
				level.setBlock(new BlockPos(cx + dx, cy - 1, cz + dz), stoneBricks, 3);

				// Floor (Y): stone bricks
				level.setBlock(new BlockPos(cx + dx, cy, cz + dz), stoneBricks, 3);

				// Interior (Y+1 to Y+CLEAR_HEIGHT): air
				for (int dy = 1; dy <= CLEAR_HEIGHT; dy++) {
					BlockPos pos = new BlockPos(cx + dx, cy + dy, cz + dz);
					if (onEdge && dy <= WALL_HEIGHT) {
						// Edge: barrier wall
						level.setBlock(pos, barrier, 3);
					} else {
						// Interior: air
						level.setBlock(pos, air, 3);
					}
				}
			}
		}
	}

	/**
	 * Restores all saved blocks from a duel arena.
	 */
	private static void restoreArena(DuelMatch match, ServerLevel level) {
		for (var entry : match.savedBlocks.entrySet()) {
			level.setBlock(entry.getKey(), entry.getValue(), 3);
		}
		CradleMod.LOGGER.info("Restored {} blocks for duel arena", match.savedBlocks.size());
	}

	// ── Tick handler ──────────────────────────────────────────────────

	/**
	 * Called every server tick. Handles expiry, countdown, and fight state.
	 */
	public static void onServerTick(MinecraftServer server) {
		long currentTick = server.getTickCount();

		// 1. Expire old pending confirmations (30 second timeout)
		PENDING_CONFIRMATIONS.entrySet().removeIf(entry -> {
			if (currentTick - entry.getValue().createdAtTick > CONFIRM_EXPIRE_TICKS) {
				ServerPlayer challenger = server.getPlayerList().getPlayer(entry.getKey());
				if (challenger != null) {
					challenger.sendSystemMessage(Component.literal(
							"\u00A76[Cradle] \u00A7cDuel confirmation expired."));
				}
				return true;
			}
			return false;
		});

		// 2. Expire old pending invites (60 second timeout)
		PENDING_INVITES.entrySet().removeIf(entry -> {
			if (currentTick - entry.getValue().createdAtTick > INVITE_EXPIRE_TICKS) {
				DuelInvitation inv = entry.getValue();
				ServerPlayer target = server.getPlayerList().getPlayer(inv.targetId);
				ServerPlayer challenger = server.getPlayerList().getPlayer(inv.challengerId);
				if (target != null) {
					target.sendSystemMessage(Component.literal(
							"\u00A76[Cradle] \u00A7cDuel invite expired."));
				}
				if (challenger != null) {
					challenger.sendSystemMessage(Component.literal(
							"\u00A76[Cradle] \u00A7cYour duel invite to "
									+ (target != null ? target.getName().getString() : "the player")
									+ " expired."));
				}
				return true;
			}
			return false;
		});

		// 3. Process active duels
		Set<DuelMatch> processed = new HashSet<>();
		for (var entry : new ArrayList<>(ACTIVE_DUELS.entrySet())) {
			DuelMatch match = entry.getValue();
			if (processed.contains(match)) continue;
			processed.add(match);

			ServerPlayer p1 = server.getPlayerList().getPlayer(match.player1);
			ServerPlayer p2 = server.getPlayerList().getPlayer(match.player2);

			// If either player disconnected during an active duel
			if (p1 == null || p2 == null) {
				UUID winnerId = (p1 != null) ? match.player1 : match.player2;
				UUID loserId = (p1 == null) ? match.player1 : match.player2;
				endDuel(match, winnerId, loserId, server, "opponent disconnected");
				continue;
			}

			switch (match.state) {
				case COUNTDOWN -> {
					match.countdownTicksRemaining--;
					// Title text at 1-second intervals: 3 → 2 → 1 → FIGHT!
					if (match.countdownTicksRemaining == 40) {
						sendCountdownTitle(p1, p2, "\u00A7e\u00A7l2");
					} else if (match.countdownTicksRemaining == 20) {
						sendCountdownTitle(p1, p2, "\u00A7e\u00A7l1");
					} else if (match.countdownTicksRemaining <= 0) {
						// FIGHT!
						sendCountdownTitle(p1, p2, "\u00A7c\u00A7lFIGHT!");
						match.state = DuelState.FIGHTING;
						match.fightStartTick = currentTick;
						// Remove freeze effects
						for (ServerPlayer p : List.of(p1, p2)) {
							p.removeEffect(MobEffects.SLOWNESS);
							p.removeEffect(MobEffects.MINING_FATIGUE);
							p.removeEffect(MobEffects.JUMP_BOOST);
						}
					}
				}
				case FIGHTING -> {
					// Check if forfeit is now available (2 minutes)
					long ticksElapsed = currentTick - match.fightStartTick;
					if (!match.forfeitNotified && ticksElapsed >= FORFEIT_DELAY_TICKS) {
						match.forfeitNotified = true;
						for (ServerPlayer p : List.of(p1, p2)) {
							p.sendSystemMessage(Component.literal(
									"\u00A76[Cradle] \u00A7eForfeit is now available. Use ")
									.append(Component.literal("\u00A7a/duel forfeit").withStyle(
											Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/duel forfeit"))))
									.append(Component.literal("\u00A7e to concede.")));
						}
					}
				}
				case ENDING -> {
					// Deferred end from ALLOW_DEATH: process this tick
					if (match.pendingWinner != null) {
						UUID loserId = match.getOpponent(match.pendingWinner);
						endDuel(match, match.pendingWinner, loserId, server,
								match.endReason != null ? match.endReason : "combat");
					}
				}
			}
		}
	}

	// ── Death handling ────────────────────────────────────────────────

	/**
	 * Called from ALLOW_DEATH event. Returns false to prevent death in friendly duels.
	 * For friendly mode: prevents death, sets 1HP, defers duel end to next tick.
	 * For competitive mode: allows death normally.
	 */
	public static boolean shouldAllowDeath(ServerPlayer player) {
		DuelMatch match = ACTIVE_DUELS.get(player.getUUID());
		if (match == null) return true; // not in duel
		if (match.state != DuelState.FIGHTING) return true; // not fighting yet

		if (match.mode == DuelMode.COMPETITIVE) {
			return true; // competitive: allow real death
		}

		// FRIENDLY: prevent death, set to 1HP, defer duel end
		player.setHealth(1.0f);
		match.pendingWinner = match.getOpponent(player.getUUID());
		match.endReason = "combat";
		match.state = DuelState.ENDING;
		return false; // prevent death
	}

	/**
	 * Called from AFTER_DEATH event. Handles competitive duel deaths.
	 */
	public static void onPlayerDeath(ServerPlayer deadPlayer, MinecraftServer server) {
		DuelMatch match = ACTIVE_DUELS.get(deadPlayer.getUUID());
		if (match == null) return;
		if (match.state != DuelState.FIGHTING) return;

		// Only handle competitive mode here (friendly is handled by ALLOW_DEATH)
		if (match.mode == DuelMode.COMPETITIVE) {
			UUID winnerId = match.getOpponent(deadPlayer.getUUID());
			endDuel(match, winnerId, deadPlayer.getUUID(), server, "combat");
		}
	}

	// ── Disconnect handling ───────────────────────────────────────────

	/**
	 * Called when a player disconnects. Cleans up invites and ends active duels.
	 */
	public static void onPlayerDisconnect(UUID playerId, MinecraftServer server) {
		// Cancel pending confirmations
		PENDING_CONFIRMATIONS.remove(playerId);

		// Cancel pending invites where this player is challenger or target
		PENDING_INVITES.entrySet().removeIf(entry -> {
			DuelInvitation inv = entry.getValue();
			return inv.challengerId.equals(playerId) || inv.targetId.equals(playerId);
		});

		// End active duel
		DuelMatch match = ACTIVE_DUELS.get(playerId);
		if (match != null) {
			UUID winnerId = match.getOpponent(playerId);
			endDuel(match, winnerId, playerId, server, "opponent disconnected");
		}
	}

	// ── Server lifecycle ──────────────────────────────────────────────

	/**
	 * Clear all duel state (called on server stop).
	 */
	public static void clearAll() {
		PENDING_CONFIRMATIONS.clear();
		PENDING_INVITES.clear();
		ACTIVE_DUELS.clear();
	}

	// ── Helpers ───────────────────────────────────────────────────────

	private static boolean isInsideCircle(int dx, int dz, int radius) {
		return dx * dx + dz * dz <= radius * radius;
	}

	private static boolean isOnEdge(int dx, int dz, int radius) {
		int distSq = dx * dx + dz * dz;
		return distSq <= radius * radius && distSq > (radius - 1) * (radius - 1);
	}

	private static void sendCountdownTitle(ServerPlayer p1, ServerPlayer p2, String text) {
		Component title = Component.literal(text);
		for (ServerPlayer p : List.of(p1, p2)) {
			p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 25, 5));
			p.connection.send(new ClientboundSetTitleTextPacket(title));
		}
	}

	private static void clearCombatEffects(ServerPlayer player) {
		player.removeEffect(MobEffects.POISON);
		player.removeEffect(MobEffects.WITHER);
		player.removeEffect(MobEffects.SLOWNESS);
		player.removeEffect(MobEffects.MINING_FATIGUE);
		player.removeEffect(MobEffects.WEAKNESS);
		player.removeEffect(MobEffects.BLINDNESS);
		player.removeEffect(MobEffects.HUNGER);
		player.removeEffect(MobEffects.JUMP_BOOST);
	}
}

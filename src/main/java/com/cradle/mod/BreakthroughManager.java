package com.cradle.mod;

import com.cradle.mod.ability.AbilityRegistry;
import com.cradle.mod.ability.PlayerLoadout;
import com.cradle.mod.item.CradleItems;
import com.cradle.mod.network.AbilityLoadoutSyncPayload;
import com.cradle.mod.network.OpenIconSelectionPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Handles advancement stage breakthroughs. When a player reaches the
 * required level AND has the required items, they break through to the next stage.
 *
 * Progression: Foundation → Copper → Iron → Jade → Low Gold → High Gold → Truegold
 *              → Underlord → Overlord → Archlord → (Sage or Herald) → (the other) → Monarch
 *
 * After Archlord, the player must choose Sage or Herald first.
 * After achieving one, they can advance to the other.
 * After achieving both, they advance to Monarch.
 */
public final class BreakthroughManager {

	/**
	 * Returns the level required to reach the given stage.
	 * Returns Integer.MAX_VALUE for FOUNDATION (you start there)
	 * and for any unknown stage.
	 */
	public static int getLevelForStage(CradlePlayerData.AdvancementStage stage) {
		return switch (stage) {
			case COPPER -> 10;
			case IRON -> 25;
			case JADE -> 50;
			case LOW_GOLD -> 120; // Higher than before — Remnant absorption is the primary path (level 100)
			case HIGH_GOLD -> 130;
			case TRUEGOLD -> 165;
			case UNDERLORD -> 200;
			case OVERLORD -> 250;
			case ARCHLORD -> 300;
			case SAGE, HERALD -> 350;
			case MONARCH -> 400;
			default -> Integer.MAX_VALUE;
		};
	}

	/**
	 * Returns the item required to advance TO the given stage, or null if none needed.
	 */
	public static Item getRequiredItem(CradlePlayerData.AdvancementStage stage) {
		return switch (stage) {
			case COPPER -> CradleItems.VITAL_FRUIT;
			case IRON -> CradleItems.SPIRIT_FRUIT;
			case JADE -> CradleItems.SPIRIT_STONE;
			case LOW_GOLD -> CradleItems.SPIRIT_STONE; // Natural accumulation path (3 stones, harder)
			case UNDERLORD -> CradleItems.UNDERLORD_REVELATION;
			case OVERLORD -> CradleItems.OVERLORD_REVELATION;
			case ARCHLORD -> CradleItems.ARCHLORD_REVELATION;
			default -> null;
		};
	}

	/**
	 * Returns how many of the required item are needed to advance TO the given stage.
	 */
	public static int getRequiredItemCount(CradlePlayerData.AdvancementStage stage) {
		return switch (stage) {
			case COPPER -> 5;  // 5 Vital Fruits
			case IRON -> 5;    // 5 Spirit Fruits
			case JADE -> 1;    // 1 Spirit Stone
			case LOW_GOLD -> 3; // 3 Spirit Stones (natural accumulation — expensive to encourage Remnant path)
			case UNDERLORD -> 1; // 1 Underlord Revelation
			case OVERLORD -> 1;  // 1 Overlord Revelation
			case ARCHLORD -> 1;  // 1 Archlord Revelation
			default -> 0;
		};
	}

	/**
	 * Returns the display name for the required item (for chat messages).
	 */
	public static String getRequiredItemName(CradlePlayerData.AdvancementStage stage) {
		return switch (stage) {
			case COPPER -> "Vital Fruit";
			case IRON -> "Spirit Fruit";
			case JADE -> "Spirit Stone";
			case LOW_GOLD -> "Spirit Stone";
			case UNDERLORD -> "Underlord Revelation";
			case OVERLORD -> "Overlord Revelation";
			case ARCHLORD -> "Archlord Revelation";
			default -> "";
		};
	}

	/**
	 * Returns the maxMadra boost granted when reaching the given stage.
	 */
	public static float getMaxMadraBoost(CradlePlayerData.AdvancementStage stage) {
		return switch (stage) {
			case COPPER -> 200f;
			case IRON -> 400f;
			case JADE -> 800f;
			case LOW_GOLD -> 1200f;
			case HIGH_GOLD -> 1500f;
			case TRUEGOLD -> 2000f;
			case UNDERLORD -> 3000f;
			case OVERLORD -> 4000f;
			case ARCHLORD -> 5500f;
			case SAGE, HERALD -> 4000f;
			case MONARCH -> 8000f;
			default -> 0f;
		};
	}

	// ── Sage/Herald branching logic ──────────────────────────────────

	/**
	 * Determines the next advancement stage for the given player data,
	 * accounting for the Sage/Herald branching after Archlord.
	 *
	 * After Archlord:
	 *   - If neither Sage nor Herald chosen: returns null (player must choose via UI)
	 *   - If has Sage or Herald: next is MONARCH (combines both into one step)
	 * At Sage: next is MONARCH (Remnant fight required to gain Herald half)
	 * At Herald: next is MONARCH (Icon touch required to gain Sage half)
	 * At Monarch: returns null (max stage)
	 * For all other stages: linear progression via next()
	 */
	public static CradlePlayerData.AdvancementStage getNextStage(CradlePlayerData data) {
		CradlePlayerData.AdvancementStage current = data.getAdvancementStage();

		if (current == CradlePlayerData.AdvancementStage.MONARCH) {
			return null; // Already at max
		}

		if (current == CradlePlayerData.AdvancementStage.ARCHLORD) {
			// Branching: player must choose Sage or Herald first
			if (!data.hasSage() && !data.hasHerald()) {
				return null; // Needs to choose via UI — handled by ChooseSageHeraldPayload
			}
			// After choosing one, next step is always Monarch
			// (the missing half is acquired as part of the Monarch advancement)
			return CradlePlayerData.AdvancementStage.MONARCH;
		}

		// Sage -> Monarch (must fight Remnant to gain Herald half)
		if (current == CradlePlayerData.AdvancementStage.SAGE) {
			return CradlePlayerData.AdvancementStage.MONARCH;
		}

		// Herald -> Monarch (must touch Icon to gain Sage half)
		if (current == CradlePlayerData.AdvancementStage.HERALD) {
			return CradlePlayerData.AdvancementStage.MONARCH;
		}

		// Linear progression for all other stages
		return current.next();
	}

	// ── Inventory helpers ────────────────────────────────────────────

	/**
	 * Counts how many of the given item the player has in their inventory.
	 */
	private static int countItemInInventory(ServerPlayer player, Item item) {
		int count = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/**
	 * Removes a specific count of the given item from the player's inventory.
	 * Assumes the player has enough (check with countItemInInventory first).
	 */
	private static void removeItemFromInventory(ServerPlayer player, Item item, int amount) {
		int remaining = amount;
		for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) {
				int toRemove = Math.min(remaining, stack.getCount());
				stack.shrink(toRemove);
				remaining -= toRemove;
			}
		}
	}

	// ── Breakthrough checks ──────────────────────────────────────────

	/**
	 * Check if the player qualifies for a breakthrough after leveling up.
	 * Only NOTIFIES the player when they reach the required level — does NOT
	 * auto-advance. The player must use the "Advance" button to actually break through.
	 */
	public static void checkBreakthrough(ServerPlayer player, CradlePlayerData data) {
		CradlePlayerData.AdvancementStage nextStage = getNextStage(data);
		if (nextStage == null) {
			return; // At max or needs Sage/Herald choice
		}

		// Special notification at level 100 for Jade players — Remnant absorption is now available
		if (nextStage == CradlePlayerData.AdvancementStage.LOW_GOLD
				&& data.getPlayerLevel() == 100
				&& data.getAdvancementStage() == CradlePlayerData.AdvancementStage.JADE) {
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7eYour spirit is strong enough to absorb a Remnant! " +
					"Find a Remnant of your Path and right-click to begin absorption."
			));
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A77Alternatively, reach level 120 with 3x Spirit Stones to advance without a Remnant."
			));
		}

		int requiredLevel = getLevelForStage(nextStage);

		// Only notify once when they first reach the required level
		if (data.getPlayerLevel() == requiredLevel) {
			Item requiredItem = getRequiredItem(nextStage);
			int requiredCount = getRequiredItemCount(nextStage);

			if (requiredItem != null && requiredCount > 0) {
				String itemName = getRequiredItemName(nextStage);
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7fYou have reached the level for \u00A7e" +
								nextStage.displayName() + "\u00A7f! Collect \u00A7c" +
								requiredCount + "x " + itemName + "\u00A7f and press \u00A7eAdvance\u00A7f in your Sacred Artist Status (J) to break through!"
				));
			} else {
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7fYou have reached the level for \u00A7e" +
								nextStage.displayName() + "\u00A7f! Press \u00A7eAdvance\u00A7f in your Sacred Artist Status (J) to break through!"
				));
			}
		}
	}

	/**
	 * Returns true if the player meets ALL requirements to advance to the next stage:
	 * - Has the required level
	 * - Has the required items in inventory
	 * - For Sage/Herald branching: a choice has been made (getNextStage returns non-null)
	 */
	public static boolean canAdvance(ServerPlayer player, CradlePlayerData data) {
		CradlePlayerData.AdvancementStage nextStage = getNextStage(data);
		if (nextStage == null) {
			return false;
		}

		int requiredLevel = getLevelForStage(nextStage);
		if (data.getPlayerLevel() < requiredLevel) {
			return false;
		}

		Item requiredItem = getRequiredItem(nextStage);
		int requiredCount = getRequiredItemCount(nextStage);
		if (requiredItem != null && requiredCount > 0) {
			int playerHas = countItemInInventory(player, requiredItem);
			if (playerHas < requiredCount) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Attempt a breakthrough. For most stages this is instant. For Lord stages
	 * (Underlord/Overlord/Archlord), it consumes the item and starts a revelation
	 * trial — the player must kill spirits to complete the breakthrough.
	 *
	 * Returns true if the breakthrough started (item consumed) or completed instantly.
	 */
	public static boolean attemptBreakthrough(ServerPlayer player, CradlePlayerData data) {
		if (!canAdvance(player, data)) {
			return false;
		}

		// Block if already in a trial
		if (RevelationTrialManager.isInTrial(player.getUUID())) {
			player.displayClientMessage(Component.literal(
					"\u00A7cYou are already undergoing a revelation trial!"
			), true);
			return false;
		}

		CradlePlayerData.AdvancementStage nextStage = getNextStage(data);
		if (nextStage == null) return false;

		// Consume required items
		Item requiredItem = getRequiredItem(nextStage);
		int requiredCount = getRequiredItemCount(nextStage);
		if (requiredItem != null && requiredCount > 0) {
			removeItemFromInventory(player, requiredItem, requiredCount);
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A77" + requiredCount + "x " +
							getRequiredItemName(nextStage) + " consumed."
			));
		}

		// Lord stages require a revelation trial instead of instant advancement
		if (RevelationTrialManager.requiresTrial(nextStage)) {
			RevelationTrialManager.startTrial(player, nextStage);
			return true; // Item consumed, trial started
		}

		// Monarch advancement — requires the missing half:
		// Sage player must fight their Remnant (Herald trial) to become Monarch.
		// Herald player must choose an Icon (Sage half) to become Monarch.
		if (nextStage == CradlePlayerData.AdvancementStage.MONARCH) {
			if (data.hasSage() && !data.hasHerald()) {
				// Sage → Monarch: must fight Remnant to merge body+spirit
				RevelationTrialManager.startHeraldTrial(player, data);
				return true; // Trial started — completion grants Monarch
			}
			if (data.hasHerald() && !data.hasSage()) {
				// Herald → Monarch: must choose an Icon to gain Sage half
				if (data.getChosenIcon() == CradlePlayerData.Icon.NONE) {
					// Open Icon selection screen with forMonarch=true
					ServerPlayNetworking.send(player,
							new OpenIconSelectionPayload(data.getChosenPath().name(), true));
					return true; // Awaiting Icon selection — ChooseIconPayload completes Monarch
				}
				// Already has Icon — fall through to performBreakthrough
			}
		}

		// All other stages: instant breakthrough
		performBreakthrough(player, data, nextStage);
		return true;
	}

	/**
	 * Performs the actual stage advancement: Iron Body check, set stage,
	 * boost Madra, notify player. Called directly for instant breakthroughs
	 * and by RevelationTrialManager when a trial is completed.
	 */
	public static void performBreakthrough(ServerPlayer player, CradlePlayerData data,
										   CradlePlayerData.AdvancementStage nextStage) {
		// Check for Iron Body crystal when advancing to Iron stage
		if (nextStage == CradlePlayerData.AdvancementStage.IRON) {
			CradlePlayerData.IronBody bodyType = detectIronBodyCrystal(player);
			data.setIronBody(bodyType);
			if (bodyType != CradlePlayerData.IronBody.NONE) {
				consumeCrystalFromHands(player, bodyType);
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A7d" + bodyType.displayName() +
								" Iron Body awakened!"
				));
				String ironBodyLore = switch (bodyType) {
					case BLOODFORGED -> "Pain fuels your restoration. Every wound makes you stronger.";
					case STEELBORN -> "Your body hardens like sacred iron. Blows glance off your skin.";
					case RAINDROP -> "You flow like water. Your reflexes sharpen beyond mortal limits.";
					default -> "";
				};
				if (!ironBodyLore.isEmpty()) {
					player.sendSystemMessage(Component.literal(
							"\u00A76[Cradle] \u00A7d\u00A7o" + ironBodyLore
					));
				}
			} else {
				player.sendSystemMessage(Component.literal(
						"\u00A76[Cradle] \u00A77You advance to Iron without a body crystal. " +
						"Your body is strong, but lacks the refinement of an Iron Body."
				));
			}
		}

		// Track Sage/Herald achievement
		if (nextStage == CradlePlayerData.AdvancementStage.SAGE) {
			data.setHasSage(true);
		} else if (nextStage == CradlePlayerData.AdvancementStage.HERALD) {
			data.setHasHerald(true);
		} else if (nextStage == CradlePlayerData.AdvancementStage.MONARCH) {
			// Monarch = both Sage + Herald. Grant whichever is missing.
			// Herald→Monarch path: player gains Sage (Icon) as part of ascending.
			// Sage→Monarch path: Herald is granted by the Remnant trial completion.
			if (!data.hasSage()) {
				data.setHasSage(true);
				data.setCurrentWillpower(data.getMaxWillpower());
			}
			if (!data.hasHerald()) {
				data.setHasHerald(true);
			}
		}

		// Advance stage
		data.setAdvancementStage(nextStage);

		// Boost maxMadra
		float boost = getMaxMadraBoost(nextStage);
		data.setMaxMadra(data.getMaxMadra() + boost);

		// Madra purification: reset to 25% of new max
		data.setCurrentMadra(data.getMaxMadra() * 0.25f);

		// Notify the player
		player.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A7d\u00A7lBREAKTHROUGH! \u00A7fYou have advanced to \u00A7e" +
						nextStage.displayName() + "\u00A7f!"
		));
		player.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A7fYour Madra has been purified. Your power grows denser..."
		));

		// Auto-enable Copper Sight when reaching Copper stage
		if (nextStage == CradlePlayerData.AdvancementStage.COPPER) {
			data.setCopperSightActive(true);
			player.displayClientMessage(Component.literal(
					"\u00A7bCopper Sight awakened! Press H to toggle. Vital aura is now visible."), true);
		}

		// Stage-specific lore narrative
		String narrative = getBreakthroughNarrative(nextStage);
		if (!narrative.isEmpty()) {
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A7d\u00A7o" + narrative
			));
		}

		// Natural Gold advancement (no Remnant) — extra message noting no Goldsign
		if (nextStage == CradlePlayerData.AdvancementStage.LOW_GOLD && !data.hasGoldsign()) {
			player.sendSystemMessage(Component.literal(
					"\u00A76[Cradle] \u00A77You reached Gold through pure willpower and accumulated madra. " +
					"Without a Remnant, you bear no Goldsign — but your spirit is no less strong."
			));
		}

		// ── Skill Tree: stage-gate ability picks + upgrade points ────
		handleSkillTreeProgression(player, data, nextStage);

		// Monarch world event — visible and audible to ALL players globally
		if (nextStage == CradlePlayerData.AdvancementStage.MONARCH) {
			triggerMonarchWorldEvent(player, data);
		}

		CradleMod.LOGGER.info("Player {} broke through to {} at level {}",
				player.getName().getString(), nextStage.name(), data.getPlayerLevel());
	}

	/**
	 * Called by RemnantEntity when a player absorbs a compatible Remnant at Jade.
	 * Advances directly to Low Gold, bypassing normal item requirements.
	 * The Goldsign should already be set on the player data before calling this.
	 */
	public static void performRemnantBreakthrough(ServerPlayer player, CradlePlayerData data) {
		CradlePlayerData.AdvancementStage nextStage = CradlePlayerData.AdvancementStage.LOW_GOLD;

		// Show Remnant-specific Goldsign narrative before the standard breakthrough
		String remnantNarrative = getRemnantBreakthroughNarrative(data.getGoldsign());
		player.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A7d\u00A7o" + remnantNarrative
		));

		performBreakthrough(player, data, nextStage);
	}

	// ── Skill Tree Progression ──────────────────────────────────────

	/**
	 * Handles skill tree ability unlocks and upgrade points at each stage gate.
	 * Called from performBreakthrough after the stage is set and madra is boosted.
	 *
	 * - Copper: Prompt to pick 1 of 3 path-specific Strikers (slot 1)
	 * - Iron: Prompt to pick Enforcer or Ruler (slot 2)
	 * - Low Gold: Auto-assign remaining type (slot 3)
	 * - Underlord+: Grant 1 upgrade point per advancement
	 */
	private static void handleSkillTreeProgression(ServerPlayer player, CradlePlayerData data,
												   CradlePlayerData.AdvancementStage stage) {
		PlayerLoadout loadout = data.getLoadout();
		String pathName = data.getChosenPath().name();

		switch (stage) {
			case COPPER -> {
				// First real technique choice: pick 1 of 3 path-specific Strikers
				player.displayClientMessage(Component.literal(
						"\u00A7a\u2694 Striker slot unlocked! Press K to choose a technique."), true);
			}
			case IRON -> {
				// Pick Enforcer or Ruler for slot 2
				player.displayClientMessage(Component.literal(
						"\u00A7a\u2694 New slot unlocked! Press K to choose Enforcer or Ruler."), true);
			}
			case LOW_GOLD -> {
				// Auto-assign the remaining type (Enforcer or Ruler) to slot 3
				// Determine what they already have in slot 2 and give the other
				String slot2Ability = loadout.getAbility(2);
				boolean hasEnforcer = false;
				boolean hasRuler = false;

				// Check all equipped abilities for type
				for (int i = 0; i < PlayerLoadout.MAX_SLOTS; i++) {
					String id = loadout.getAbility(i);
					if (id != null && !id.equals("basic_enforcement")) {
						var def = AbilityRegistry.get(id);
						if (def != null) {
							switch (def.getType()) {
								case ENFORCER -> hasEnforcer = true;
								case RULER -> hasRuler = true;
								default -> {}
							}
						}
					}
				}

				// Give the missing type
				String autoAbilityId = null;
				String autoAbilityName = null;
				if (!hasRuler) {
					// Give the path's ruler
					autoAbilityId = getPathRulerId(pathName);
					autoAbilityName = "Ruler";
				} else if (!hasEnforcer) {
					// Give the path's enforcer
					autoAbilityId = getPathEnforcerId(pathName);
					autoAbilityName = "Enforcer";
				}

				if (autoAbilityId != null && !loadout.hasAbility(3)) {
					loadout.equipAbility(3, autoAbilityId);
					var def = AbilityRegistry.get(autoAbilityId);
					String displayName = def != null ? def.getDisplayName() : autoAbilityId;
					player.displayClientMessage(Component.literal(
							"\u00A7a\u2694 " + displayName + " (" + autoAbilityName + ") unlocked!"), true);
				} else {
					player.displayClientMessage(Component.literal(
							"\u00A7a\u2694 New slot unlocked! Press K for Skill Tree."), true);
				}
			}
			case UNDERLORD, OVERLORD, ARCHLORD, SAGE, HERALD, MONARCH -> {
				// Grant 1 upgrade point per advancement from Underlord+
				loadout.addUpgradePoints(1);
				player.displayClientMessage(Component.literal(
						"\u00A7b\u2B50 Upgrade point gained! (" + loadout.getUpgradePoints()
								+ " available) Press K to upgrade."), true);
			}
			default -> {
				// No skill tree changes for other stages (Jade, High Gold, Truegold)
			}
		}

		// Sync the loadout to the client
		syncLoadoutToClient(player, data);
	}

	/**
	 * Get the path-specific Enforcer ability ID.
	 */
	private static String getPathEnforcerId(String pathName) {
		return switch (pathName) {
			case "BLACK_FLAME" -> "blackflame_burning_body";
			case "ENDLESS_SWORD" -> "endless_flowing_edge";
			case "STELLAR_SPEAR" -> "stellar_alignment";
			case "CLOUD_HAMMER" -> "cloud_thunderous_weight";
			case "HOLLOW_KING" -> "hollow_circulation";
			default -> null;
		};
	}

	/**
	 * Get the path-specific Ruler ability ID.
	 */
	private static String getPathRulerId(String pathName) {
		return switch (pathName) {
			case "BLACK_FLAME" -> "blackflame_domain_of_ash";
			case "ENDLESS_SWORD" -> "endless_field_of_blades";
			case "STELLAR_SPEAR" -> "stellar_spear_domain";
			case "CLOUD_HAMMER" -> "cloud_gravity_field";
			case "HOLLOW_KING" -> "hollow_domain";
			default -> null;
		};
	}

	/**
	 * Sync loadout data to the client (utility wrapper).
	 */
	private static void syncLoadoutToClient(ServerPlayer player, CradlePlayerData data) {
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

	// ── Monarch World Event ─────────────────────────────────────────

	/**
	 * Triggers a dramatic, globally-visible world event when a player becomes a Monarch.
	 * Inspired by the End Portal opening — every player on the server sees and hears this.
	 * Canon: When a Monarch is born, reality itself shakes. All of Cradle knows.
	 */
	public static void triggerMonarchWorldEvent(ServerPlayer monarch, CradlePlayerData data) {
		var server = monarch.level().getServer();
		if (server == null) return;

		String monarchName = monarch.getName().getString();

		// ── 1. Title text to ALL players ──
		Component title = Component.literal("\u00A76\u00A7lMONARCH");
		Component subtitle = Component.literal(
				"\u00A7b" + monarchName + " has ascended to the pinnacle of power");

		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.connection.send(new ClientboundSetTitlesAnimationPacket(20, 100, 40));
			p.connection.send(new ClientboundSetTitleTextPacket(title));
			p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		}

		// ── 2. Sound — Ender Dragon death + thunder to every player ──
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			// Play at each player's own position so everyone hears at full volume
			p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
					SoundEvents.ENDER_DRAGON_DEATH, SoundSource.MASTER, 1.0f, 1.0f);
			p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
					SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.MASTER, 2.0f, 0.8f);
		}

		// ── 3. Thunder — set thunderstorm for 200 ticks (10 seconds) globally ──
		for (ServerLevel level : server.getAllLevels()) {
			level.setWeatherParameters(0, 200, true, true);
		}

		// ── 4. Lightning ring — 8 lightning bolts in a circle (radius 15) ──
		if (monarch.level() instanceof ServerLevel monarchLevel) {
			for (int i = 0; i < 8; i++) {
				double angle = (2 * Math.PI * i) / 8.0;
				double lx = monarch.getX() + 15.0 * Math.cos(angle);
				double lz = monarch.getZ() + 15.0 * Math.sin(angle);

				LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, monarchLevel);
				bolt.setPos(lx, monarch.getY(), lz);
				bolt.setVisualOnly(false); // Real lightning — dramatic!
				monarchLevel.addFreshEntity(bolt);
			}

			// ── 5. Particle explosion — massive burst at the Monarch's location ──
			// 200 Soul Fire Flames (expanding outward)
			monarchLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					monarch.getX(), monarch.getY() + 1.0, monarch.getZ(),
					200, 3.0, 2.0, 3.0, 0.15);

			// 100 End Rod particles (golden sparkle)
			monarchLevel.sendParticles(ParticleTypes.END_ROD,
					monarch.getX(), monarch.getY() + 1.5, monarch.getZ(),
					100, 2.5, 3.0, 2.5, 0.1);

			// 50 Witch particles (purple sparkle mist)
			monarchLevel.sendParticles(ParticleTypes.WITCH,
					monarch.getX(), monarch.getY() + 0.5, monarch.getZ(),
					50, 4.0, 1.0, 4.0, 0.05);
		}

		// ── 6. Chat broadcast to ALL players ──
		Component broadcast = Component.literal(
				"\u00A76[Cradle] \u00A7b\u00A7l\u2726 A new Monarch has been born! " +
				"The world trembles at the ascension of " + monarchName + "! \u2726");
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.sendSystemMessage(broadcast);
		}

		// ── 7. Rumble — play explosion sound to all players for screen shake feel ──
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
					SoundEvents.GENERIC_EXPLODE, SoundSource.MASTER, 0.5f, 0.5f);
		}

		// ── 8. Personal lore messages to the Monarch ──
		monarch.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A7b\u00A7l\u2726 You have ascended. Reality bows before you. \u2726"));
		monarch.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A7b\u00A7oYou stand at the pinnacle of Cradle. Sage and Herald, united in one being."));
		monarch.sendSystemMessage(Component.literal(
				"\u00A76[Cradle] \u00A7bV for Authority. B for Spirit Shift. All power is yours."));

		CradleMod.LOGGER.info("Monarch world event triggered for player {}", monarchName);
	}

	/**
	 * Checks both hands for an Iron Body crystal item.
	 * Returns the matching IronBody type, or NONE if no crystal is held.
	 */
	private static CradlePlayerData.IronBody detectIronBodyCrystal(ServerPlayer player) {
		ItemStack mainHand = player.getMainHandItem();
		ItemStack offHand = player.getOffhandItem();

		if (mainHand.is(CradleItems.BLOODFORGED_CRYSTAL) || offHand.is(CradleItems.BLOODFORGED_CRYSTAL)) {
			return CradlePlayerData.IronBody.BLOODFORGED;
		}
		if (mainHand.is(CradleItems.STEELBORN_CRYSTAL) || offHand.is(CradleItems.STEELBORN_CRYSTAL)) {
			return CradlePlayerData.IronBody.STEELBORN;
		}
		if (mainHand.is(CradleItems.RAINDROP_CRYSTAL) || offHand.is(CradleItems.RAINDROP_CRYSTAL)) {
			return CradlePlayerData.IronBody.RAINDROP;
		}
		return CradlePlayerData.IronBody.NONE;
	}

	/**
	 * Consumes one crystal item from whichever hand holds it.
	 */
	private static void consumeCrystalFromHands(ServerPlayer player, CradlePlayerData.IronBody bodyType) {
		Item crystal = switch (bodyType) {
			case BLOODFORGED -> CradleItems.BLOODFORGED_CRYSTAL;
			case STEELBORN -> CradleItems.STEELBORN_CRYSTAL;
			case RAINDROP -> CradleItems.RAINDROP_CRYSTAL;
			default -> null;
		};
		if (crystal == null) return;

		if (player.getMainHandItem().is(crystal)) {
			player.getMainHandItem().shrink(1);
		} else if (player.getOffhandItem().is(crystal)) {
			player.getOffhandItem().shrink(1);
		}
	}

	/**
	 * Returns a lore-appropriate narrative string for the given breakthrough stage.
	 */
	public static String getBreakthroughNarrative(CradlePlayerData.AdvancementStage stage) {
		return switch (stage) {
			case COPPER -> "Your channels grow stronger. Aura flows through you like a river.";
			case IRON -> "Your body is forged anew. Flesh and bone are tempered by madra.";
			case JADE -> "Your spirit awakens. You can sense the vital aura of all living things.";
			case LOW_GOLD -> "Gold shines within you. Your madra has transformed.";
			case HIGH_GOLD -> "Your madra grows denser, more potent. The boundary of Gold deepens.";
			case TRUEGOLD -> "Your spirit is perfected at the Gold stage. You stand at the threshold of power.";
			case UNDERLORD -> "You look within and find your truth. The soul ignites with revelation.";
			case OVERLORD -> "You impose your will upon your madra. Your power obeys your intent alone.";
			case ARCHLORD -> "Your authority extends beyond yourself. The world bends to your command.";
			case SAGE -> "You touch the Way and speak with the voice of creation. Icons bow before your will.";
			case HERALD -> "Your spirit and body merge as one. You are no longer bound by mortal form.";
			case MONARCH -> "You stand at the peak of Cradle. None can challenge your dominion.";
			default -> "";
		};
	}

	/**
	 * Returns a Remnant-specific narrative when advancing to Gold via absorption.
	 */
	public static String getRemnantBreakthroughNarrative(CradlePlayerData.Goldsign goldsign) {
		return switch (goldsign) {
			case BLACK_FLAME_EYES -> "The Remnant's fire floods your spirit. Dark embers ignite in your eyes — your Goldsign.";
			case SWORD_ARMS -> "The Remnant's edge merges with your soul. Blade-like lines trace your arms — your Goldsign.";
			case SPEAR_LIGHT -> "The Remnant's light flows into you. A golden glow surrounds your hands — your Goldsign.";
			case CRACKLING_SKIN -> "The Remnant's storm courses through you. Energy crackles across your skin — your Goldsign.";
			case PALE_AURA -> "The Remnant's purity merges with your spirit. A pale shimmer surrounds you — your Goldsign.";
			default -> "The Remnant dissolves into your spirit. Gold shines within you.";
		};
	}

	/**
	 * Returns the level needed for the next breakthrough, or -1 if at max stage,
	 * or -2 if the player needs to make a Sage/Herald choice first.
	 * Now takes CradlePlayerData instead of AdvancementStage because it needs
	 * the hasSage/hasHerald flags for branching.
	 */
	public static int getNextBreakthroughLevel(CradlePlayerData data) {
		CradlePlayerData.AdvancementStage current = data.getAdvancementStage();

		// At Archlord with no choice made = needs Sage/Herald selection
		if (current == CradlePlayerData.AdvancementStage.ARCHLORD
				&& !data.hasSage() && !data.hasHerald()) {
			return -2; // Signal: needs choice
		}

		CradlePlayerData.AdvancementStage nextStage = getNextStage(data);
		if (nextStage == null) {
			return -1; // Max stage
		}
		return getLevelForStage(nextStage);
	}
}

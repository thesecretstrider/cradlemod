package com.cradle.mod.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.List;

/**
 * AI goal that moves an NPC between a list of waypoints.
 * At each waypoint the mob dwells for a configurable number of ticks
 * before moving to the next one in a loop.
 */
public class WaypointWanderGoal extends Goal {
	private final PathfinderMob mob;
	private final List<WaypointEntry> waypoints;
	private final double speed;
	private int currentIndex = 0;
	private int dwellTicksRemaining = 0;

	public record WaypointEntry(BlockPos pos, int dwellTicks) {}

	public WaypointWanderGoal(PathfinderMob mob, List<WaypointEntry> waypoints, double speed) {
		this.mob = mob;
		this.waypoints = waypoints;
		this.speed = speed;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		return !waypoints.isEmpty();
	}

	@Override
	public void tick() {
		if (waypoints.isEmpty()) return;

		WaypointEntry target = waypoints.get(currentIndex);
		double distSq = mob.blockPosition().distSqr(target.pos());

		if (distSq < 4.0) { // within 2 blocks
			if (dwellTicksRemaining > 0) {
				dwellTicksRemaining--;
			} else {
				currentIndex = (currentIndex + 1) % waypoints.size();
				dwellTicksRemaining = waypoints.get(currentIndex).dwellTicks();
			}
		} else {
			mob.getNavigation().moveTo(
					target.pos().getX() + 0.5,
					target.pos().getY(),
					target.pos().getZ() + 0.5,
					speed);
		}
	}

	@Override
	public boolean canContinueToUse() {
		return !waypoints.isEmpty();
	}
}

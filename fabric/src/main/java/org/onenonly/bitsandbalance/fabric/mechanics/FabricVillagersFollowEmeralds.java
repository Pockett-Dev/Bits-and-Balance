package org.onenonly.bitsandbalance.fabric.mechanics;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.mixin.MobGoalSelectorAccessor;

import java.util.EnumSet;

/**
 * Fabric port: Villagers Follow Emeralds
 * Villagers are tempted by emeralds and emerald blocks.
 */
public final class FabricVillagersFollowEmeralds {
    private FabricVillagersFollowEmeralds() {
    }

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!FabricMechanicsConfig.enableVillagersFollowEmeralds) return;
            if (!(world instanceof ServerLevel)) return;
            if (entity instanceof Villager villager) {
                enhanceVillager(villager);
            }
        });
    }

    private static void enhanceVillager(Villager villager) {
        try {
            GoalSelector goalSelector = ((MobGoalSelectorAccessor) (Object) villager).bitsandbalance$getGoalSelector();

            if (hasEmeraldGoal(goalSelector)) {
                return;
            }

            EmeraldFollowGoal emeraldGoal = new EmeraldFollowGoal(villager);
            // High priority so villagers strongly prefer following emeralds while the condition holds.
            // (0 is typically reserved for float/swim in vanilla.)
            goalSelector.addGoal(1, emeraldGoal);
        } catch (Throwable ignored) {
        }
    }

    private static boolean hasEmeraldGoal(GoalSelector goalSelector) {
        try {
            for (var wrapped : goalSelector.getAvailableGoals()) {
                if (wrapped != null && wrapped.getGoal() instanceof EmeraldFollowGoal) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Custom Goal that makes villagers follow players holding emeralds.
     */
    private static class EmeraldFollowGoal extends Goal {
        private final Villager villager;
        private Player targetPlayer;
        private int retryCooldown;
        private int updatePathCounter;
        private static final double FOLLOW_RANGE = 10.0D;
        private static final double MAX_FOLLOW_RANGE = 12.0D;
        private static final double STOP_DISTANCE = 2.0D;
        private static final double STOP_DISTANCE_SQ = STOP_DISTANCE * STOP_DISTANCE;
        private static final double MAX_FOLLOW_RANGE_SQ = MAX_FOLLOW_RANGE * MAX_FOLLOW_RANGE;
        private static final int PATH_RECALC_TICKS = 4;
        private static final int RETRY_TICKS_WHEN_IDLE = 5;
        private static final int RETRY_TICKS_WHEN_BUSY = 10;

        public EmeraldFollowGoal(Villager villager) {
            this.villager = villager;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!FabricMechanicsConfig.enableVillagersFollowEmeralds) {
                return false;
            }
            if (this.retryCooldown > 0) {
                --this.retryCooldown;
                return false;
            }

            if (isTargetStillValid(this.targetPlayer)) {
                return true;
            }

            if (isVillagerBusy(this.villager)) {
                this.retryCooldown = this.adjustedTickDelay(RETRY_TICKS_WHEN_BUSY);
                return false;
            }

            this.targetPlayer = findNearestPlayerWithEmeralds();
            if (this.targetPlayer == null) {
                this.retryCooldown = this.adjustedTickDelay(RETRY_TICKS_WHEN_IDLE);
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (!FabricMechanicsConfig.enableVillagersFollowEmeralds) {
                return false;
            }
            return isTargetStillValid(this.targetPlayer);
        }

        @Override
        public void start() {
            this.retryCooldown = 0;
            this.updatePathCounter = 0;
            suppressCompetingBrainActivity(this.targetPlayer);
        }

        @Override
        public void tick() {
            if (this.targetPlayer != null) {
                this.villager.getLookControl().setLookAt(
                    this.targetPlayer,
                    10.0F,
                    this.villager.getMaxHeadXRot()
                );

                double distance = this.villager.distanceToSqr(this.targetPlayer);
                if (distance <= STOP_DISTANCE_SQ) {
                    suppressCompetingBrainActivity(this.targetPlayer);
                    this.villager.getNavigation().stop();
                    this.updatePathCounter = 0;
                    return;
                }

                suppressCompetingBrainActivity(this.targetPlayer);
                boolean shouldRefreshPath = this.updatePathCounter <= 0 || this.villager.getNavigation().isDone();
                if (shouldRefreshPath) {
                    this.updatePathCounter = PATH_RECALC_TICKS;
                    this.villager.getNavigation().moveTo(
                        this.targetPlayer,
                        FabricMechanicsConfig.villagerEmeraldFollowSpeed
                    );
                } else {
                    --this.updatePathCounter;
                }
            }
        }

        @Override
        public void stop() {
            this.retryCooldown = 0;
            this.updatePathCounter = 0;
            try {
                this.villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            } catch (Throwable ignored) {
            }
            this.villager.getNavigation().stop();
        }

        private boolean isTargetStillValid(Player player) {
            if (player == null || !player.isAlive()) {
                return false;
            }
            if (isVillagerBusy(this.villager)) {
                return false;
            }
            if (!isHoldingEmerald(player)) {
                return false;
            }
            return this.villager.distanceToSqr(player) <= MAX_FOLLOW_RANGE_SQ;
        }

        private void suppressCompetingBrainActivity(Player player) {
            try {
                var brain = this.villager.getBrain();
                brain.eraseMemory(MemoryModuleType.WALK_TARGET);
                brain.eraseMemory(MemoryModuleType.PATH);
                brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                if (player != null) {
                    brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));
                }
                brain.setActiveActivityIfPossible(Activity.IDLE);
            } catch (Throwable ignored) {
            }
        }

        private Player findNearestPlayerWithEmeralds() {
            try {
                AABB box = this.villager.getBoundingBox().inflate(FOLLOW_RANGE);
                var candidates = this.villager.level().getEntitiesOfClass(Player.class, box,
                        p -> p != null && p.isAlive() && !p.isSpectator() && isHoldingEmerald(p));
                Player best = null;
                double bestDist = Double.MAX_VALUE;
                for (Player p : candidates) {
                    double d = this.villager.distanceToSqr(p);
                    if (d < bestDist) {
                        bestDist = d;
                        best = p;
                    }
                }
                return best;
            } catch (Throwable ignored) {
                return null;
            }
        }

        private boolean isHoldingEmerald(Player player) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
             return mainHand.getItem() == Items.EMERALD || mainHand.getItem() == Items.EMERALD_BLOCK ||
                 offHand.getItem() == Items.EMERALD || offHand.getItem() == Items.EMERALD_BLOCK;
        }

        private static boolean isVillagerBusy(Villager villager) {
            try {
                if (villager.getTradingPlayer() != null) return true;
            } catch (Throwable ignored) {
            }
            try {
                if (villager.isSleeping()) return true;
            } catch (Throwable ignored) {
            }
            try {
                if (villager.isPassenger()) return true;
            } catch (Throwable ignored) {
            }
            return false;
        }
    }
}

package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

import java.util.*;
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class MechanicsEvents {

    private static boolean bitsandbalance$hasEmeraldFollowGoal(AbstractVillager villager) {
        try {
            for (WrappedGoal wrappedGoal : villager.goalSelector.getAvailableGoals()) {
                if (wrappedGoal != null && wrappedGoal.getGoal() instanceof VillagerEmeraldFollowGoal) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var entity = event.getEntity();
        if (!(entity instanceof ServerPlayer)) return;
        ServerPlayer sp = (ServerPlayer) entity;

        boolean crawlingEnabled = Config.enableCrawlingMechanic;

        // Read crawl desire
        boolean wantCrawl;
        try { wantCrawl = sp.getPersistentData().getBoolean("bitsandbalance_crawling").orElse(false); } catch (Throwable e) { wantCrawl = false; }

        // Apply crawl (forced swimming) and clear crouch while active
        if (crawlingEnabled && wantCrawl) {
            try {
                if (sp.getForcedPose() != Pose.SWIMMING) sp.setForcedPose(Pose.SWIMMING);
            } catch (Throwable ignored) {}
            try { sp.setShiftKeyDown(false); } catch (Throwable ignored) {}
        } else {
            try {
                if (sp.getForcedPose() == Pose.SWIMMING) sp.setForcedPose(null);
            } catch (Throwable ignored) {}
        }


        // All other mechanics functionality has been moved to dedicated modules
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity ent = event.getEntity();
        if (ent.level().isClientSide()) return;

        // Villagers Follow Emeralds
        if (!Config.enableVillagersFollowEmeralds) return;
        if (!(ent instanceof AbstractVillager villager)) return;
        try {
            // Defer goal injection to the next server tick to avoid being overwritten by vanilla goal registration on natural spawns
            ServerLevel sl = (ServerLevel) event.getLevel();
            sl.getServer().execute(() -> {
                try {
                    if (!villager.isAlive()) return;
                    if (bitsandbalance$hasEmeraldFollowGoal(villager)) return;
                    villager.goalSelector.addGoal(1, new VillagerEmeraldFollowGoal(villager, Config.villagerEmeraldFollowSpeed));
                } catch (Throwable ignored2) {}
            });
        } catch (Throwable ignored) {
        }
    }

    // Custom goal for villagers to follow players holding emeralds
    private static class VillagerEmeraldFollowGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final AbstractVillager villager;
        private final double speedModifier;
        private Player targetPlayer;
        private int retryCooldown = 0;
        private int pathUpdateCooldown = 0;
        private static final int RETRY_TICKS_WHEN_IDLE = 5;
        private static final int RETRY_TICKS_WHEN_BUSY = 10;
        private static final double FOLLOW_RANGE = 10.0D;
        private static final double MAX_FOLLOW_RANGE = 12.0D;
        private static final double STOP_DISTANCE = 2.0D;
        private static final double STOP_DISTANCE_SQ = STOP_DISTANCE * STOP_DISTANCE;
        private static final double MAX_FOLLOW_RANGE_SQ = MAX_FOLLOW_RANGE * MAX_FOLLOW_RANGE;
        private static final int PATH_RECALC_TICKS = 4;

        VillagerEmeraldFollowGoal(AbstractVillager villager, double speedModifier) {
            this.villager = villager;
            this.speedModifier = speedModifier;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!Config.enableVillagersFollowEmeralds) return false;
            if (retryCooldown > 0) {
                retryCooldown--;
                return false;
            }

            if (isTargetStillValid(targetPlayer)) return true;

            if (villager.isBaby()) return false;
            if (!(villager.level() instanceof ServerLevel)) return false;

            if (isVillagerBusy(villager)) {
                retryCooldown = this.adjustedTickDelay(RETRY_TICKS_WHEN_BUSY);
                return false;
            }

            Player nearest = findNearestPlayerWithEmeralds();
            if (nearest == null) {
                retryCooldown = this.adjustedTickDelay(RETRY_TICKS_WHEN_IDLE);
                return false;
            }
            targetPlayer = nearest;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (!Config.enableVillagersFollowEmeralds) return false;
            return isTargetStillValid(targetPlayer);
        }

        @Override
        public void start() {
            retryCooldown = 0;
            pathUpdateCooldown = 0;
            suppressCompetingBrainActivity(targetPlayer);
            if (Config.villagerEmeraldShowParticles && villager.level() instanceof ServerLevel sl2) {
                // Display happy villager particles
                sl2.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        villager.getX(), villager.getY() + villager.getBbHeight() * 0.6, villager.getZ(),
                        Config.villagerEmeraldParticlesCount, 0.4, 0.4, 0.4, 0.01);
            }
        }

        @Override
        public void tick() {
            if (targetPlayer == null || !targetPlayer.isAlive()) return;

            villager.getLookControl().setLookAt(targetPlayer, 10.0F, (float) villager.getMaxHeadXRot());

            double distanceSq = villager.distanceToSqr(targetPlayer);
            if (distanceSq <= STOP_DISTANCE_SQ) {
                suppressCompetingBrainActivity(targetPlayer);
                villager.getNavigation().stop();
                pathUpdateCooldown = 0;
                return;
            }

            suppressCompetingBrainActivity(targetPlayer);
            boolean shouldRefreshPath = pathUpdateCooldown <= 0 || villager.getNavigation().isDone();
            if (shouldRefreshPath) {
                pathUpdateCooldown = PATH_RECALC_TICKS;
                villager.getNavigation().moveTo(targetPlayer, speedModifier);
            } else {
                pathUpdateCooldown--;
            }
        }

        @Override
        public void stop() {
            pathUpdateCooldown = 0;
            retryCooldown = 0;
            try { villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET); } catch (Throwable ignored) {}
            try { villager.getNavigation().stop(); } catch (Throwable ignored) {}
        }

        private boolean isTargetStillValid(Player player) {
            if (player == null || !player.isAlive()) return false;
            if (!(villager.level() instanceof ServerLevel)) return false;
            if (isVillagerBusy(villager)) return false;
            if (!isHoldingEmeralds(player)) return false;
            return villager.distanceToSqr(player) <= MAX_FOLLOW_RANGE_SQ;
        }

        private void suppressCompetingBrainActivity(Player player) {
            try {
                var brain = villager.getBrain();
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
                AABB box = villager.getBoundingBox().inflate(FOLLOW_RANGE);
                List<Player> players = villager.level().getEntitiesOfClass(Player.class, box,
                        p -> p != null && p.isAlive() && !p.isSpectator() && isHoldingEmeralds(p));
                Player best = null;
                double bestDist = Double.MAX_VALUE;
                for (Player p : players) {
                    double d = villager.distanceToSqr(p);
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

        private static boolean isHoldingEmeralds(Player player) {
            try {
                ItemStack main = player.getMainHandItem();
                ItemStack off = player.getOffhandItem();
                return main.getItem() == Items.EMERALD || main.getItem() == Items.EMERALD_BLOCK
                        || off.getItem() == Items.EMERALD || off.getItem() == Items.EMERALD_BLOCK;
            } catch (Throwable ignored) {
                return false;
            }
        }

        private static boolean isVillagerBusy(AbstractVillager villager) {
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

    // All other functionality has been moved to dedicated modules

}
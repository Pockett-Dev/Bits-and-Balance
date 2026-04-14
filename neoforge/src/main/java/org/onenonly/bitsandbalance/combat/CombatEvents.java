package org.onenonly.bitsandbalance.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.onenonly.bitsandbalance.BitsAndBalance;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.onenonly.bitsandbalance.Config;
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class CombatEvents {

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (!Config.enableMaintainExperience) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (shouldKeepInventory(player)) return;

        int total = computeTotalExperience(player);
        int percent = Math.max(0, Math.min(100, Config.maintainExperienceLossPercent));
        int deleted = (int) Math.floor(total * (percent / 100.0));
        if (deleted < 0) deleted = 0;
        int toDrop = Math.max(0, total - deleted);
        event.setDroppedExperience(toDrop);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!Config.enableMaintainExperience) return;
        if (!event.isWasDeath()) return;
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (shouldKeepInventory(player)) return;

        // With new behavior, we delete the configured percentage and drop the rest; nothing is retained on respawn.
        // Reset player XP to zero explicitly to avoid any vanilla carryover.
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        player.totalExperience = 0;
    }

    private static boolean shouldKeepInventory(Player player) {
        try {
            if (!(player.level() instanceof ServerLevel serverLevel)) return false;
            return serverLevel.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int computeTotalExperience(Player player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;
        int xpToNext = player.getXpNeededForNextLevel();
        int totalToLevel = xpFromLevel(level);
        int progressPoints = Math.max(0, Math.round(progress * xpToNext));
        return Math.max(0, totalToLevel + progressPoints);
    }

    private static int xpFromLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) Math.floor(2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) Math.floor(4.5 * level * level - 162.5 * level + 2220);
        }
    }

    // Second Chance cooldown handling on respawn
    @SubscribeEvent
    public static void onPlayerCloneSecondChance(PlayerEvent.Clone event) {
        if (!Config.enableSecondChance) return;
        if (!event.isWasDeath()) return;
        Player original = event.getOriginal();
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        final String key = "rebalance_second_chance_next_allowed";
        if (Config.secondChanceResetOnRespawn) {
            // Explicitly clear any stored cooldown on the new player
            player.getPersistentData().remove(key);
        } else {
            // Persist cooldown across death by copying from original to new entity
            long val = original.getPersistentData().getLong(key).orElse(0L);
            if (val > 0L) {
                player.getPersistentData().putLong(key, val);
            } else {
                // ensure no stale value remains
                player.getPersistentData().remove(key);
            }
        }
    }
}

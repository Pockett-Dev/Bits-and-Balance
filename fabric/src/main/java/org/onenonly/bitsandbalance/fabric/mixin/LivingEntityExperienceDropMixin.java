package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricCombatConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class LivingEntityExperienceDropMixin {

    @ModifyArg(
            method = "dropExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"
            ),
            index = 2,
            require = 0
    )
    private int bitsandbalance$modifyDroppedExperience(int original) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!FabricCombatConfig.enableMaintainExperience) {
            return original;
        }

        if (!(self instanceof Player player)) {
            return original;
        }

        if (shouldKeepInventory(player)) {
            return original;
        }

        int total = computeTotalExperience(player);
        int percent = clampPercent(FabricCombatConfig.maintainExperienceLossPercent);
        int deleted = (int) Math.floor(total * (percent / 100.0));
        if (deleted < 0) deleted = 0;
        return Math.max(0, total - deleted);
    }

    private static boolean shouldKeepInventory(Player player) {
        try {
            if (!(player.level() instanceof ServerLevel serverLevel)) return false;
            return serverLevel.getGameRules().get(GameRules.KEEP_INVENTORY);
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

    private static int clampPercent(int value) {
        if (value < 0) return 0;
        if (value > 100) return 100;
        return value;
    }
}

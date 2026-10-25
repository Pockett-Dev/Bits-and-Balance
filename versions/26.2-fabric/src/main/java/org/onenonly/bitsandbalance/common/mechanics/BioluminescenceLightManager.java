package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.common.client.BioluminescenceClientLightManager;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;

public final class BioluminescenceLightManager {
    private BioluminescenceLightManager() {
    }

    public static void tickClient() {
        BioluminescenceClientLightManager.tickClient();
    }

    public static void observeEntity(Entity entity) {
        BioluminescenceClientLightManager.observeEntity(entity);
    }

    public static int getLightmapWithDynamicLight(BlockPos pos, int lightmap) {
        return BioluminescenceClientLightManager.getLightmapWithDynamicLight(pos, lightmap);
    }

    public static int getLightmapWithDynamicLight(BlockAndTintGetter level, BlockPos pos, int lightmap) {
        return BioluminescenceClientLightManager.getLightmapWithDynamicLight(level, pos, lightmap);
    }

    public static int getLightmapWithDynamicLight(double sampleX, double sampleY, double sampleZ, int lightmap) {
        return BioluminescenceClientLightManager.getLightmapWithDynamicLight(sampleX, sampleY, sampleZ, lightmap);
    }

    public static int getLightmapWithDynamicLight(BlockAndTintGetter level, double sampleX, double sampleY, double sampleZ, int lightmap) {
        return BioluminescenceClientLightManager.getLightmapWithDynamicLight(level, sampleX, sampleY, sampleZ, lightmap);
    }

    public static int getLightmapWithDynamicLight(BlockAndTintGetter level, double sampleX, double sampleY, double sampleZ, Direction faceDirection, int lightmap) {
        return BioluminescenceClientLightManager.getLightmapWithDynamicLight(level, sampleX, sampleY, sampleZ, faceDirection, lightmap);
    }

    public static double getDynamicLightLevel(BlockPos pos) {
        return BioluminescenceClientLightManager.getDynamicLightLevel(pos);
    }

    public static double getDynamicLightLevel(double sampleX, double sampleY, double sampleZ) {
        return BioluminescenceClientLightManager.getDynamicLightLevel(sampleX, sampleY, sampleZ);
    }

    public static int getEntityLuminance(Entity entity) {
        return BioluminescenceClientLightManager.getEntityLuminance(entity);
    }

    public static int getEntityLuminance(LivingEntity entity) {
        return BioluminescenceClientLightManager.getEntityLuminance(entity);
    }

    public static double getEntityDynamicLuminance(LivingEntity entity) {
        return BioluminescenceClientLightManager.getEntityDynamicLuminance(entity);
    }

    public static int getBioluminescenceRemainingTicks(LivingEntity entity) {
        MobEffectInstance bioluminescence = bitsandbalance$getBioluminescenceInstance(entity);
        if (bioluminescence != null) {
            return bioluminescence.getDuration();
        }

        return entity.level().isClientSide()
                ? BioluminescenceClientLightManager.getSyncedRemainingDuration(entity.getId())
                : 0;
    }

    public static int getBioluminescenceAmplifier(LivingEntity entity) {
        MobEffectInstance bioluminescence = bitsandbalance$getBioluminescenceInstance(entity);
        if (bioluminescence != null) {
            return bioluminescence.getAmplifier();
        }

        return entity.level().isClientSide()
                ? BioluminescenceClientLightManager.getSyncedAmplifier(entity.getId())
                : 0;
    }

    public static void syncClientBioluminescence(int entityId, int remainingTicks, int amplifier) {
        BioluminescenceClientLightManager.syncClientBioluminescence(entityId, remainingTicks, amplifier);
    }

    static MobEffectInstance bitsandbalance$getBioluminescenceInstance(LivingEntity entity) {
        return entity.getEffect(ModGlowGoo.bioluminescence());
    }
}

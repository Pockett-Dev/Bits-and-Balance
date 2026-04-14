package org.onenonly.bitsandbalance.fabric.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricLeashedTeleport;

public final class ResurfacingEffect extends MobEffect {
    public ResurfacingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x008080);
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!FabricPotionConfig.enableResurfacingPotion) {
            return false;
        }

        teleportToSurface(level, entity);
        return true;
    }

    private static void teleportToSurface(ServerLevel level, LivingEntity entity) {
        BlockPos currentPos = entity.blockPosition();
        spawnTeleportEffects(level, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 8, 0.0D);

        BlockPos surfacePos = findSurfacePosition(level, currentPos);
        double targetX = surfacePos.getX() + 0.5D;
        double targetY = surfacePos.getY() + 1.0D;
        double targetZ = surfacePos.getZ() + 0.5D;

        if (entity instanceof ServerPlayer serverPlayer && FabricMechanicsConfig.enableLeashedTeleport) {
            FabricLeashedTeleport.capturePreTeleport(serverPlayer);
            entity.teleportTo(targetX, targetY, targetZ);
            FabricLeashedTeleport.processPostTeleport(serverPlayer);
        } else {
            entity.teleportTo(targetX, targetY, targetZ);
        }

        spawnTeleportEffects(level, targetX, targetY, targetZ, 40, 0.2D);
    }

    private static BlockPos findSurfacePosition(ServerLevel level, BlockPos startPos) {
        int x = startPos.getX();
        int z = startPos.getZ();

        for (int y = level.getMaxY() - 1; y >= level.getMinY(); y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockState blockState = level.getBlockState(checkPos);
            BlockState aboveState = level.getBlockState(checkPos.above());

            if (!blockState.isAir() && blockState.isSolidRender() && (aboveState.isAir() || !aboveState.isSolidRender())) {
                return checkPos;
            }
        }

        return new BlockPos(x, 64, z);
    }

    private static void spawnTeleportEffects(ServerLevel level, double x, double y, double z, int count, double speed) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, count, 0.8D, 0.8D, 0.8D, speed);
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.AMBIENT, 0.5F, 1.0F);
    }
}

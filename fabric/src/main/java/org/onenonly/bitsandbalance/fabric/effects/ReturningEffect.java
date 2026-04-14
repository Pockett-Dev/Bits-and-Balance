package org.onenonly.bitsandbalance.fabric.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;

/**
 * Instant effect that teleports a player to their last death location (same dimension only).
 */
public final class ReturningEffect extends MobEffect {
    private static final int MAX_ATTEMPTS = 50;
    private static final int TELEPORT_POST_EFFECT_TICKS = 40;

    public ReturningEffect() {
        super(MobEffectCategory.NEUTRAL, 0x034150);
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!FabricPotionConfig.enableReturningPotion) {
            return true;
        }

        if (!(entity instanceof Player player)) {
            return true;
        }

        GlobalPos death = player.getLastDeathLocation().orElse(null);
        if (death == null) return true;
        if (!death.dimension().equals(level.dimension())) return true;

        BlockPos deathPos = death.pos();
        BlockPos target = findSafeNear(level, deathPos);
        if (target == null) {
            // Fall back to the surface at death X/Z.
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, deathPos.getX(), deathPos.getZ());
            target = new BlockPos(deathPos.getX(), y, deathPos.getZ());
        }

        if (player.getVehicle() != null) {
            player.stopRiding();
        }

        ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;
        double sourceX = player.getX();
            double sourceY = player.getY() + (player.getBbHeight() * 0.5D);
        double sourceZ = player.getZ();

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false, false));
        spawnReturningParticles(level, serverPlayer, sourceX, sourceY, sourceZ);
        playDepartureSounds(level, sourceX, sourceY, sourceZ);

        finishTeleport(player, target.immutable());
        return true;
    }

    private static void finishTeleport(Player player, BlockPos target) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        player.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TELEPORT_POST_EFFECT_TICKS, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, TELEPORT_POST_EFFECT_TICKS, 0, false, true, true));
        player.removeEffect(MobEffects.INVISIBILITY);

        ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;
    double destX = player.getX();
    double destY = player.getY() + (player.getBbHeight() * 0.5D);
    double destZ = player.getZ();

        spawnReturningParticles(level, serverPlayer, destX, destY, destZ);
        playArrivalSounds(level, destX, destY, destZ);
    }

    private static BlockPos findSafeNear(ServerLevel level, BlockPos origin) {
        if (isSafe(level, origin)) return origin;

        for (int radius = 1; radius <= 10; radius++) {
            for (int attempt = 0; attempt < Math.max(1, MAX_ATTEMPTS / 10); attempt++) {
                int x = origin.getX() + (level.getRandom().nextInt(radius * 2 + 1) - radius);
                int z = origin.getZ() + (level.getRandom().nextInt(radius * 2 + 1) - radius);

                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos candidate = new BlockPos(x, surfaceY, z);
                if (isSafe(level, candidate)) return candidate;

                // Also try around the original Y (in caves), not just surface.
                BlockPos candidateAtY = new BlockPos(x, origin.getY(), z);
                if (isSafe(level, candidateAtY)) return candidateAtY;
            }
        }

        return null;
    }

    private static boolean isSafe(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());

        boolean feetClear = feet.getCollisionShape(level, pos).isEmpty();
        boolean headClear = head.getCollisionShape(level, pos.above()).isEmpty();
        boolean groundSolid = !ground.getCollisionShape(level, pos.below()).isEmpty();

        return feetClear && headClear && groundSolid;
    }

    private static void spawnReturningParticles(ServerLevel level, ServerPlayer focusPlayer, double x, double y, double z) {
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 34, 0.08, 0.12, 0.08, 0.05);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 10, 0.05, 0.08, 0.05, 0.03);

        if (focusPlayer != null) {
            sendForcedParticles(level, focusPlayer, x, y, z);
        }

        for (ServerPlayer nearbyPlayer : level.players()) {
            if (focusPlayer != null && nearbyPlayer == focusPlayer) {
                continue;
            }
            if (nearbyPlayer.distanceToSqr(x, y, z) <= 4096.0D) {
                sendForcedParticles(level, nearbyPlayer, x, y, z);
            }
        }
    }

    private static void sendForcedParticles(ServerLevel level, ServerPlayer player, double x, double y, double z) {
        level.sendParticles(player, ParticleTypes.SOUL, true, true, x, y, z, 34, 0.08, 0.12, 0.08, 0.05);
        level.sendParticles(player, ParticleTypes.SOUL_FIRE_FLAME, true, true, x, y, z, 10, 0.05, 0.08, 0.05, 0.03);
    }

    private static void playDepartureSounds(ServerLevel level, double x, double y, double z) {
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.AMBIENT, 0.5F, 1.0F);
        level.playSound(null, x, y, z, SoundEvents.SOUL_SAND_STEP, SoundSource.AMBIENT, 0.35F, 0.85F);
    }

    private static void playArrivalSounds(ServerLevel level, double x, double y, double z) {
        level.playSound(null, x, y, z, SoundEvents.SOUL_SAND_STEP, SoundSource.AMBIENT, 0.35F, 0.85F);
        level.playSound(null, x, y, z, SoundEvents.AMBIENT_CAVE, SoundSource.AMBIENT, 0.4F, 0.8F);
    }

}

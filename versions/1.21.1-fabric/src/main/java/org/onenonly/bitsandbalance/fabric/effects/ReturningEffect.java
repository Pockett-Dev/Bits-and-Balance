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
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.onenonly.bitsandbalance.common.mechanics.LeashedTeleportFollowers;
import org.onenonly.bitsandbalance.common.mechanics.ReturningTeleportSafety;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;

/**
 * Instant effect that teleports a player to their last death location.
 */
public final class ReturningEffect extends MobEffect {
    private static final int TELEPORT_POST_EFFECT_TICKS = 30;

    public ReturningEffect() {
        super(MobEffectCategory.NEUTRAL, 0x034150);
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }
        if (!FabricPotionConfig.enableReturningPotion) {
            return true;
        }

        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }

        GlobalPos death = player.getLastDeathLocation().orElse(null);
        if (death == null) return true;

        ServerLevel targetLevel = level.getServer().getLevel(death.dimension());
        if (targetLevel == null) return true;

        BlockPos deathPos = death.pos();
        BlockPos target = ReturningTeleportSafety.findSafeDestination(targetLevel, player, deathPos);
        if (target == null) {
            return true;
        }

        if (player.getVehicle() != null) {
            player.stopRiding();
        }

        double sourceX = player.getX();
        double sourceY = player.getY() + (player.getBbHeight() * 0.5D);
        double sourceZ = player.getZ();

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false, false));
        spawnReturningParticles(level, player, sourceX, sourceY, sourceZ);
        playDepartureSounds(level, sourceX, sourceY, sourceZ);

        finishTeleport(player, targetLevel, target.immutable());
        return true;
    }

    private static void finishTeleport(ServerPlayer player, ServerLevel targetLevel, BlockPos target) {
        List<UUID> followerIds = LeashedTeleportFollowers.captureLeashedFollowerIds(player);
        boolean teleported = player.teleportTo(
            targetLevel,
            target.getX() + 0.5D,
            target.getY(),
            target.getZ() + 0.5D,
            Set.of(),
            player.getYRot(),
            player.getXRot()
        );
        if (!teleported) {
            player.removeEffect(MobEffects.INVISIBILITY);
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TELEPORT_POST_EFFECT_TICKS, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, TELEPORT_POST_EFFECT_TICKS, 0, false, true, true));
        player.removeEffect(MobEffects.INVISIBILITY);

        double destX = player.getX();
        double destY = player.getY() + (player.getBbHeight() * 0.5D);
        double destZ = player.getZ();

        spawnReturningParticles(targetLevel, player, destX, destY, destZ);
        playArrivalSounds(targetLevel, destX, destY, destZ);
        if (!followerIds.isEmpty()) {
            LeashedTeleportFollowers.teleportCapturedFollowers(
                player,
                followerIds,
                new Vec3(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D)
            );
        }
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
        level.sendParticles(player, ParticleTypes.SOUL, true, x, y, z, 34, 0.08, 0.12, 0.08, 0.05);
        level.sendParticles(player, ParticleTypes.SOUL_FIRE_FLAME, true, x, y, z, 10, 0.05, 0.08, 0.05, 0.03);
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

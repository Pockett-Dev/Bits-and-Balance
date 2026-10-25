package org.onenonly.bitsandbalance.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nonnull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.ReturningTeleportSafety;
import org.onenonly.bitsandbalance.mechanics.modules.LeashedTeleportModule;
/**
 * Returning effect - teleports the affected player to their latest death location.
 * Works as an instant effect like Instant Health.
 * Color: #034150 (dark teal)
 */
public class ReturningEffect extends MobEffect {
    
    private static final int TELEPORT_POST_EFFECT_TICKS = 30;
    
    public ReturningEffect() {
        // Dark teal color (RGB: 3, 65, 80 -> hex: 0x034150)
        super(MobEffectCategory.NEUTRAL, 0x034150);
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    @Override
    public boolean applyEffectTick(@Nonnull LivingEntity livingEntity, int amplifier) {
        // This method is called for instant effects
        if (!Config.enableReturningPotion) {
            return false;
        }
        
        // Only apply to players (death locations are player-specific)
        if (!(livingEntity instanceof ServerPlayer player)) {
            return false;
        }
        
        teleportToDeathLocation(player);
        return true;
    }

    private void teleportToDeathLocation(ServerPlayer player) {
        Level level = player.level();
        
        // Get the player's last death location
        GlobalPos deathPos = player.getLastDeathLocation().orElse(null);
        
        if (deathPos == null) {
            // No death location recorded - do nothing
            return;
        }
        
        BlockPos deathBlockPos = deathPos.pos();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerLevel targetLevel = serverLevel.getServer().getLevel(deathPos.dimension());
        if (targetLevel == null) {
            return;
        }
        BlockPos safePos = ReturningTeleportSafety.findSafeDestination(targetLevel, player, deathBlockPos);
        if (safePos == null) {
            return;
        }

        double sourceX = player.getX();
        double sourceY = player.getY() + (player.getBbHeight() * 0.5);
        double sourceZ = player.getZ();

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false, false));
        spawnReturningParticles(serverLevel, player, sourceX, sourceY, sourceZ);
        playTeleportSound(serverLevel, sourceX, sourceY, sourceZ);

        finishTeleport(player, targetLevel, safePos.immutable());
    }

    private void finishTeleport(ServerPlayer player, ServerLevel targetLevel, BlockPos safePos) {
        List<UUID> leashedFollowerIds = Config.enableLeashedTeleport
            ? LeashedTeleportModule.captureLeashedFollowerIds(player)
            : List.of();

        if (player.getVehicle() != null) {
            player.stopRiding();
        }

        boolean teleported = player.teleportTo(
            targetLevel,
            safePos.getX() + 0.5,
            safePos.getY(),
            safePos.getZ() + 0.5,
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
        double destY = player.getY() + (player.getBbHeight() * 0.5);
        double destZ = player.getZ();

        spawnReturningParticles(targetLevel, player, destX, destY, destZ);
        playArrivalSounds(targetLevel, destX, destY, destZ);

        if (Config.enableLeashedTeleport && !leashedFollowerIds.isEmpty()) {
            teleportLeashedFollowers(player, safePos, leashedFollowerIds);
        }
    }
    
    /**
     * Teleports leashed followers to the destination location
     */
    private void teleportLeashedFollowers(ServerPlayer player, BlockPos destination, List<UUID> followerIds) {
        LeashedTeleportModule.teleportCapturedFollowers(
            player,
            followerIds,
            new Vec3(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D)
        );
    }

    private void spawnReturningParticles(ServerLevel serverLevel, ServerPlayer focusPlayer, double x, double y, double z) {
        serverLevel.sendParticles(ParticleTypes.SOUL, x, y, z, 34, 0.08, 0.12, 0.08, 0.05);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 10, 0.05, 0.08, 0.05, 0.03);

        if (focusPlayer != null) {
            sendForcedParticles(serverLevel, focusPlayer, x, y, z);
        }

        for (ServerPlayer nearbyPlayer : serverLevel.players()) {
            if (focusPlayer != null && nearbyPlayer == focusPlayer) {
                continue;
            }
            if (nearbyPlayer.distanceToSqr(x, y, z) <= 4096.0D) {
                sendForcedParticles(serverLevel, nearbyPlayer, x, y, z);
            }
        }
    }

    private void sendForcedParticles(ServerLevel serverLevel, ServerPlayer player, double x, double y, double z) {
        serverLevel.sendParticles(player, ParticleTypes.SOUL, true, x, y, z, 34, 0.08, 0.12, 0.08, 0.05);
        serverLevel.sendParticles(player, ParticleTypes.SOUL_FIRE_FLAME, true, x, y, z, 10, 0.05, 0.08, 0.05, 0.03);
    }


    private void playTeleportSound(Level level, double x, double y, double z) {
        // Only play sounds on the server side
        if (level instanceof ServerLevel serverLevel) {
            // Play Enderman teleport sound at the location
            serverLevel.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, 
                net.minecraft.sounds.SoundSource.AMBIENT, 0.5f, 1.0f);
            // Also play a soul sand sound for the mystical effect
            serverLevel.playSound(null, x, y, z, SoundEvents.SOUL_SAND_STEP, 
                net.minecraft.sounds.SoundSource.AMBIENT, 0.3f, 0.8f);
        }
    }

    private void playArrivalSounds(Level level, double x, double y, double z) {
        // Only play sounds on the server side
        if (level instanceof ServerLevel serverLevel) {
            // Play soul sand sound for the mystical effect (no enderman teleport at arrival)
            serverLevel.playSound(null, x, y, z, SoundEvents.SOUL_SAND_STEP, 
                net.minecraft.sounds.SoundSource.AMBIENT, 0.3f, 0.8f);
            // Play ambient cave sound at the death location for eerie atmosphere
            serverLevel.playSound(null, x, y, z, SoundEvents.AMBIENT_CAVE, 
                net.minecraft.sounds.SoundSource.AMBIENT, 0.4f, 0.8f);
        }
    }
}

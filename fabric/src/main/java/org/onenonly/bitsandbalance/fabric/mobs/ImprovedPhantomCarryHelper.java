package org.onenonly.bitsandbalance.fabric.mobs;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.fabric.config.FabricMobsConfig;

public final class ImprovedPhantomCarryHelper {
    private static final int HIT_DISMOUNT_SLOW_FALL_TICKS = 40;

    private ImprovedPhantomCarryHelper() {
    }

    public static void forceDrop(Player player, Phantom phantom, PhantomPickupTracker tracker, boolean hitTriggered) {
        if (hitTriggered) {
            tracker.bitsandbalance$setPhantomHitDuringCarry(true);
        }

        boolean hitDuringCarry = tracker.bitsandbalance$didHitPhantomDuringCarry();
        tracker.bitsandbalance$clearPhantomPickupState();

        if (player.isPassenger() && player.getVehicle() == phantom) {
            double dropX = player.getX();
            double dropY = Math.min(player.getY(), phantom.getY()) - 0.25D;
            double dropZ = player.getZ();

            player.stopRiding();
            player.teleportTo(dropX, dropY, dropZ);
            player.setDeltaMovement(player.getDeltaMovement().add(0.0D, -0.2D, 0.0D));
        }

        if (hitDuringCarry) {
            applyHitDismountSlowFalling(player);
        }
    }

    public static void finalizeEndedCarry(Player player, PhantomPickupTracker tracker) {
        boolean hitDuringCarry = tracker.bitsandbalance$didHitPhantomDuringCarry();
        tracker.bitsandbalance$clearPhantomPickupState();

        if (hitDuringCarry) {
            applyHitDismountSlowFalling(player);
        }
    }

    private static void applyHitDismountSlowFalling(Player player) {
        if (!FabricMobsConfig.enablePhantomSlowFallingOnHitDismount) return;

        MobEffectInstance current = player.getEffect(MobEffects.SLOW_FALLING);
        int durationTicks = HIT_DISMOUNT_SLOW_FALL_TICKS;
        int amplifier = 0;

        if (current != null) {
            durationTicks = Math.max(durationTicks, current.getDuration());
            amplifier = Math.max(amplifier, current.getAmplifier());
        }

        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, durationTicks, amplifier));
    }
}
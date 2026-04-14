package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.onenonly.bitsandbalance.Config;
/**
 * Alternative approach: Hook into Player's hurt method to detect phantom attacks
 */
@Mixin(Player.class)
public abstract class PlayerPhantomHurtMixin {

    @Inject(method = "hurt", at = @At("HEAD"))
    private void bitsandbalance$onPlayerHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableImprovedPhantoms) return;
        
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;
        
        // Check if the damage source is a phantom
        if (source.getEntity() instanceof Phantom phantom) {
            
            // Apply stacking slowness effect
            if (Config.enablePhantomSlownessStacking) {
                applyStackingSlowness(player);
            }
            
            // Attempt phantom pickup
            if (Config.enablePhantomPickup && !player.isPassenger()) {
                double roll = phantom.getRandom().nextDouble();
                // Pickup chance check
                if (roll < Config.phantomPickupChance) {
                    attemptPhantomPickup(phantom, player);
                }
            }
        }
    }
    
    private static void applyStackingSlowness(Player player) {
        MobEffectInstance currentSlowness = player.getEffect(MobEffects.SLOWNESS);
        
        if (currentSlowness != null) {
            // Player already has slowness - only upgrade the level, keep existing duration
            int currentLevel = currentSlowness.getAmplifier() + 1; // Convert from amplifier to level
            int newLevel = Math.min(currentLevel + 1, Config.phantomSlownessMaxLevel);
            int remainingDuration = currentSlowness.getDuration(); // Keep existing duration
            
            // Only apply if we can actually upgrade the level
            if (newLevel > currentLevel) {
                MobEffectInstance upgradedSlowness = new MobEffectInstance(MobEffects.SLOWNESS, remainingDuration, newLevel - 1);
                player.addEffect(upgradedSlowness);
            } else {
            }
        } else {
            // Player doesn't have slowness - apply new 10-second Slowness I
            int duration = Config.phantomSlownessDuration * 20;
            MobEffectInstance newSlowness = new MobEffectInstance(MobEffects.SLOWNESS, duration, 0); // Level 1 = amplifier 0
            player.addEffect(newSlowness);
        }
    }
    
    private static void attemptPhantomPickup(Phantom phantom, Player player) {
        try {
            // Make the player ride the phantom
            boolean success = player.startRiding(phantom, true, true);
            if (success) {
                // Store pickup time in player's persistent data for dismount delay
                long currentTime = player.level().getGameTime();
                player.getPersistentData().putLong("bitsandbalance_phantom_pickup_time", currentTime);
                
                // Optional: Add some visual/audio feedback here
                // You could play a sound or spawn particles
            } else {
                BitsAndBalance.LOGGER.warn("[PlayerPhantomHurtMixin] Failed to make player ride phantom");
            }
        } catch (Exception e) {
            BitsAndBalance.LOGGER.error("[PlayerPhantomHurtMixin] Exception during phantom pickup: ", e);
        }
    }
}

package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.onenonly.bitsandbalance.Config;
/**
 * Mixin for Phantom to implement improved phantom mechanics:
 * - Phantom pickup/carry mechanics with configurable chance and delay
 * - Speed reduction when carrying players (configurable multiplier)
 * - Tracking player mount/dismount for combat system integration
 */
@Mixin(Phantom.class)
public abstract class PhantomMixin {

    // Try multiple possible method names for phantom attacks
    @Inject(method = "doHurtTarget", at = @At("RETURN"), require = 0)
    private void bitsandbalance$onPhantomDoHurtTarget(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        handlePhantomAttackOnPlayer((Phantom) (Object) this, target, cir.getReturnValue());
    }
    
    @Inject(method = "hurt", at = @At("HEAD"), require = 0)
    private void bitsandbalance$onPhantomHurt(net.minecraft.world.damagesource.DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // This is when the phantom gets hurt, not when it hurts others
    }
    
    private void handlePhantomAttackOnPlayer(Phantom phantom, LivingEntity target, Boolean attackSuccessful) {
        if (!Config.enableImprovedPhantoms) return;
        if (target.level().isClientSide()) return;
        
        if (attackSuccessful == null || !attackSuccessful) return;
        
        if (target instanceof Player player) {
            
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
    
    // Alternative approach - hook into the tick method to check for recent attacks
    @Inject(method = "tick", at = @At("HEAD"))
    private void bitsandbalance$onPhantomTick(CallbackInfo ci) {
        Phantom phantom = (Phantom) (Object) this;
        if (phantom.level().isClientSide()) return;
        
        // Phantom tick processing for improved phantoms feature
        
        if (!Config.enableImprovedPhantoms) return;
        
        // Check if phantom recently attacked a player (using vanilla attack target tracking)
        LivingEntity target = phantom.getTarget();
        if (target instanceof Player player && phantom.distanceTo(player) < 2.0) {
            // Check if phantom is in attack animation or recently attacked
            if (phantom.getLastHurtMob() == player && phantom.getLastHurtMobTimestamp() > phantom.level().getGameTime() - 5) {
                handlePhantomAttackOnPlayer(phantom, player);
            }
        }
    }
    
    private static void handlePhantomAttackOnPlayer(Phantom phantom, Player player) {
        
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
    
    // Reduce phantom movement speed when carrying a player by directly modifying velocity
    @Inject(method = "tick", at = @At("TAIL"))
    private void bitsandbalance$modifySpeedWhenCarryingPlayer(CallbackInfo ci) {
        if (!Config.enableImprovedPhantoms || !Config.enablePhantomPickup) return;
        
        Phantom phantom = (Phantom) (Object) this;
        if (phantom.level().isClientSide()) return;
        
        // Check if phantom is carrying a player
        boolean hasPlayerPassenger = false;
        Player playerPassenger = null;
        if (!phantom.getPassengers().isEmpty()) {
            for (var passenger : phantom.getPassengers()) {
                if (passenger instanceof Player) {
                    hasPlayerPassenger = true;
                    playerPassenger = (Player) passenger;
                    break;
                }
            }
        }
        
        // Track dismount for double damage detection
        if (hasPlayerPassenger && playerPassenger != null) {
            phantom.getPersistentData().putString("bitsandbalance_had_player_rider", playerPassenger.getUUID().toString());
        } else if (!hasPlayerPassenger && phantom.getPersistentData().contains("bitsandbalance_had_player_rider")) {
            // Player just dismounted
            String lastRiderUUID = phantom.getPersistentData().getString("bitsandbalance_had_player_rider").orElse("");
            phantom.getPersistentData().putString("bitsandbalance_last_rider", lastRiderUUID);
            phantom.getPersistentData().putLong("bitsandbalance_last_dismount_time", phantom.level().getGameTime());
            phantom.getPersistentData().remove("bitsandbalance_had_player_rider");
        }
        
        // Apply speed reduction by directly modifying velocity
        if (hasPlayerPassenger) {
            var currentVelocity = phantom.getDeltaMovement();
            double speedMultiplier = Config.phantomPickupSpeedMultiplier;
            
            // Scale down the velocity by the speed multiplier
            var reducedVelocity = currentVelocity.scale(speedMultiplier);
            phantom.setDeltaMovement(reducedVelocity);
            
            // Velocity reduced for phantom carrying player
        }
    }
    
    private static void applyStackingSlowness(Player player) {
        MobEffectInstance currentSlowness = player.getEffect(MobEffects.SLOWNESS);
        int newLevel = 1; // Start with Slowness I
        
        if (currentSlowness != null) {
            // Stack the effect, but cap at max level
            newLevel = Math.min(currentSlowness.getAmplifier() + 2, Config.phantomSlownessMaxLevel);
        }
        
        int durationTicks = Config.phantomSlownessDuration * 20; // Convert seconds to ticks
        MobEffectInstance newSlowness = new MobEffectInstance(MobEffects.SLOWNESS, durationTicks, newLevel - 1);
        player.addEffect(newSlowness);
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
                BitsAndBalance.LOGGER.warn("[PhantomMixin] Failed to make player ride phantom");
            }
        } catch (Exception e) {
            BitsAndBalance.LOGGER.error("[PhantomMixin] Exception during phantom pickup: ", e);
        }
    }
}

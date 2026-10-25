package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.compat.BetterCombatCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.onenonly.bitsandbalance.Config;
/**
 * Mixin for Player to detect phantom attacks via tick-based detection.
 * This enables players to attack phantoms while mounted on them with double damage.
 * Uses tick-based detection to bypass Minecraft's friendly fire protection for mounts.
 */
@Mixin(Player.class)
public abstract class PlayerTickMixin {

    private boolean wasSwinging = false;
    private int attackCooldown = 0;

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void bitsandbalance$detectPhantomAttack(CallbackInfo ci) {
        if (!Config.enableImprovedPhantoms || !Config.enablePhantomDoubleDamage) return;
        
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;
        
        // Reduce cooldown
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        
        // Check if player is swinging and mounted on a phantom
        boolean isSwinging = player.swinging;
        boolean isCurrentlyMounted = player.isPassenger() && player.getVehicle() instanceof Phantom;
        
        if (isSwinging && !wasSwinging && isCurrentlyMounted && attackCooldown == 0) {
            // Player just started swinging while mounted on a phantom
            Phantom phantom = (Phantom) player.getVehicle();
            
            // Check if Better Combat is present and handle accordingly
            if (BetterCombatCompat.isBetterCombatLoaded() && Config.enableBetterCombatIntegration) {
                BetterCombatCompat.handleBetterCombatPhantomAttack(player, phantom);
            } else {
                // Fallback to vanilla behavior
                
                // Apply damage manually
                float damage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                float finalDamage = damage * 2.0f; // Double damage
                
                
                // Create damage source and apply damage
                var damageSource = player.damageSources().playerAttack(player);
                phantom.hurt(damageSource, finalDamage);
                
                // Play attack sound
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG, 
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
                
            // Set cooldown to prevent spam (10 ticks = 0.5 seconds)
            attackCooldown = 10;
        }
        
        // Also check for recently dismounted players attacking phantoms
        if (isSwinging && !wasSwinging && !isCurrentlyMounted && attackCooldown == 0) {
            // Look for phantoms nearby that the player might have recently dismounted from
            AABB searchArea = player.getBoundingBox().inflate(3.0);
            var nearbyPhantoms = player.level().getEntitiesOfClass(Phantom.class, searchArea);
            
            for (Phantom phantom : nearbyPhantoms) {
                var phantomData = phantom.getPersistentData();
                if (phantomData.contains("bitsandbalance_last_rider") && phantomData.contains("bitsandbalance_last_dismount_time")) {
                    String lastRiderUUID = phantomData.getString("bitsandbalance_last_rider").orElse("");
                    long lastDismountTime = phantomData.getLong("bitsandbalance_last_dismount_time").orElse(0L);
                    long currentTime = phantom.level().getGameTime();
                    long ticksSinceDismount = currentTime - lastDismountTime;
                    
                    if (player.getUUID().toString().equals(lastRiderUUID) && ticksSinceDismount <= 10) {
                        
                        // Check if Better Combat is present and handle accordingly
                        if (BetterCombatCompat.isBetterCombatLoaded() && Config.enableBetterCombatIntegration) {
                            BetterCombatCompat.handleBetterCombatPhantomAttack(player, phantom);
                        } else {
                            // Fallback to vanilla behavior
                            
                            // Apply double damage
                            float damage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                            float finalDamage = damage * 2.0f;
                            
                            
                            var damageSource = player.damageSources().playerAttack(player);
                            phantom.hurt(damageSource, finalDamage);
                            
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG, 
                                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                        }
                            
                        // Set cooldown to prevent spam (10 ticks = 0.5 seconds)
                        attackCooldown = 10;
                        break; // Only attack one phantom
                    }
                }
            }
        }
        
        wasSwinging = isSwinging;
    }
}

package org.onenonly.bitsandbalance.compat;

import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

import java.lang.reflect.Method;

/**
 * Compatibility handler for Better Combat mod integration.
 * Provides enhanced phantom attack mechanics when Better Combat is present.
 */
public class BetterCombatCompat {
    
    private static boolean betterCombatLoaded = false;
    private static boolean checkedForBetterCombat = false;
    
    // Better Combat API classes and methods (accessed via reflection)
    private static Class<?> weaponAttributesClass;
    private static Class<?> attackEventClass;
    private static Method getWeaponAttributesMethod;
    private static Method calculateDamageMethod;
    
    /**
     * Check if Better Combat mod is loaded
     */
    public static boolean isBetterCombatLoaded() {
        if (!checkedForBetterCombat) {
            betterCombatLoaded = ModList.get().isLoaded("bettercombat");
            checkedForBetterCombat = true;
            if (betterCombatLoaded) {
                BitsAndBalance.LOGGER.info("[BetterCombatCompat] Better Combat mod detected, enabling enhanced compatibility");
                initializeBetterCombatAPI();
            }
        }
        return betterCombatLoaded;
    }
    
    /**
     * Initialize Better Combat API access via reflection
     */
    private static void initializeBetterCombatAPI() {
        try {
            // Try to access Better Combat's API classes
            weaponAttributesClass = Class.forName("net.bettercombat.api.WeaponAttributes");
            attackEventClass = Class.forName("net.bettercombat.api.AttackEvent");
            
            // Try to get methods for weapon attributes and damage calculation
            getWeaponAttributesMethod = Class.forName("net.bettercombat.api.BetterCombatAPI")
                .getMethod("getWeaponAttributes", ItemStack.class);
            calculateDamageMethod = Class.forName("net.bettercombat.api.BetterCombatAPI")
                .getMethod("calculateDamage", LivingEntity.class, LivingEntity.class, ItemStack.class);
            
            // Try to get fields from attack events (for future use)
            // attackTargetField = attackEventClass.getField("target");
            // attackDamageField = attackEventClass.getField("damage");
            
            BitsAndBalance.LOGGER.info("[BetterCombatCompat] Better Combat API initialized successfully");
        } catch (Exception e) {
            BitsAndBalance.LOGGER.warn("[BetterCombatCompat] Could not initialize Better Combat API, using fallback methods: {}", e.getMessage());
            // Reset API references if initialization fails
            weaponAttributesClass = null;
            attackEventClass = null;
            getWeaponAttributesMethod = null;
            calculateDamageMethod = null;
        }
    }
    
    /**
     * Handle phantom attack when Better Combat is present.
     * This method should be called from our attack detection systems
     * when Better Combat is loaded to provide enhanced compatibility.
     */
    public static void handleBetterCombatPhantomAttack(Player player, Phantom phantom) {
        if (!isBetterCombatLoaded() || !Config.enableBetterCombatIntegration) {
            return;
        }
        
        
        ItemStack weapon = player.getMainHandItem();
        float damage;
        
        // Try to use Better Combat's damage calculation if API is available
        if (calculateDamageMethod != null) {
            try {
                damage = (Float) calculateDamageMethod.invoke(null, player, phantom, weapon);
            } catch (Exception e) {
                BitsAndBalance.LOGGER.warn("[BetterCombatCompat] Failed to use Better Combat damage calculation, using fallback: {}", e.getMessage());
                damage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            }
        } else {
            // Fallback to vanilla damage calculation
            damage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        }
        
        // Apply our 2x multiplier to the calculated damage
        float finalDamage = damage * 2.0f;
        
        
        // Create damage source and apply damage
        var damageSource = player.damageSources().playerAttack(player);
        phantom.hurt(damageSource, finalDamage);
        
        // Play attack sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
            net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG, 
            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }
    
    /**
     * Enhanced phantom attack handler that integrates with Better Combat's attack system
     * This method should be called when Better Combat triggers an attack event
     */
    public static boolean handleBetterCombatAttackEvent(Player player, LivingEntity target, ItemStack weapon, float baseDamage) {
        if (!isBetterCombatLoaded() || !Config.enableBetterCombatIntegration) {
            return false; // Let Better Combat handle it normally
        }
        
        // Check if the target is a phantom and the player is mounted on it
        if (target instanceof Phantom && player.isPassenger() && player.getVehicle() == target) {
            
            // Apply our 2x damage multiplier
            float finalDamage = baseDamage * 2.0f;
            
            
            // Apply damage directly
            var damageSource = player.damageSources().playerAttack(player);
            target.hurt(damageSource, finalDamage);
            
            // Play attack sound
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG, 
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            
            return true; // We handled the attack, prevent Better Combat from processing it
        }
        
        return false; // Let Better Combat handle it normally
    }
    
    /**
     * Check if a player is mounted on a phantom and can attack with Better Combat
     */
    public static boolean canAttackPhantomWhileMounted(Player player) {
        if (!isBetterCombatLoaded() || !Config.enableBetterCombatIntegration) {
            return false;
        }
        
        return player.isPassenger() && player.getVehicle() instanceof Phantom;
    }
    
    /**
     * Get weapon attributes for an item using Better Combat API
     */
    public static Object getWeaponAttributes(ItemStack weapon) {
        if (!isBetterCombatLoaded() || getWeaponAttributesMethod == null) {
            return null;
        }
        
        try {
            return getWeaponAttributesMethod.invoke(null, weapon);
        } catch (Exception e) {
            BitsAndBalance.LOGGER.warn("[BetterCombatCompat] Failed to get weapon attributes: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if Better Combat API is properly initialized
     */
    public static boolean isAPIInitialized() {
        return betterCombatLoaded && weaponAttributesClass != null && attackEventClass != null;
    }
    
    /**
     * Initialize Better Combat compatibility hooks if the mod is present
     */
    public static void initializeCompatibility() {
        if (isBetterCombatLoaded()) {
            BitsAndBalance.LOGGER.info("[BetterCombatCompat] Initializing Better Combat compatibility features");
            
            // Register Better Combat specific event handlers if needed
            try {
                registerBetterCombatEvents();
            } catch (Exception e) {
                BitsAndBalance.LOGGER.error("[BetterCombatCompat] Failed to register Better Combat events: ", e);
            }
        }
    }
    
    /**
     * Register Better Combat specific event handlers
     * This method uses reflection to avoid hard dependencies
     */
    private static void registerBetterCombatEvents() {
        try {
            // Attempt to register for Better Combat's attack events
            // This would be implemented based on Better Combat's actual API
            // For now, we'll rely on our existing detection systems
            
        } catch (Exception e) {
            BitsAndBalance.LOGGER.warn("[BetterCombatCompat] Could not register Better Combat events, falling back to standard detection: {}", e.getMessage());
        }
    }
}

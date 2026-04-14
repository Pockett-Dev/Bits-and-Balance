package org.onenonly.bitsandbalance.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Optional;

import org.onenonly.bitsandbalance.Config;
/**
 * Balance: Source Dependent Invulnerability Frames
 * Allows damage within the invulnerability window if the incoming damage type
 * differs from the type that initiated the current i-frames, with a blacklist.
 *
 * Lightweight implementation that tracks damage sources without interfering with vanilla damage processing.
 */
@Mixin(LivingEntity.class)
public abstract class SourceDependentIFramesMixin {

    // Note: In MC 1.21.1, we need to track invulnerability differently
    // The isInvulnerable/isInvulnerableTo methods don't exist
    // We'll track damage timing directly

    // Track invulnerability frame timing and source
    @Unique
    private long rebalance$lastDamageTime = 0;
    @Unique
    private Identifier rebalance$currentIFrameSource = null;

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void rebalance$allowDifferentSourceDuringIFrames(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableSourceDependentIFrames) return;

        LivingEntity self = (LivingEntity) (Object) this;
        // Server-side only; client should not make gameplay decisions
        if (self.level().isClientSide()) return;

        // Check if entity is currently in invulnerability frames (within 10 ticks of last damage)
        long currentTime = self.level().getGameTime();
        boolean isInIFrames = (currentTime - rebalance$lastDamageTime) < 10;

        if (!isInIFrames) return; // Not in invulnerability frames, let normal damage processing continue

        // Get current damage type
        Identifier currentDamageType = getDamageTypeKey(source);

        // Check if this damage type is blacklisted (should never bypass)
        if (Config.sourceIFrameBlacklist != null && Config.sourceIFrameBlacklist.contains(currentDamageType)) {
            return; // Do not bypass i-frames for blacklisted damage types
        }

        // Check if we have a tracked source from current i-frame period
        if (rebalance$currentIFrameSource != null) {
            // If damage source is different from the one that initiated i-frames, allow bypass
            if (!Objects.equals(rebalance$currentIFrameSource, currentDamageType)) {
                // Prevent rapid stacking for DoT/environmental fire sources that often alternate damage types.
                if (isFireLikeDamage(source, currentDamageType) && isFireLikeLocation(rebalance$currentIFrameSource)) {
                    return;
                }
                // Allow the damage by setting return value to true (damage was processed)
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void rebalance$recordIFrameSource(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableSourceDependentIFrames) return;

        LivingEntity self = (LivingEntity) (Object) this;
        // Server-side only
        if (self.level().isClientSide()) return;

        // Only track if damage was actually applied (successful hurt)
        Boolean hurtResult = cir.getReturnValue();
        if (hurtResult != null && hurtResult) {
            // Update last damage time
            rebalance$lastDamageTime = self.level().getGameTime();

            // Get the damage type that was just applied
            Identifier damageType = getDamageTypeKey(source);

            // Check if this damage type is blacklisted
            if (Config.sourceIFrameBlacklist != null && Config.sourceIFrameBlacklist.contains(damageType)) {
                return; // Don't track blacklisted damage types as they don't create i-frames
            }

            // Track the source that initiated the current invulnerability period
            rebalance$currentIFrameSource = damageType;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void rebalance$clearIFrameSourceWhenExpired(CallbackInfo ci) {
        if (!Config.enableSourceDependentIFrames) return;

        LivingEntity self = (LivingEntity) (Object) this;
        // Server-side only
        if (self.level().isClientSide()) return;

        // Clear tracked source when invulnerability expires (after 10 ticks)
        long currentTime = self.level().getGameTime();
        if (rebalance$currentIFrameSource != null && (currentTime - rebalance$lastDamageTime) >= 10) {
            rebalance$currentIFrameSource = null;
        }
    }

    private static Identifier getDamageTypeKey(DamageSource source) {
        // Use a more reliable method to identify damage types
        try {
            // Try the modern approach first
            Object holder = source.getClass().getMethod("typeHolder").invoke(source);
            if (holder != null) {
                Optional<?> optKey = (Optional<?>) holder.getClass().getMethod("unwrapKey").invoke(holder);
                if (optKey != null && optKey.isPresent()) {
                    @SuppressWarnings("unchecked")
                    ResourceKey<DamageType> key = (ResourceKey<DamageType>) optKey.get();
                    return key.identifier();
                }
            }
        } catch (Throwable ignored) { }
        
        // Fallback: use damage source type identification
        try {
            // Check for common damage types using the source's type information
            if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)) {
                return Identifier.fromNamespaceAndPath("minecraft", "in_fire");
            }
            if (source.is(DamageTypes.LAVA)) {
                return Identifier.fromNamespaceAndPath("minecraft", "lava");
            }
            if (source.is(DamageTypes.HOT_FLOOR)) {
                return Identifier.fromNamespaceAndPath("minecraft", "hot_floor");
            }
            if (source.is(DamageTypes.MAGIC)) {
                return Identifier.fromNamespaceAndPath("minecraft", "magic");
            }
            if (source.is(DamageTypes.WITHER)) {
                return Identifier.fromNamespaceAndPath("minecraft", "wither");
            }
            if (source.is(DamageTypes.STARVE)) {
                return Identifier.fromNamespaceAndPath("minecraft", "starve");
            }
            if (source.is(DamageTypes.FALL)) {
                return Identifier.fromNamespaceAndPath("minecraft", "fall");
            }
            if (source.is(DamageTypes.FLY_INTO_WALL)) {
                return Identifier.fromNamespaceAndPath("minecraft", "fly_into_wall");
            }
            if (source.is(DamageTypes.GENERIC)) {
                return Identifier.fromNamespaceAndPath("minecraft", "generic");
            }
            if (source.is(DamageTypes.MOB_ATTACK)) {
                return Identifier.fromNamespaceAndPath("minecraft", "mob_attack");
            }
            if (source.is(DamageTypes.PLAYER_ATTACK)) {
                return Identifier.fromNamespaceAndPath("minecraft", "player_attack");
            }
            if (source.is(DamageTypes.ARROW)) {
                return Identifier.fromNamespaceAndPath("minecraft", "arrow");
            }
            if (source.is(DamageTypes.TRIDENT)) {
                return Identifier.fromNamespaceAndPath("minecraft", "trident");
            }
            if (source.is(DamageTypes.MOB_PROJECTILE)) {
                return Identifier.fromNamespaceAndPath("minecraft", "mob_projectile");
            }
            if (source.is(DamageTypes.THORNS)) {
                return Identifier.fromNamespaceAndPath("minecraft", "thorns");
            }
            if (source.is(DamageTypes.EXPLOSION)) {
                return Identifier.fromNamespaceAndPath("minecraft", "explosion");
            }
            if (source.is(DamageTypes.SONIC_BOOM)) {
                return Identifier.fromNamespaceAndPath("minecraft", "sonic_boom");
            }
            if (source.is(DamageTypes.SWEET_BERRY_BUSH)) {
                return Identifier.fromNamespaceAndPath("minecraft", "sweet_berry_bush");
            }
            if (source.is(DamageTypes.CACTUS)) {
                return Identifier.fromNamespaceAndPath("minecraft", "cactus");
            }
            if (source.is(DamageTypes.STING)) {
                return Identifier.fromNamespaceAndPath("minecraft", "sting");
            }
            if (source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) {
                return Identifier.fromNamespaceAndPath("minecraft", "mob_attack_no_aggro");
            }
            if (source.is(DamageTypes.PLAYER_EXPLOSION)) {
                return Identifier.fromNamespaceAndPath("minecraft", "player_explosion");
            }
            if (source.is(DamageTypes.BAD_RESPAWN_POINT)) {
                return Identifier.fromNamespaceAndPath("minecraft", "bad_respawn_point");
            }
            if (source.is(DamageTypes.OUTSIDE_BORDER)) {
                return Identifier.fromNamespaceAndPath("minecraft", "outside_border");
            }
            if (source.is(DamageTypes.GENERIC_KILL)) {
                return Identifier.fromNamespaceAndPath("minecraft", "generic_kill");
            }
        } catch (Throwable ignored) { }
        
        // Final fallback: use source class name as identifier
        return Identifier.fromNamespaceAndPath("unknown", source.getClass().getSimpleName().toLowerCase());
    }

    private static boolean isFireLikeDamage(DamageSource source, Identifier resolvedType) {
        try {
            if (source.is(DamageTypes.ON_FIRE)
                    || source.is(DamageTypes.IN_FIRE)
                    || source.is(DamageTypes.LAVA)
                    || source.is(DamageTypes.HOT_FLOOR)
                    || source.is(DamageTypes.CAMPFIRE)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return isFireLikeLocation(resolvedType);
    }

    private static boolean isFireLikeLocation(Identifier type) {
        if (type == null) return false;
        String path;
        try {
            path = type.getPath();
        } catch (Throwable ignored) {
            return false;
        }

        return "on_fire".equals(path)
                || "in_fire".equals(path)
                || "lava".equals(path)
                || "hot_floor".equals(path)
                || "campfire".equals(path);
    }
}

package org.onenonly.bitsandbalance.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.onenonly.bitsandbalance.common.config.BalanceFeatureFlags;
/**
 * Balance: Source Dependent Invulnerability Frames
 * Lets different damage-source families keep independent victim cooldown timers
 * so overlapping damage can apply without turning same-source i-frames off.
 */
@Mixin(LivingEntity.class)
public abstract class SourceDependentIFramesMixin {

    @Unique
    private static final long rebalance$SOURCE_IFRAME_TICKS = 10L;
    @Unique
    private static final ResourceLocation rebalance$FIRE_COOLDOWN_GROUP = ResourceLocation.fromNamespaceAndPath("bitsandbalance", "fire_family");
    @Unique
    private final Map<ResourceLocation, Long> rebalance$lastDamageTickByCooldownGroup = new HashMap<>();

    @Inject(method = "hurt", at = @At("HEAD"))
    private void rebalance$allowDifferentSourceDuringIFrames(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!BalanceFeatureFlags.sourceDependentIFramesEnabled()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        long currentTime = self.level().getGameTime();
        rebalance$cleanupExpiredSourceCooldowns(currentTime);
        if (self.invulnerableTime <= rebalance$SOURCE_IFRAME_TICKS) return;

        ResourceLocation rawDamageType = getDamageTypeKey(source);

        if (BalanceFeatureFlags.sourceIFrameBlacklisted(rawDamageType.toString())) {
            return;
        }

        ResourceLocation cooldownGroup = rebalance$getCooldownGroup(source, rawDamageType);
        Long lastDamageTick = rebalance$lastDamageTickByCooldownGroup.get(cooldownGroup);
        if (lastDamageTick != null && (currentTime - lastDamageTick) < rebalance$SOURCE_IFRAME_TICKS) {
            return;
        }

        self.invulnerableTime = 0;
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void rebalance$recordIFrameSource(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!BalanceFeatureFlags.sourceDependentIFramesEnabled()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        Boolean hurtResult = cir.getReturnValue();
        if (hurtResult == null || !hurtResult) {
            return;
        }

        ResourceLocation rawDamageType = getDamageTypeKey(source);

        if (BalanceFeatureFlags.sourceIFrameBlacklisted(rawDamageType.toString())) {
            return;
        }

        rebalance$lastDamageTickByCooldownGroup.put(rebalance$getCooldownGroup(source, rawDamageType), self.level().getGameTime());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void rebalance$cleanupCooldowns(CallbackInfo ci) {
        if (!BalanceFeatureFlags.sourceDependentIFramesEnabled() || rebalance$lastDamageTickByCooldownGroup.isEmpty()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        rebalance$cleanupExpiredSourceCooldowns(self.level().getGameTime());
    }

    @Unique
    private void rebalance$cleanupExpiredSourceCooldowns(long currentTime) {
        Iterator<Map.Entry<ResourceLocation, Long>> iterator = rebalance$lastDamageTickByCooldownGroup.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, Long> entry = iterator.next();
            if ((currentTime - entry.getValue()) >= rebalance$SOURCE_IFRAME_TICKS) {
                iterator.remove();
            }
        }
    }

    @Unique
    private static ResourceLocation rebalance$getCooldownGroup(DamageSource source, ResourceLocation rawDamageType) {
        if (isFireLikeDamage(source, rawDamageType)) {
            return rebalance$FIRE_COOLDOWN_GROUP;
        }
        return rawDamageType;
    }

    private static ResourceLocation getDamageTypeKey(DamageSource source) {
        // Use a more reliable method to identify damage types
        try {
            // Try the modern approach first
            Object holder = source.getClass().getMethod("typeHolder").invoke(source);
            if (holder != null) {
                Optional<?> optKey = (Optional<?>) holder.getClass().getMethod("unwrapKey").invoke(holder);
                if (optKey != null && optKey.isPresent()) {
                    @SuppressWarnings("unchecked")
                    ResourceKey<DamageType> key = (ResourceKey<DamageType>) optKey.get();
                    return key.location();
                }
            }
        } catch (Throwable ignored) { }
        
        // Fallback: use damage source type identification
        try {
            // Check for common damage types using the source's type information
            if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "in_fire");
            }
            if (source.is(DamageTypes.LAVA)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "lava");
            }
            if (source.is(DamageTypes.HOT_FLOOR)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "hot_floor");
            }
            if (source.is(DamageTypes.MAGIC)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "magic");
            }
            if (source.is(DamageTypes.WITHER)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "wither");
            }
            if (source.is(DamageTypes.STARVE)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "starve");
            }
            if (source.is(DamageTypes.FALL)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "fall");
            }
            if (source.is(DamageTypes.FLY_INTO_WALL)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "fly_into_wall");
            }
            if (source.is(DamageTypes.GENERIC)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "generic");
            }
            if (source.is(DamageTypes.MOB_ATTACK)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "mob_attack");
            }
            if (source.is(DamageTypes.PLAYER_ATTACK)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "player_attack");
            }
            if (source.is(DamageTypes.ARROW)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "arrow");
            }
            if (source.is(DamageTypes.TRIDENT)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "trident");
            }
            if (source.is(DamageTypes.MOB_PROJECTILE)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "mob_projectile");
            }
            if (source.is(DamageTypes.THORNS)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "thorns");
            }
            if (source.is(DamageTypes.EXPLOSION)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "explosion");
            }
            if (source.is(DamageTypes.SONIC_BOOM)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "sonic_boom");
            }
            if (source.is(DamageTypes.SWEET_BERRY_BUSH)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "sweet_berry_bush");
            }
            if (source.is(DamageTypes.CACTUS)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "cactus");
            }
            if (source.is(DamageTypes.STING)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "sting");
            }
            if (source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "mob_attack_no_aggro");
            }
            if (source.is(DamageTypes.PLAYER_EXPLOSION)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "player_explosion");
            }
            if (source.is(DamageTypes.BAD_RESPAWN_POINT)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "bad_respawn_point");
            }
            if (source.is(DamageTypes.OUTSIDE_BORDER)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "outside_border");
            }
            if (source.is(DamageTypes.GENERIC_KILL)) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "generic_kill");
            }
        } catch (Throwable ignored) { }
        
        // Final fallback: use source class name as ResourceLocation
        return ResourceLocation.fromNamespaceAndPath("unknown", source.getClass().getSimpleName().toLowerCase());
    }

    private static boolean isFireLikeDamage(DamageSource source, ResourceLocation resolvedType) {
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

    private static boolean isFireLikeLocation(ResourceLocation type) {
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

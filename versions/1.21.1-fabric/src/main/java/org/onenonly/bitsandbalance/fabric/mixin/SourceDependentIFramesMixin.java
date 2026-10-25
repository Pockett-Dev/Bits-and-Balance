package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.common.config.BalanceFeatureFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Balance: Source Dependent Invulnerability Frames
 * Lets different damage-source families keep independent victim cooldown timers
 * so overlapping damage can apply without turning same-source i-frames off.
 */
@Mixin(LivingEntity.class)
public abstract class SourceDependentIFramesMixin {

    @Unique
    private static final long bitsandbalance$SOURCE_IFRAME_TICKS = 10L;
    @Unique
    private static final ResourceLocation bitsandbalance$FIRE_COOLDOWN_GROUP = ResourceLocation.fromNamespaceAndPath("bitsandbalance", "fire_family");
    @Unique
    private final Map<ResourceLocation, Long> bitsandbalance$lastDamageTickByCooldownGroup = new HashMap<>();

    @Inject(method = "hurt", at = @At("HEAD"))
    private void bitsandbalance$allowDifferentSourceDuringIFrames(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!BalanceFeatureFlags.sourceDependentIFramesEnabled()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        long currentTime = self.level().getGameTime();
        bitsandbalance$cleanupExpiredSourceCooldowns(currentTime);
        if (self.invulnerableTime <= bitsandbalance$SOURCE_IFRAME_TICKS) return;

        ResourceLocation rawDamageType = getDamageTypeKey(source);

        if (BalanceFeatureFlags.sourceIFrameBlacklisted(rawDamageType.toString())) {
            return;
        }

        ResourceLocation cooldownGroup = bitsandbalance$getCooldownGroup(source, rawDamageType);
        Long lastDamageTick = bitsandbalance$lastDamageTickByCooldownGroup.get(cooldownGroup);
        if (lastDamageTick != null && (currentTime - lastDamageTick) < bitsandbalance$SOURCE_IFRAME_TICKS) {
            return;
        }

        self.invulnerableTime = 0;
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void bitsandbalance$recordIFrameSource(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!BalanceFeatureFlags.sourceDependentIFramesEnabled()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        Boolean hurtResult = cir.getReturnValue();
        if (hurtResult == null || !hurtResult) return;

        ResourceLocation rawDamageType = getDamageTypeKey(source);
        if (BalanceFeatureFlags.sourceIFrameBlacklisted(rawDamageType.toString())) {
            return;
        }

        bitsandbalance$lastDamageTickByCooldownGroup.put(bitsandbalance$getCooldownGroup(source, rawDamageType), self.level().getGameTime());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void bitsandbalance$cleanupCooldowns(CallbackInfo ci) {
        if (!BalanceFeatureFlags.sourceDependentIFramesEnabled() || bitsandbalance$lastDamageTickByCooldownGroup.isEmpty()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        bitsandbalance$cleanupExpiredSourceCooldowns(self.level().getGameTime());
    }

    @Unique
    private void bitsandbalance$cleanupExpiredSourceCooldowns(long currentTime) {
        Iterator<Map.Entry<ResourceLocation, Long>> iterator = bitsandbalance$lastDamageTickByCooldownGroup.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, Long> entry = iterator.next();
            if ((currentTime - entry.getValue()) >= bitsandbalance$SOURCE_IFRAME_TICKS) {
                iterator.remove();
            }
        }
    }

    @Unique
    private static ResourceLocation bitsandbalance$getCooldownGroup(DamageSource source, ResourceLocation rawDamageType) {
        if (isFireLikeDamage(source, rawDamageType)) {
            return bitsandbalance$FIRE_COOLDOWN_GROUP;
        }
        return rawDamageType;
    }

    private static ResourceLocation getDamageTypeKey(DamageSource source) {
        try {
            Object holder = source.getClass().getMethod("typeHolder").invoke(source);
            if (holder != null) {
                Optional<?> optKey = (Optional<?>) holder.getClass().getMethod("unwrapKey").invoke(holder);
                if (optKey != null && optKey.isPresent()) {
                    @SuppressWarnings("unchecked")
                    ResourceKey<DamageType> key = (ResourceKey<DamageType>) optKey.get();
                    return key.location();
                }
            }
        } catch (Throwable ignored) {
        }

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
        // Use the path to avoid being overly strict about namespace.
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

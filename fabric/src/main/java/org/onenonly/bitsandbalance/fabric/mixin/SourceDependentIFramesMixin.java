package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.Optional;

/**
 * Balance: Source Dependent Invulnerability Frames
 * Allow damage within i-frames if the incoming damage type differs
 * from the type that initiated the current i-frames (with a blacklist).
 */
@Mixin(LivingEntity.class)
public abstract class SourceDependentIFramesMixin {

    @Unique
    private Identifier bitsandbalance$currentIFrameSource;

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void bitsandbalance$allowDifferentSourceDuringIFrames(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricBalanceConfig.enableSourceDependentIFrames) return;

        LivingEntity self = (LivingEntity) (Object) this;

        // In vanilla, invulnerability is applied when invulnerableTime > 10.
        boolean inIFrames;
        try {
            inIFrames = self.invulnerableTime > 10.0F;
        } catch (Throwable ignored) {
            return;
        }
        if (!inIFrames) return;

        Identifier currentDamageType = getDamageTypeKey(source);

        if (FabricBalanceConfig.sourceIFrameBlacklist != null && FabricBalanceConfig.sourceIFrameBlacklist.contains(currentDamageType.toString())) {
            return;
        }

        if (bitsandbalance$currentIFrameSource != null && !Objects.equals(bitsandbalance$currentIFrameSource, currentDamageType)) {
            // Prevent rapid stacking for DoT/environmental fire sources that often alternate damage types.
            if (isFireLikeDamage(source, currentDamageType) && isFireLikeLocation(bitsandbalance$currentIFrameSource)) {
                return;
            }
            // Reset i-frame state so vanilla damage processing can proceed normally.
            try {
                self.invulnerableTime = 0;
            } catch (Throwable ignored) {
            }
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void bitsandbalance$recordIFrameSource(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricBalanceConfig.enableSourceDependentIFrames) return;

        LivingEntity self = (LivingEntity) (Object) this;
        Boolean hurtResult = cir.getReturnValue();
        if (hurtResult == null || !hurtResult) return;

        Identifier damageType = getDamageTypeKey(source);
        if (FabricBalanceConfig.sourceIFrameBlacklist != null && FabricBalanceConfig.sourceIFrameBlacklist.contains(damageType.toString())) {
            return;
        }

        bitsandbalance$currentIFrameSource = damageType;
    }

    private static Identifier getDamageTypeKey(DamageSource source) {
        try {
            Object holder = source.getClass().getMethod("typeHolder").invoke(source);
            if (holder != null) {
                Optional<?> optKey = (Optional<?>) holder.getClass().getMethod("unwrapKey").invoke(holder);
                if (optKey != null && optKey.isPresent()) {
                    @SuppressWarnings("unchecked")
                    ResourceKey<DamageType> key = (ResourceKey<DamageType>) optKey.get();
                    return key.identifier();
                }
            }
        } catch (Throwable ignored) {
        }

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

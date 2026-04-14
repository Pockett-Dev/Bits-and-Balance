package org.onenonly.bitsandbalance.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
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

import org.onenonly.bitsandbalance.Config;

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
    private static final Identifier rebalance$FIRE_COOLDOWN_GROUP = Identifier.fromNamespaceAndPath("bitsandbalance", "fire_family");
    @Unique
    private final Map<Identifier, Long> rebalance$lastDamageTickByCooldownGroup = new HashMap<>();

    @Inject(method = "hurtServer", at = @At("HEAD"), require = 0)
    private void rebalance$allowDifferentSourceDuringIFrames(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableSourceDependentIFrames) return;

        LivingEntity self = (LivingEntity) (Object) this;
        long currentTime = level.getGameTime();
        rebalance$cleanupExpiredSourceCooldowns(currentTime);
        if (self.invulnerableTime <= rebalance$SOURCE_IFRAME_TICKS) return;

        Identifier rawDamageType = getDamageTypeKey(source);
        if (Config.sourceIFrameBlacklist != null && Config.sourceIFrameBlacklist.contains(rawDamageType)) {
            return;
        }

        Identifier cooldownGroup = rebalance$getCooldownGroup(source, rawDamageType);
        Long lastDamageTick = rebalance$lastDamageTickByCooldownGroup.get(cooldownGroup);
        if (lastDamageTick != null && (currentTime - lastDamageTick) < rebalance$SOURCE_IFRAME_TICKS) {
            return;
        }

        self.invulnerableTime = 0;
    }

    @Inject(method = "hurtServer", at = @At("RETURN"), require = 0)
    private void rebalance$recordIFrameSource(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableSourceDependentIFrames) return;

        Boolean hurtResult = cir.getReturnValue();
        if (hurtResult == null || !hurtResult) {
            return;
        }

        Identifier rawDamageType = getDamageTypeKey(source);
        if (Config.sourceIFrameBlacklist != null && Config.sourceIFrameBlacklist.contains(rawDamageType)) {
            return;
        }

        rebalance$lastDamageTickByCooldownGroup.put(rebalance$getCooldownGroup(source, rawDamageType), level.getGameTime());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void rebalance$cleanupCooldowns(CallbackInfo ci) {
        if (!Config.enableSourceDependentIFrames || rebalance$lastDamageTickByCooldownGroup.isEmpty()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        rebalance$cleanupExpiredSourceCooldowns(self.level().getGameTime());
    }

    @Unique
    private void rebalance$cleanupExpiredSourceCooldowns(long currentTime) {
        Iterator<Map.Entry<Identifier, Long>> iterator = rebalance$lastDamageTickByCooldownGroup.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Identifier, Long> entry = iterator.next();
            if ((currentTime - entry.getValue()) >= rebalance$SOURCE_IFRAME_TICKS) {
                iterator.remove();
            }
        }
    }

    @Unique
    private static Identifier rebalance$getCooldownGroup(DamageSource source, Identifier rawDamageType) {
        if (isFireLikeDamage(source, rawDamageType)) {
            return rebalance$FIRE_COOLDOWN_GROUP;
        }
        return rawDamageType;
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
        } catch (Throwable ignored) { }

        try {
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

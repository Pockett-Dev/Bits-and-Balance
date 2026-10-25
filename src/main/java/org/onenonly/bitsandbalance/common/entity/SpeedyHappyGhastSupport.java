package org.onenonly.bitsandbalance.common.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class SpeedyHappyGhastSupport {
    private static final double VISIBLE_EFFECT_EXTRA_PARTICLE_CHANCE = 0.45D;
    private static final String VANILLA_BACKPORT_MOD_ID = "minecraft";
    private static final String VANILLA_BACKPORT_HAPPY_GHAST_PATH = "happy_ghast";
    private static final String VANILLA_BACKPORT_HAPPY_GHAST_CLASS_NAME =
        "com.blackgear.vanillabackport.common.level.entities.happyghast.HappyGhast";
    private static final ResourceLocation BITSANDBALANCE_SPEED_FLYING_SPEED_ID =
        ResourceLocation.fromNamespaceAndPath("bitsandbalance", "speed_flying_speed");
    private static final ResourceLocation BITSANDBALANCE_SLOWNESS_FLYING_SPEED_ID =
        ResourceLocation.fromNamespaceAndPath("bitsandbalance", "slowness_flying_speed");

    private SpeedyHappyGhastSupport() {
    }

    public static boolean isHappyGhastLikeEntity(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityTypeId != null && bitsandbalance$isKnownHappyGhastId(entityTypeId)) {
            return true;
        }

        if (VANILLA_BACKPORT_HAPPY_GHAST_CLASS_NAME.equals(entity.getClass().getName())) {
            return true;
        }

        if (entityTypeId != null && bitsandbalance$isHappyGhastToken(entityTypeId.getPath())) {
            return true;
        }

        return bitsandbalance$isHappyGhastToken(entity.getClass().getName());
    }

    public static float applyPotionAdjustedFlyingSpeed(LivingEntity entity, float flyingSpeed) {
        return flyingSpeed * getPotionAdjustedFlyingSpeedMultiplier(entity);
    }

    public static Vec3 applyPotionAdjustedVelocityChange(LivingEntity entity, Vec3 previousVelocity, Vec3 currentVelocity) {
        if (previousVelocity == null || currentVelocity == null) {
            return currentVelocity;
        }

        float multiplier = getPotionAdjustedFlyingSpeedMultiplier(entity);
        if (Math.abs(multiplier - 1.0F) < 1.0E-4F) {
            return currentVelocity;
        }

        Vec3 velocityChange = currentVelocity.subtract(previousVelocity);
        if (velocityChange.lengthSqr() <= 1.0E-8D) {
            return currentVelocity;
        }

        return previousVelocity.add(velocityChange.scale(multiplier));
    }

    public static void applyMissingSplashPotionEffects(ThrownPotion potion, Iterable<MobEffectInstance> effects, Entity directHitEntity) {
        if (potion == null || effects == null) {
            return;
        }

        AABB splashBounds = potion.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);
        Vec3 splashCenter = potion.position();
        Entity effectSource = potion.getEffectSource();

        for (LivingEntity entity : potion.level().getEntitiesOfClass(LivingEntity.class, splashBounds, SpeedyHappyGhastSupport::isHappyGhastLikeEntity)) {
            double distanceSqr = entity.getBoundingBox().distanceToSqr(splashCenter);
            if (distanceSqr >= 16.0D) {
                continue;
            }

            double multiplier = entity == directHitEntity ? 1.0D : 1.0D - Math.sqrt(distanceSqr) / 4.0D;
            if (multiplier <= 0.0D) {
                continue;
            }

            for (MobEffectInstance effect : effects) {
//? if mc == 26.2 || mc == 26.3
                if (effect.getEffect().value().isInstantaneous()) {
//? else
                if (effect.getEffect().value().isInstantenous()) {
//? endif
                    effect.getEffect().value().applyInstantenousEffect(potion, effectSource, entity, effect.getAmplifier(), multiplier);
                    continue;
                }

                int scaledDuration = (int) (multiplier * effect.getDuration() + 0.5D);
                if (scaledDuration <= 20) {
                    continue;
                }

                MobEffectInstance scaledEffect = new MobEffectInstance(
                    effect.getEffect(),
                    scaledDuration,
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
                );

                if (bitsandbalance$hasEqualOrStrongerEffect(entity, scaledEffect)) {
                    continue;
                }

                entity.addEffect(scaledEffect, effectSource);
            }
        }
    }

    public static float getPotionAdjustedFlyingSpeedMultiplier(LivingEntity entity) {
        if (entity == null) {
            return 1.0F;
        }

        float multiplier = bitsandbalance$getAttributeFlyingSpeedMultiplier(entity);
        if (bitsandbalance$shouldApplyPotionFallback(entity)) {
            multiplier *= bitsandbalance$getPotionEffectOnlyMultiplier(entity);
        }

        return multiplier;
    }

    public static boolean shouldUseVisibleEffectParticles(LivingEntity entity) {
        if (!isHappyGhastLikeEntity(entity)) {
            return false;
        }

        return entity.hasEffect(MobEffects.MOVEMENT_SPEED)
            || entity.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }

    public static Vec3 getVisibleEffectParticlePosition(LivingEntity entity) {
        if (entity == null) {
            return Vec3.ZERO;
        }

        double angle = entity.getRandom().nextDouble() * (Math.PI * 2.0D);
        double radius = Math.max(entity.getBbWidth() * 0.55D, 1.0D) + 0.25D + entity.getRandom().nextDouble() * 0.2D;
        double x = entity.getX() + Math.cos(angle) * radius;
        double y = entity.getY() + entity.getBbHeight() * (0.22D + entity.getRandom().nextDouble() * 0.42D);
        double z = entity.getZ() + Math.sin(angle) * radius;
        return new Vec3(x, y, z);
    }

    public static void spawnVisibleEffectParticles(Level level, ParticleOptions particle, LivingEntity entity, double velocityX, double velocityY, double velocityZ) {
        if (level == null || particle == null || entity == null) {
            return;
        }

        Vec3 primary = getVisibleEffectParticlePosition(entity);
        level.addParticle(particle, primary.x, primary.y, primary.z, velocityX, velocityY, velocityZ);

        if (entity.getRandom().nextDouble() < VISIBLE_EFFECT_EXTRA_PARTICLE_CHANCE) {
            Vec3 secondary = getVisibleEffectParticlePosition(entity);
            level.addParticle(particle, secondary.x, secondary.y, secondary.z, velocityX, velocityY, velocityZ);
        }
    }

    private static float bitsandbalance$getAttributeFlyingSpeedMultiplier(LivingEntity entity) {
        try {
            AttributeInstance flyingSpeedAttribute = entity.getAttribute(Attributes.FLYING_SPEED);
            if (flyingSpeedAttribute != null) {
                double baseValue = flyingSpeedAttribute.getBaseValue();
                double actualValue = flyingSpeedAttribute.getValue();
                if (baseValue > 0.0D && actualValue >= 0.0D) {
                    return (float) (actualValue / baseValue);
                }
            }
        } catch (Throwable ignored) {
        }

        return 1.0F;
    }

    private static float bitsandbalance$getPotionEffectOnlyMultiplier(LivingEntity entity) {
        if (entity == null) {
            return 1.0F;
        }

        float multiplier = 1.0F;

        MobEffectInstance speed = entity.getEffect(MobEffects.MOVEMENT_SPEED);
        if (speed != null) {
            multiplier *= 1.0F + 0.2F * (speed.getAmplifier() + 1);
        }

        MobEffectInstance slowness = entity.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slowness != null) {
            multiplier *= Math.max(0.0F, 1.0F - 0.15F * (slowness.getAmplifier() + 1));
        }

        return multiplier;
    }

    private static boolean bitsandbalance$shouldApplyPotionFallback(LivingEntity entity) {
        MobEffectInstance speed = entity.getEffect(MobEffects.MOVEMENT_SPEED);
        MobEffectInstance slowness = entity.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (speed == null && slowness == null) {
            return false;
        }

        try {
            AttributeInstance flyingSpeedAttribute = entity.getAttribute(Attributes.FLYING_SPEED);
            if (flyingSpeedAttribute == null) {
                return true;
            }

            boolean missingSpeedModifier = speed != null
                && flyingSpeedAttribute.getModifier(BITSANDBALANCE_SPEED_FLYING_SPEED_ID) == null;
            boolean missingSlownessModifier = slowness != null
                && flyingSpeedAttribute.getModifier(BITSANDBALANCE_SLOWNESS_FLYING_SPEED_ID) == null;

            return missingSpeedModifier || missingSlownessModifier;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static boolean bitsandbalance$hasEqualOrStrongerEffect(LivingEntity entity, MobEffectInstance candidate) {
        MobEffectInstance current = entity.getEffect(candidate.getEffect());
        if (current == null) {
            return false;
        }

        if (current.getAmplifier() > candidate.getAmplifier()) {
            return true;
        }

        return current.getAmplifier() == candidate.getAmplifier()
            && current.getDuration() >= candidate.getDuration();
    }

    private static boolean bitsandbalance$isHappyGhastToken(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return false;
        }

        String normalized = rawValue.toLowerCase(Locale.ROOT);
        return normalized.contains("happy_ghast") || normalized.contains("happyghast");
    }

    private static boolean bitsandbalance$isKnownHappyGhastId(ResourceLocation entityTypeId) {
        return VANILLA_BACKPORT_MOD_ID.equals(entityTypeId.getNamespace())
            && VANILLA_BACKPORT_HAPPY_GHAST_PATH.equals(entityTypeId.getPath());
    }
}

package org.onenonly.bitsandbalance.events;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.ModEffects;
import org.onenonly.bitsandbalance.mixin.ArrowAccessor;

/**
 * Event handler to detect when tipped arrows with our custom effects hit entities.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class TippedArrowEventHandler {

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        // Check if the projectile is an arrow
        if (event.getProjectile() instanceof AbstractArrow arrow) {
            // Check if it hit an entity
            if (event.getRayTraceResult() instanceof EntityHitResult entityHit) {
                if (entityHit.getEntity() instanceof LivingEntity target) {
                    // Check if the arrow has potion effects by looking at its item stack
                    var pickupItem = ((ArrowAccessor) arrow).invokeGetPickupItem();
                    if (pickupItem.has(DataComponents.POTION_CONTENTS)) {
                        var potionContents = pickupItem.get(DataComponents.POTION_CONTENTS);
                        if (potionContents != null) {
                            boolean hasEffects = potionContents.getAllEffects().iterator().hasNext();
                            if (hasEffects && target.level() instanceof ServerLevel serverLevel) {
                                // Check for our custom effects and apply them
                                potionContents.getAllEffects().forEach(effectInstance -> {
                                    if (effectInstance.getEffect() == ModEffects.DISPLACEMENT) {
                                        // For instant effects, we need to manually trigger the effect
                                        ModEffects.DISPLACEMENT.get().applyEffectTick(serverLevel, target, effectInstance.getAmplifier());
                                    } else if (effectInstance.getEffect() == ModEffects.RESURFACING) {
                                        // For instant effects, we need to manually trigger the effect
                                        ModEffects.RESURFACING.get().applyEffectTick(serverLevel, target, effectInstance.getAmplifier());
                                    } else if (effectInstance.getEffect() == ModEffects.RETURNING) {
                                        // For instant effects, we need to manually trigger the effect
                                        ModEffects.RETURNING.get().applyEffectTick(serverLevel, target, effectInstance.getAmplifier());
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    }
}

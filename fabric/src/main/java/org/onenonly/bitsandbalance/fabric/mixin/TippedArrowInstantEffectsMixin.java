package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.EntityHitResult;
import org.onenonly.bitsandbalance.fabric.registry.FabricEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ensures our custom instant potion effects correctly trigger when delivered via tipped arrows.
 *
 * Port of NeoForge's ProjectileImpact handler.
 */
@Mixin(AbstractArrow.class)
public abstract class TippedArrowInstantEffectsMixin {

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void bitsandbalance$instantEffects(EntityHitResult hit, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;
        if (!(hit.getEntity() instanceof LivingEntity target)) return;

        ItemStack pickup = ((ArrowAccessor) self).invokeGetPickupItem();
        if (!pickup.has(DataComponents.POTION_CONTENTS)) return;

        PotionContents contents = pickup.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return;

        for (MobEffectInstance instance : contents.getAllEffects()) {
            MobEffect effect = instance.getEffect().value();
            if (effect == FabricEffects.DISPLACEMENT || effect == FabricEffects.RESURFACING || effect == FabricEffects.RETURNING) {
                effect.applyEffectTick(level, target, instance.getAmplifier());
            }
        }
    }
}

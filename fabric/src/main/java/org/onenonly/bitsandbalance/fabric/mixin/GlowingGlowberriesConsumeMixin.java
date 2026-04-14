package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class GlowingGlowberriesConsumeMixin {

    @Unique
    private boolean bitsandbalance$pendingGlowberryGlowing;

    @Inject(
            method = "completeUsingItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",
                    shift = At.Shift.BEFORE
            )
    )
    private void bitsandbalance$glowingGlowberries$beforeFinishUsingItem(CallbackInfo ci) {
        if (!FabricTweaksConfig.enableGlowingGlowberries) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        ItemStack using = self.getUseItem();
                bitsandbalance$pendingGlowberryGlowing = using != null && using.getItem() == Items.GLOW_BERRIES;
    }

    @Inject(
            method = "completeUsingItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",
                    shift = At.Shift.AFTER
            )
    )
    private void bitsandbalance$glowingGlowberries$afterFinishUsingItem(CallbackInfo ci) {
        if (!bitsandbalance$pendingGlowberryGlowing) return;
        bitsandbalance$pendingGlowberryGlowing = false;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        int durationTicks = Math.max(1, FabricTweaksConfig.glowingGlowberriesDuration) * 20;
        self.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks, 0));
    }
}

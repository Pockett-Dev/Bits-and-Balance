package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Fox.class)
public abstract class GlowingGlowberriesFoxMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void bitsandbalance$foxGlowberries(CallbackInfo ci) {
        if (!FabricTweaksConfig.enableGlowingGlowberries) return;

        Fox fox = (Fox) (Object) this;
        if (fox.level().isClientSide()) return;
        if (fox.hasEffect(MobEffects.GLOWING)) return;

        ItemStack held = fox.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getItem() != Items.GLOW_BERRIES || held.getCount() <= 0) return;

        int durationTicks = Math.max(1, FabricTweaksConfig.foxGlowingDuration) * 20;
        fox.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks, 0));
    }
}

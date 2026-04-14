package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "org.violetmoon.quark.content.tweaks.module.GoldToolsHaveFortuneModule")
public abstract class GoldToolsHaveFortuneTooltipCompatMixin {

    @Inject(method = "createTooltipStack", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bitsandbalance$skipTooltipStackRewriteWithoutRegistries(ItemStack stack,
                                                                                DataComponentType<?> componentType,
                                                                                HolderLookup.Provider provider,
                                                                                CallbackInfoReturnable<ItemStack> cir) {
        if (provider == null) {
            cir.setReturnValue(stack);
        }
    }

    @Inject(method = "modifyTooltip", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bitsandbalance$skipTooltipRewriteWithoutRegistries(ItemStack stack,
                                                                           List<?> tooltip,
                                                                           HolderLookup.Provider provider,
                                                                           CallbackInfo ci) {
        if (provider == null) {
            ci.cancel();
        }
    }
}
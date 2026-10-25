package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.onenonly.bitsandbalance.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "moriyashiine.enchancement.common.util.EnchancementUtil", remap = false)
public abstract class EnchancementElytraDurabilityMixin {
    @Inject(method = "isUnbreakable", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void bitsandbalance$allowVanillaElytraDurability(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (Config.enableEnchancementElytraDurabilityCompat
                && !stack.isEmpty()
                && stack.getItem() == Items.ELYTRA) {
            cir.setReturnValue(false);
        }
    }
}

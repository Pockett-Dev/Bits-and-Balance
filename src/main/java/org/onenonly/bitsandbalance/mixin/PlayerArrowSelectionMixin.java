package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.common.mechanics.ArrowSelection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerArrowSelectionMixin {
    @Inject(method = "getProjectile", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$preferSelectedArrow(ItemStack weapon, CallbackInfoReturnable<ItemStack> cir) {
        if (!ArrowSelection.isRangedWeapon(weapon)) return;
        ItemStack preferred = ArrowSelection.findPreferredProjectile((Player) (Object) this, weapon);
        if (!preferred.isEmpty()) cir.setReturnValue(preferred);
    }
}

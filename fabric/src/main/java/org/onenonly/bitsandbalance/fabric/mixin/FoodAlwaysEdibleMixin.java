package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class FoodAlwaysEdibleMixin {

    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$foodAlwaysEdible(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        if (FabricBalanceConfig.enableFoodAlwaysEdible) {
            cir.setReturnValue(true);
        }
    }
}

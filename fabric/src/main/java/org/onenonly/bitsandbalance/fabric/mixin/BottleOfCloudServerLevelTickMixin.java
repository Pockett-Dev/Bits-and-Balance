package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudPlacedGlass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class BottleOfCloudServerLevelTickMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void bitsandbalance$bottleOfCloudTick(CallbackInfo ci) {
        BottleOfCloudPlacedGlass.tick((ServerLevel) (Object) this);
    }
}

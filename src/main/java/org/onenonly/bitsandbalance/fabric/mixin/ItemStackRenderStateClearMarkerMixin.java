package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.onenonly.bitsandbalance.common.client.BabItemStackRenderStateClearAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateClearMarkerMixin implements BabItemStackRenderStateClearAccess {

    @Unique
    private boolean bitsandbalance$needsIdentityInjection = true;

    @Override
    public boolean bitsandbalance$needsIdentityInjection() {
        return this.bitsandbalance$needsIdentityInjection;
    }

    @Override
    public void bitsandbalance$setNeedsIdentityInjection(boolean value) {
        this.bitsandbalance$needsIdentityInjection = value;
    }

    @Inject(method = "clear", at = @At("RETURN"), require = 0)
    private void bitsandbalance$markCleared(CallbackInfo ci) {
        this.bitsandbalance$needsIdentityInjection = true;
    }
}

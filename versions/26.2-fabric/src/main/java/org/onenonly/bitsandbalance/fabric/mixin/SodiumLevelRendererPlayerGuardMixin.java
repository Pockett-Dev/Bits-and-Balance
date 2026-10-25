package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class SodiumLevelRendererPlayerGuardMixin {

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$skipEarlyUpdateWithoutPlayer(Camera camera, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) {
            ci.cancel();
        }
    }
}

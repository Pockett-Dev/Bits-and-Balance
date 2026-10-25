package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMenuFpsMixin {
    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$uncapMenuFps(CallbackInfoReturnable<Integer> cir) {
        if (!FabricTweaksConfig.enableUncapMenuFps) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && !mc.isWindowActive()) return;

        if (mc != null && mc.screen != null && (mc.level == null || mc.isPaused())) {
            cir.setReturnValue(260);
        }
    }
}
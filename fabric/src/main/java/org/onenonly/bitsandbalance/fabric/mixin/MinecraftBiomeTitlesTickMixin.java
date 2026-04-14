package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import org.onenonly.bitsandbalance.common.client.BiomeTitlesClient;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftBiomeTitlesTickMixin {

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void bitsandbalance$biomeTitlesTick(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;

        org.onenonly.bitsandbalance.common.client.SoulFireOverlayClient.tick(mc, FabricClientConfig.soulFireOverlayEnabled);

        BiomeTitlesClient.Slot slot = "title".equalsIgnoreCase(FabricClientConfig.biomeTitlesSlot)
            ? BiomeTitlesClient.Slot.TITLE
            : BiomeTitlesClient.Slot.SUBTITLE;
        BiomeTitlesClient.tick(mc, new BiomeTitlesClient.Settings(
                FabricClientConfig.biomeTitlesEnabled,
                FabricClientConfig.biomeTitlesRecentSize,
                FabricClientConfig.biomeTitlesShowTicks,
                FabricClientConfig.biomeTitlesFadeTicks,
                (float) FabricClientConfig.biomeTitlesScale,
                FabricClientConfig.biomeTitlesOffsetX,
                FabricClientConfig.biomeTitlesOffsetY,
            FabricClientConfig.biomeTitlesShadow,
            slot,
            FabricClientConfig.biomeTitlesDisabledBiomeIds,
            FabricClientConfig.biomeTitlesDisabledBiomeTags
        ));
    }
}

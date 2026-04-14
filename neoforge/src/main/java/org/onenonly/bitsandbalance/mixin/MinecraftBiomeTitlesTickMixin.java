package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.client.BiomeTitlesClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftBiomeTitlesTickMixin {

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void bitsandbalance$biomeTitlesTick(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;

        org.onenonly.bitsandbalance.common.client.SoulFireOverlayClient.tick(mc, Config.soulFireOverlayEnabled);

        BiomeTitlesClient.Slot slot = "title".equalsIgnoreCase(Config.biomeTitlesSlot)
            ? BiomeTitlesClient.Slot.TITLE
            : BiomeTitlesClient.Slot.SUBTITLE;
        BiomeTitlesClient.tick(mc, new BiomeTitlesClient.Settings(
                Config.biomeTitlesEnabled,
                Config.biomeTitlesRecentSize,
                Config.biomeTitlesShowTicks,
                Config.biomeTitlesFadeTicks,
                (float) Config.biomeTitlesScale,
                Config.biomeTitlesOffsetX,
                Config.biomeTitlesOffsetY,
            Config.biomeTitlesShadow,
            slot,
            Config.biomeTitlesDisabledBiomeIds,
            Config.biomeTitlesDisabledBiomeTags
        ));
    }
}

package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.onenonly.bitsandbalance.common.client.BiomeTitlesClient;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiBiomeTitlesMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void bitsandbalance$renderBiomeTitles(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        BiomeTitlesClient.Slot slot = "title".equalsIgnoreCase(FabricClientConfig.biomeTitlesSlot)
            ? BiomeTitlesClient.Slot.TITLE
            : BiomeTitlesClient.Slot.SUBTITLE;
        BiomeTitlesClient.render(graphics, deltaTracker, new BiomeTitlesClient.Settings(
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

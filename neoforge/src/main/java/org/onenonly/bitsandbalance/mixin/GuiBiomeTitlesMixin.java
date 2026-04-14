package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.client.BiomeTitlesClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiBiomeTitlesMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void bitsandbalance$renderBiomeTitles(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        BiomeTitlesClient.Slot slot = "title".equalsIgnoreCase(Config.biomeTitlesSlot)
            ? BiomeTitlesClient.Slot.TITLE
            : BiomeTitlesClient.Slot.SUBTITLE;
        BiomeTitlesClient.render(graphics, deltaTracker, new BiomeTitlesClient.Settings(
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

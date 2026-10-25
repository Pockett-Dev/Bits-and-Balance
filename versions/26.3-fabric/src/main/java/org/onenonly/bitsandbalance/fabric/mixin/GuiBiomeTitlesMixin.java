package org.onenonly.bitsandbalance.fabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.onenonly.bitsandbalance.common.client.BiomeTitlesClient;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiBiomeTitlesMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At("TAIL"))
    private void bitsandbalance$renderBiomeTitles(DeltaTracker deltaTracker,
                                                  boolean renderHud,
                                                  boolean renderToasts,
                                                  CallbackInfo ci,
                                                  @Local(ordinal = 0) GuiGraphicsExtractor graphics) {
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
            FabricClientConfig.biomeTitlesSuppressInStructures,
            FabricClientConfig.biomeTitlesDisabledBiomeIds,
            FabricClientConfig.biomeTitlesDisabledBiomeTags
        ));
    }
}

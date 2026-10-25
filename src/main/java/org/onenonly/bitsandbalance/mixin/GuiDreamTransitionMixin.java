package org.onenonly.bitsandbalance.mixin;

//? if mc == 26.2 || mc == 26.3
import net.minecraft.client.gui.Hud;
//? else
import net.minecraft.client.gui.Gui;
//? endif
import net.minecraft.client.DeltaTracker;
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? else
import net.minecraft.client.gui.GuiGraphics;
//? endif
import org.onenonly.bitsandbalance.common.client.DreamProjectionClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if mc == 26.2 || mc == 26.3
@Mixin(Hud.class)
//? else
@Mixin(Gui.class)
//? endif
public abstract class GuiDreamTransitionMixin {
    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void bitsandbalance$renderDreamTransition(GuiGraphicsExtractor graphics, DeltaTracker tracker,
                                                       CallbackInfo ci) {
    //? else
    @Inject(method = "render", at = @At("TAIL"))
    private void bitsandbalance$renderDreamTransition(GuiGraphics graphics, DeltaTracker tracker,
                                                       CallbackInfo ci) {
    //? endif
        DreamProjectionClient.renderTransition(graphics);
    }
}

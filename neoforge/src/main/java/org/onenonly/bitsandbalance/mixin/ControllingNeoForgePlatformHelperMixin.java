package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.blamejared.controlling.platform.NeoForgePlatformHelper", remap = false)
public class ControllingNeoForgePlatformHelperMixin {

    @Inject(
            method = "handleKeyReleased",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;setKeyModifierAndCode(Lnet/neoforged/neoforge/client/settings/KeyModifier;Lcom/mojang/blaze3d/platform/InputConstants$Key;)V"
            ),
            cancellable = true
    )
    private void bitsandbalance$ignoreModifierOnlyRelease(
            @Coerce Object screen,
            Options options,
            KeyEvent event,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (event.isEscape()) {
            return;
        }

        if (!(screen instanceof KeyBindsScreen keyBindsScreen)) {
            return;
        }

        if (!(keyBindsScreen instanceof KeyBindsScreenModifierStateAccessor access)) {
            return;
        }

        if (!access.bitsandbalance$isLastKeyHeldDown()
                && !access.bitsandbalance$isLastModifierHeldDown()
                && access.bitsandbalance$getLastPressedKey().equals(InputConstants.UNKNOWN)
                && !access.bitsandbalance$getLastPressedModifier().equals(InputConstants.UNKNOWN)) {
            cir.setReturnValue(true);
        }
    }
}

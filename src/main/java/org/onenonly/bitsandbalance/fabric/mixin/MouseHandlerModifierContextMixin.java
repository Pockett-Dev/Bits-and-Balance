package org.onenonly.bitsandbalance.fabric.mixin;

//? if mc == 26.2 || mc == 26.3
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierContext;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? endif

//? if mc == 26.2 || mc == 26.3
@Mixin(MouseHandler.class)
//? endif
public class MouseHandlerModifierContextMixin {
    //? if mc == 26.2 || mc == 26.3
    private static final int STABLE_SHIFT_MASK = 1;
    private static final int STABLE_CONTROL_MASK = 2;
    private static final int STABLE_ALT_MASK = 4;

    @Inject(
            method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V",
            at = @At("HEAD"),
            require = 1
    )
    private void bitsandbalance$setModifierContext(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        int modifiers = 0;
        if (buttonInfo.hasShiftDown()) {
            modifiers |= STABLE_SHIFT_MASK;
        }
        if (buttonInfo.hasControlDown()) {
            modifiers |= STABLE_CONTROL_MASK;
        }
        if (buttonInfo.hasAltDown()) {
            modifiers |= STABLE_ALT_MASK;
        }
        KeybindModifierContext.set(modifiers & KeybindModifierStore.MOD_MASK);
    }

    @Inject(
            method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V",
            at = @At("RETURN"),
            require = 1
    )
    private void bitsandbalance$clearModifierContext(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        KeybindModifierContext.clear();
    }
    //? endif
}

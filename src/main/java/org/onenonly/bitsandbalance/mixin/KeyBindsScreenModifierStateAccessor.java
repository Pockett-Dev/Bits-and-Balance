package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBindsScreen.class)
public interface KeyBindsScreenModifierStateAccessor {

    @Accessor("lastPressedModifier")
    InputConstants.Key bitsandbalance$getLastPressedModifier();

    @Accessor("lastPressedModifier")
    void bitsandbalance$setLastPressedModifier(InputConstants.Key lastPressedModifier);

    @Accessor("lastPressedKey")
    InputConstants.Key bitsandbalance$getLastPressedKey();

    @Accessor("lastPressedKey")
    void bitsandbalance$setLastPressedKey(InputConstants.Key lastPressedKey);

    @Accessor("isLastKeyHeldDown")
    boolean bitsandbalance$isLastKeyHeldDown();

    @Accessor("isLastKeyHeldDown")
    void bitsandbalance$setIsLastKeyHeldDown(boolean isLastKeyHeldDown);

    @Accessor("isLastModifierHeldDown")
    boolean bitsandbalance$isLastModifierHeldDown();

    @Accessor("isLastModifierHeldDown")
    void bitsandbalance$setIsLastModifierHeldDown(boolean isLastModifierHeldDown);
}

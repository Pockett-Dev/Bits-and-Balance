package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBindsScreen.class)
public interface KeyBindsScreenAccessor {
    @Accessor("selectedKey")
    KeyMapping bitsandbalance$getSelectedKey();

    @Accessor("keyBindsList")
    KeyBindsList bitsandbalance$getKeyBindsList();
}

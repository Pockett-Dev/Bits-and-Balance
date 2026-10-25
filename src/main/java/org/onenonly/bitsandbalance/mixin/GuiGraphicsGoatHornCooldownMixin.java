//? if mc == 1.21.1
package org.onenonly.bitsandbalance.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.common.mechanics.GoatHornCooldowns;
import org.onenonly.bitsandbalance.common.mechanics.GoatHornPurposes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 1.21.1's vanilla overlay only accepts Item; supply the selected stack's horn timer. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsGoatHornCooldownMixin {
    @ModifyExpressionValue(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemCooldowns;getCooldownPercent(Lnet/minecraft/world/item/Item;F)F"))
    private float bitsandbalance$individualHornOverlay(float original, Font font, ItemStack stack,
                                                        int x, int y, String text) {
        LocalPlayer player = Minecraft.getInstance().player;
        String horn = GoatHornPurposes.horn(stack);
        boolean dreamCooldown = "dream".equals(horn) && GoatHornPurposes.isEnabled("dream");
        if (player == null || horn.isEmpty() || (!GoatHornPurposes.separateCooldowns && !dreamCooldown)) {
            return original;
        }
        return GoatHornCooldowns.percent(player, stack,
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
    }
}
//? endif

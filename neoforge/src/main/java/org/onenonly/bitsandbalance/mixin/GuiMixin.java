package org.onenonly.bitsandbalance.mixin;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.onenonly.bitsandbalance.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tweaks: Curse Uses
 * Hide pumpkin overlay when the worn pumpkin has Curse of Vanishing.
 * Correct hook for MC 1.21.1: Gui#renderTextureOverlay
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void rebalance$maybeSkipPumpkinOverlay(GuiGraphics graphics, Identifier overlayTex, float alpha, CallbackInfo ci) {
        if (rebalance$shouldSkip(overlayTex)) {
            ci.cancel();
        }
    }

    private static boolean rebalance$shouldSkip(Identifier overlayTex) {
        if (!Config.curseHidePumpkinOverlayOnVanishing || overlayTex == null) return false;
        try {
            boolean isPumpkin = "minecraft".equals(overlayTex.getNamespace()) && "textures/misc/pumpkinblur.png".equals(overlayTex.getPath());
            if (!isPumpkin) return false;

            Player player = Minecraft.getInstance().player;
            if (player == null) return false;
            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (head.isEmpty() || !head.is(Items.CARVED_PUMPKIN)) return false;

            ItemEnchantments ench = head.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var holder : ench.keySet()) {
                if (holder.is(Enchantments.VANISHING_CURSE)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}

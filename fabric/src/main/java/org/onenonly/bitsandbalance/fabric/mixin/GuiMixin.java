package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.onenonly.bitsandbalance.common.client.SoulFireOverlayClient;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tweaks: Curse Uses
 * Hide pumpkin overlay when the worn pumpkin has Curse of Vanishing.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    @ModifyVariable(
            method = "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1,
            require = 0
    )
        private Identifier bitsandbalance$swapFireOverlay(Identifier overlayTex) {
        if (!FabricClientConfig.soulFireOverlayEnabled) return overlayTex;
        if (!SoulFireOverlayClient.shouldUseSoulFireOverlayNow(Minecraft.getInstance())) return overlayTex;
        return SoulFireOverlayClient.remapFireOverlay(overlayTex);
    }

    @Inject(
            method = "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void bitsandbalance$maybeSkipPumpkinOverlay(
            GuiGraphics graphics,
            Identifier overlayTex,
            float alpha,
            CallbackInfo ci
    ) {
        if (bitsandbalance$shouldSkip(overlayTex)) {
            ci.cancel();
        }
    }

    private static boolean bitsandbalance$shouldSkip(Identifier overlayTex) {
        if (!FabricTweaksConfig.curseHidePumpkinOverlayOnVanishing || overlayTex == null) return false;

        try {
            boolean isPumpkin = "minecraft".equals(overlayTex.getNamespace())
                    && "textures/misc/pumpkinblur.png".equals(overlayTex.getPath());
            if (!isPumpkin) return false;

            Player player = Minecraft.getInstance().player;
            if (player == null) return false;

            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (head.isEmpty() || !head.is(Items.CARVED_PUMPKIN)) return false;

            ItemEnchantments ench = head.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
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

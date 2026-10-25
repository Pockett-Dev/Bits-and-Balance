package org.onenonly.bitsandbalance.mixin;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.client.SoulFireOverlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tweaks: Curse of Vanishing Usage
 * Hide pumpkin overlay when the worn pumpkin has Curse of Vanishing.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    private static final Material SOUL_FIRE_MATERIAL_1 = new Material(
            TextureAtlas.LOCATION_BLOCKS,
            ResourceLocation.parse("block/soul_fire_1")
    );

    @ModifyVariable(
            method = "renderTextureOverlay(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;F)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2,
            require = 0
    )
        private static ResourceLocation bitsandbalance$swapFireOverlayA(ResourceLocation overlayTex) {
        if (!Config.soulFireOverlayEnabled) return overlayTex;
        if (!SoulFireOverlayClient.shouldUseSoulFireOverlayNow(Minecraft.getInstance())) return overlayTex;
        return SoulFireOverlayClient.remapFireOverlay(overlayTex);
    }

    @ModifyVariable(
            method = "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;F)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1,
            require = 0
    )
        private static ResourceLocation bitsandbalance$swapFireOverlayB(ResourceLocation overlayTex) {
        if (!Config.soulFireOverlayEnabled) return overlayTex;
        if (!SoulFireOverlayClient.shouldUseSoulFireOverlayNow(Minecraft.getInstance())) return overlayTex;
        return SoulFireOverlayClient.remapFireOverlay(overlayTex);
    }

    @Redirect(
            method = "renderFire(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/resources/model/ModelBakery;FIRE_1:Lnet/minecraft/client/resources/model/Material;"
            ),
            require = 0
    )
    private static Material bitsandbalance$swapFireMaterial1() {
        if (!Config.soulFireOverlayEnabled) return ModelBakery.FIRE_1;
        if (!SoulFireOverlayClient.shouldUseSoulFireOverlayNow(Minecraft.getInstance())) return ModelBakery.FIRE_1;
        return SOUL_FIRE_MATERIAL_1;
    }

    // Variant A: 4-arg signature (includes Minecraft parameter)
    @Inject(method = "renderTextureOverlay(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;F)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void rebalance$maybeSkipPumpkinOverlayA(Minecraft minecraft, GuiGraphics guiGraphics, ResourceLocation overlayTex, float alpha, CallbackInfo ci) {
        if (rebalance$shouldSkip(overlayTex)) {
            ci.cancel();
        }
    }

    // Variant B: 3-arg signature (without Minecraft parameter)
    @Inject(method = "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;F)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void rebalance$maybeSkipPumpkinOverlayB(GuiGraphics guiGraphics, ResourceLocation overlayTex, float alpha, CallbackInfo ci) {
        if (rebalance$shouldSkip(overlayTex)) {
            ci.cancel();
        }
    }

    private static boolean rebalance$shouldSkip(ResourceLocation overlayTex) {
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

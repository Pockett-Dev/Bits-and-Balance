package org.onenonly.bitsandbalance.mixin;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.client.SoulFireOverlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tweaks: Curse Uses
 * Hide pumpkin overlay when the worn pumpkin has Curse of Vanishing.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    private static final Identifier FIRE_SPRITE_0 = Identifier.parse("minecraft:block/fire_0");
    private static final Identifier FIRE_SPRITE_1 = Identifier.parse("minecraft:block/fire_1");
    private static final Identifier SOUL_FIRE_SPRITE_0 = Identifier.parse("minecraft:block/soul_fire_0");
    private static final Identifier SOUL_FIRE_SPRITE_1 = Identifier.parse("minecraft:block/soul_fire_1");

    @ModifyVariable(
            method = "renderTextureOverlay(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2,
            require = 0
    )
        private static Identifier bitsandbalance$swapFireOverlayA(Identifier overlayTex) {
        if (!Config.soulFireOverlayEnabled) return overlayTex;
        if (!SoulFireOverlayClient.shouldUseSoulFireOverlayNow(Minecraft.getInstance())) return overlayTex;
        return SoulFireOverlayClient.remapFireOverlay(overlayTex);
    }

    @ModifyVariable(
            method = "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1,
            require = 0
    )
        private static Identifier bitsandbalance$swapFireOverlayB(Identifier overlayTex) {
        if (!Config.soulFireOverlayEnabled) return overlayTex;
        if (!SoulFireOverlayClient.shouldUseSoulFireOverlayNow(Minecraft.getInstance())) return overlayTex;
        return SoulFireOverlayClient.remapFireOverlay(overlayTex);
    }

    @ModifyVariable(
            method = "renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private static TextureAtlasSprite bitsandbalance$swapFireSprite(TextureAtlasSprite sprite) {
        if (!Config.soulFireOverlayEnabled) return sprite;
        if (!SoulFireOverlayClient.shouldUseSoulFireOverlayNow(Minecraft.getInstance())) return sprite;

        Identifier beforeName = null;
        try {
            beforeName = sprite != null ? sprite.contents().name() : null;
        } catch (Throwable ignored) {
        }

        Identifier target = null;
        if (FIRE_SPRITE_0.equals(beforeName)) target = SOUL_FIRE_SPRITE_0;
        else if (FIRE_SPRITE_1.equals(beforeName)) target = SOUL_FIRE_SPRITE_1;

        if (target != null) {
            try {
                Object tex = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
                TextureAtlasSprite fetched = tex instanceof TextureAtlas atlas ? atlas.getSprite(target) : null;
                if (fetched != null) {
                    return fetched;
                }
            } catch (Throwable ignored) {
            }
        }

        // Fallback to the common reflective lookup.
        return SoulFireOverlayClient.remapFireSprite(sprite);
    }

    // Variant A: 4-arg signature (includes Minecraft parameter)
    @Inject(method = "renderTextureOverlay(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void rebalance$maybeSkipPumpkinOverlayA(Minecraft minecraft, GuiGraphics guiGraphics, Identifier overlayTex, float alpha, CallbackInfo ci) {
        if (rebalance$shouldSkip(overlayTex)) {
            ci.cancel();
        }
    }

    // Variant B: 3-arg signature (without Minecraft parameter)
    @Inject(method = "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void rebalance$maybeSkipPumpkinOverlayB(GuiGraphics guiGraphics, Identifier overlayTex, float alpha, CallbackInfo ci) {
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

package org.onenonly.bitsandbalance.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.common.client.SoulFireOverlayClient;
import org.onenonly.bitsandbalance.common.client.UsesForCursesClient;
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
            method = "renderTextureOverlay",
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
            method = "renderTextureOverlay",
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

    @WrapOperation(
            method = "renderCameraOverlays",
            at = @At(
                    value = "INVOKE",
                target = "Lnet/minecraft/client/player/LocalPlayer;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"
            ),
            require = 0
    )
    private ItemStack bitsandbalance$hideVanishingPumpkinAtSlotLookup(
            LocalPlayer player,
            EquipmentSlot slot,
            Operation<ItemStack> original
    ) {
        ItemStack stack = original.call(player, slot);
        if (UsesForCursesClient.shouldHidePumpkinHeadOverlay(slot, stack, FabricTweaksConfig.curseHidePumpkinOverlayOnVanishing)) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

        @WrapOperation(
            method = "renderCameraOverlays",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"
            ),
            require = 0
        )
    private Object bitsandbalance$suppressPumpkinEquippableOverlay(
            ItemStack stack,
            DataComponentType<?> componentType,
            Operation<Object> original
    ) {
        if (UsesForCursesClient.shouldSuppressEquippableComponent(stack, componentType, FabricTweaksConfig.curseHidePumpkinOverlayOnVanishing)) {
            return null;
        }

        return original.call(stack, componentType);
    }

    @ModifyExpressionValue(
            method = "renderCameraOverlays",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Optional;isPresent()Z",
                    ordinal = 0
            ),
            require = 0
    )
    private boolean bitsandbalance$skipVanishingPumpkinEquipmentOverlay(
            boolean original,
            @Local(ordinal = 0) EquipmentSlot slot,
            @Local(ordinal = 0) ItemStack stack
    ) {
        if (!original) return false;
        if (UsesForCursesClient.shouldHidePumpkinHeadOverlay(slot, stack, FabricTweaksConfig.curseHidePumpkinOverlayOnVanishing)) {
            return false;
        }
        return true;
    }

    private static boolean bitsandbalance$shouldSkip(Identifier overlayTex) {
        return UsesForCursesClient.shouldHidePumpkinOverlay(overlayTex, FabricTweaksConfig.curseHidePumpkinOverlayOnVanishing);
    }
}

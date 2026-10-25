package org.onenonly.bitsandbalance.fabric.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.common.client.TooltipStackContext;
import org.onenonly.bitsandbalance.fabric.client.FabricKeyBindings;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierHelper;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.network.ItemSharePayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    private static long bitsandbalance$lastItemShareAtMs = 0L;

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"), require = 0)
    private void bitsandbalance$captureTooltipStack(GuiGraphics gfx, ItemStack stack, int x, int y, CallbackInfo ci) {
        TooltipStackContext.set(stack);
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("RETURN"), require = 0)
    private void bitsandbalance$clearTooltipStack(GuiGraphics gfx, ItemStack stack, int x, int y, CallbackInfo ci) {
        TooltipStackContext.clear();
    }

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$itemShareKey(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricTweaksConfig.enableItemSharing) {
            return;
        }

        Screen self = (Screen) (Object) this;
        if (!(self instanceof AbstractContainerScreen<?>)) {
            return;
        }

        try {
            if (!KeybindModifierHelper.matchesWithRequiredMask(
                FabricKeyBindings.ITEM_SHARE,
                keyCode,
                scanCode,
                modifiers,
                KeybindModifierStore.getRequiredMask(FabricKeyBindings.ITEM_SHARE)
            )) {
                return;
            }

            Slot slot = bitsandbalance$findHoveredSlot(self);
            if (slot == null) {
                return;
            }

            ItemStack stack = slot.getItem();
            if (stack == null || stack.isEmpty()) {
                return;
            }

            long cooldownMs = (long) (Math.max(0.0D, FabricTweaksConfig.itemShareCooldownSeconds) * 1000.0D);
            if (cooldownMs > 0L) {
                long now = System.currentTimeMillis();
                long elapsed = now - bitsandbalance$lastItemShareAtMs;
                if (elapsed >= 0L && elapsed < cooldownMs) {
                    cir.setReturnValue(true);
                    return;
                }
                bitsandbalance$lastItemShareAtMs = now;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            if (ClientPlayNetworking.canSend(ItemSharePayload.TYPE)) {
                ClientPlayNetworking.send(new ItemSharePayload(stack));
            }

            cir.setReturnValue(true);
        } catch (Throwable ignored) {
        }
    }

    private static Slot bitsandbalance$findHoveredSlot(Object screen) {
        if (screen == null) {
            return null;
        }
        Class<?> cls = screen.getClass();
        Slot fallback = null;
        while (cls != null) {
            for (Field field : cls.getDeclaredFields()) {
                if (!Slot.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(screen);
                    if (value instanceof Slot slot) {
                        String name = field.getName().toLowerCase();
                        if (name.contains("hover")) {
                            return slot;
                        }
                        if (fallback == null) {
                            fallback = slot;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            cls = cls.getSuperclass();
        }
        return fallback;
    }
}
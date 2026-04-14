package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.common.client.TooltipStackContext;
import org.onenonly.bitsandbalance.mechanics.ItemSharePayload;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.ClientConfig;
import org.onenonly.bitsandbalance.client.MyModKeyBindings;
import org.onenonly.bitsandbalance.client.KeybindModifierHelper;
/**
 * General Screen-level handler for item sharing functionality.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {

    private static long bitsandbalance$lastItemShareAtMs = 0L;

    @Inject(
            method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("HEAD"),
            require = 0
    )
    private void bitsandbalance$captureTooltipStack(GuiGraphics gfx, ItemStack stack, int x, int y, CallbackInfo ci) {
        TooltipStackContext.set(stack);
    }

    @Inject(
            method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("RETURN"),
            require = 0
    )
    private void bitsandbalance$clearTooltipStack(GuiGraphics gfx, ItemStack stack, int x, int y, CallbackInfo ci) {
        TooltipStackContext.clear();
    }


    private static Slot rebalance$findHoveredSlot(Object screen) {
        if (screen == null) return null;
        Class<?> cls = screen.getClass();
        Slot fallback = null;
        while (cls != null) {
            for (Field f : cls.getDeclaredFields()) {
                if (Slot.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object o = f.get(screen);
                        if (o instanceof Slot s && s != null) {
                            String name = f.getName().toLowerCase();
                            if (name.contains("hover")) return s;
                            if (fallback == null) fallback = s;
                        }
                    } catch (Throwable ignored) {}
                }
            }
            cls = cls.getSuperclass();
        }
        return fallback;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void rebalance$itemShareKey(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableItemSharing) return;
        Screen self = (Screen)(Object)this;
        if (!(self instanceof AbstractContainerScreen<?>)) return;
        Minecraft mc = Minecraft.getInstance();
        int keyCode = keyEvent.key();
        int scanCode = keyEvent.scancode();
        int modifiers = keyEvent.modifiers();
        try {
            // Check if the custom item share keybind is pressed WITH configured modifiers
            if (KeybindModifierHelper.matchesWithModifiers(
                    MyModKeyBindings.ITEM_SHARE,
                    keyCode,
                    scanCode,
                    modifiers,
                    ClientConfig.itemShareRequireShift,
                    ClientConfig.itemShareRequireAlt,
                    ClientConfig.itemShareRequireCtrl
            )) {
                Slot slot = rebalance$findHoveredSlot(self);
                if (slot != null) {
                    ItemStack stack = slot.getItem();
                    if (stack != null && !stack.isEmpty()) {
                        long cooldownMs = (long) (Math.max(0.0D, Config.itemShareCooldownSeconds) * 1000.0D);
                        if (cooldownMs > 0L) {
                            long now = System.currentTimeMillis();
                            long elapsed = now - bitsandbalance$lastItemShareAtMs;
                            if (elapsed >= 0L && elapsed < cooldownMs) {
                                cir.setReturnValue(true);
                                return;
                            }
                            bitsandbalance$lastItemShareAtMs = now;
                        }
                        if (mc.getConnection() != null) {
                            mc.getConnection().send(new ItemSharePayload(stack));
                        }
                        // consume key: prevents other actions
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

}

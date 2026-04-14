package org.onenonly.bitsandbalance.fabric.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.fabric.client.FabricKeyBindings;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierHelper;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.network.ItemSharePayload;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/** General Screen-level handler for item sharing functionality. */
@Mixin(Screen.class)
public abstract class ScreenMixin {

    private static long bitsandbalance$lastItemShareAtMs = 0L;

    private static Slot bitsandbalance$findHoveredSlot(Object screen) {
        if (screen == null) return null;
        Class<?> cls = screen.getClass();
        Slot fallback = null;
        while (cls != null) {
            for (Field f : cls.getDeclaredFields()) {
                if (Slot.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object o = f.get(screen);
                        if (o instanceof Slot s) {
                            String name = f.getName().toLowerCase();
                            if (name.contains("hover")) return s;
                            if (fallback == null) fallback = s;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
            cls = cls.getSuperclass();
        }
        return fallback;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$itemShareKey(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricTweaksConfig.enableItemSharing) return;

        Screen self = (Screen) (Object) this;
        if (!(self instanceof AbstractContainerScreen<?>)) return;

        int keyCode = keyEvent.key();
        int scanCode = keyEvent.scancode();
        int modifiers = keyEvent.modifiers();

        try {
            int requiredMask = KeybindModifierStore.getRequiredMask(FabricKeyBindings.ITEM_SHARE);
            if (!bitsandbalance$matchesWithModifiers(
                    FabricKeyBindings.ITEM_SHARE,
                    keyCode,
                    scanCode,
                    modifiers,
                    requiredMask
            )) {
                return;
            }

            Slot slot = bitsandbalance$findHoveredSlot(self);
            if (slot == null) return;

            ItemStack stack = slot.getItem();
            if (stack == null || stack.isEmpty()) return;

            double cooldownSeconds = Math.max(0.0D, FabricTweaksConfig.itemShareCooldownSeconds);
            long cooldownMs = (long) (cooldownSeconds * 1000.0D);
            if (cooldownMs > 0L) {
                long now = System.currentTimeMillis();
                long elapsed = now - bitsandbalance$lastItemShareAtMs;
                if (elapsed >= 0L && elapsed < cooldownMs) {
                    cir.setReturnValue(true);
                    return;
                }
                bitsandbalance$lastItemShareAtMs = now;
            }

            var mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (ClientPlayNetworking.canSend(ItemSharePayload.TYPE)) {
                ClientPlayNetworking.send(new ItemSharePayload(stack));
            }

            // consume key: prevents other actions
            cir.setReturnValue(true);
        } catch (Throwable ignored) {
        }
    }

    private static boolean bitsandbalance$matchesWithModifiers(
            net.minecraft.client.KeyMapping keyMapping,
            int keyCode,
            int scanCode,
            int modifiers,
            int requiredMask
    ) {
        return KeybindModifierHelper.matchesWithModifiers(
                keyMapping,
                keyCode,
                scanCode,
                modifiers,
                (requiredMask & GLFW.GLFW_MOD_SHIFT) != 0,
                (requiredMask & GLFW.GLFW_MOD_ALT) != 0,
                (requiredMask & GLFW.GLFW_MOD_CONTROL) != 0
        );
    }
}

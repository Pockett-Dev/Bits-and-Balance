package org.onenonly.bitsandbalance.common.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.lang.reflect.Method;

final class InventorySyncUtil {
    private InventorySyncUtil() {
    }

    static void syncPlayerInventory(ServerPlayer player) {
        if (player == null) return;

        try {
            player.getInventory().setChanged();
        } catch (Throwable ignored) {
        }

        try {
            if (player.containerMenu != null) {
                player.containerMenu.slotsChanged(player.getInventory());
            }
        } catch (Throwable ignored) {
        }

        try {
            if (player.inventoryMenu != null && player.inventoryMenu != player.containerMenu) {
                player.inventoryMenu.slotsChanged(player.getInventory());
            }
        } catch (Throwable ignored) {
        }

        syncMenu(player.containerMenu);
        if (player.inventoryMenu != player.containerMenu) {
            syncMenu(player.inventoryMenu);
        }
    }

    private static void syncMenu(AbstractContainerMenu menu) {
        if (menu == null) return;
        invokeNoArg(menu, "broadcastChanges");
        invokeNoArg(menu, "broadcastFullState");
        invokeNoArg(menu, "sendAllDataToRemote");
    }

    private static void invokeNoArg(Object target, String methodName) {
        if (target == null) return;

        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        } catch (Throwable ignored) {
        }
    }
}
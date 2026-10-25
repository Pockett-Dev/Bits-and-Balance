package org.onenonly.bitsandbalance.fabric.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.onenonly.bitsandbalance.common.config.GameplayFeatureFlags;
import org.onenonly.bitsandbalance.common.inventory.AutomaticBlockRestock;
import org.onenonly.bitsandbalance.mixin.InventoryAccessor;

public final class FabricAutomaticBlockRestock {
    private FabricAutomaticBlockRestock() {
    }

    public static void shrinkAndRestock(UseOnContext context) {
        ItemStack held = context.getItemInHand();
        if (held.isEmpty()) return;

        ItemStack original = held.copy();
        held.shrink(1);

        if (!GameplayFeatureFlags.automaticBlockRestockEnabled()) return;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        int selected;
        try {
            selected = ((InventoryAccessor) (Object) player.getInventory()).bitsandbalance$getSelected();
        } catch (Throwable ignored) {
            return;
        }
        if (selected < 0 || selected > 8) return;

        ItemStack current;
        try {
            current = player.getInventory().getItem(selected);
        } catch (Throwable ignored) {
            return;
        }
        if (current != null && !current.isEmpty()) return;

        AutomaticBlockRestock.tryRestockEmptyHotbarBlock(player, selected, original);
    }
}

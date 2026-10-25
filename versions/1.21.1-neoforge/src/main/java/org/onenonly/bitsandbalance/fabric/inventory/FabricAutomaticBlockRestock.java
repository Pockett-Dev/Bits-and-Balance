package org.onenonly.bitsandbalance.fabric.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.inventory.AutomaticBlockRestock;

/**
 * NeoForge bridge used by the shared step placement code.
 */
public final class FabricAutomaticBlockRestock {
    private FabricAutomaticBlockRestock() {
    }

    public static void shrinkAndRestock(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (stack == null || stack.isEmpty()) {
            return;
        }

        Player player = context.getPlayer();
        ItemStack original = stack.copy();
        int selected = -1;
        if (player != null) {
            selected = player.getInventory().selected;
        }

        stack.shrink(1);

        if (!Config.enableAutomaticBlockRestock || !stack.isEmpty()) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && selected >= 0) {
            AutomaticBlockRestock.tryRestockEmptyHotbarBlock(serverPlayer, selected, original);
        }
    }
}

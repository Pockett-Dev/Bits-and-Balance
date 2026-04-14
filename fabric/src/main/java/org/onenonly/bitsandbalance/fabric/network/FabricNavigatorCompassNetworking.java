package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Deprecated compatibility shim.
 *
 * <p>This class used to contain both server and client networking code. That caused dedicated-server
 * startup crashes because it referenced client-only classes (e.g. Screen). Server and client logic
 * is now split into dedicated classes.</p>
 */
@Deprecated
public final class FabricNavigatorCompassNetworking {
    private FabricNavigatorCompassNetworking() {
    }

    public static void initServer() {
        FabricNavigatorCompassNetworkingServer.initServer();
    }

    public static void sendOpenGui(ServerPlayer player, int x, int y, int z) {
        FabricNavigatorCompassNetworkingServer.sendOpenGui(player, x, y, z);
    }

    public static void setCompassTarget(ItemStack compass, ServerLevel level, BlockPos targetPos) {
        FabricNavigatorCompassNetworkingServer.setCompassTarget(compass, level, targetPos);
    }

    public static void clearCompassTarget(ItemStack compass) {
        FabricNavigatorCompassNetworkingServer.clearCompassTarget(compass);
    }
}

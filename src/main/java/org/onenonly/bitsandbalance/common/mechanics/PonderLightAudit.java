package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

//? if fabric
import org.onenonly.bitsandbalance.fabric.network.FabricPonderLightAuditSync;
//? else
import net.neoforged.neoforge.network.PacketDistributor;
import org.onenonly.bitsandbalance.network.PonderLightAuditPayload;
//? endif

/** Server-authorized trigger for the private Ponder light-placement guide. */
public final class PonderLightAudit {
    private PonderLightAudit() {
    }

    public static boolean start(ServerPlayer player) {
        if (!GoatHornPurposes.isEnabled("ponder") || player.isSpectator() || !player.isAlive()) {
            return false;
        }
        int emission = lightEmission(player);
        //? if fabric
        FabricPonderLightAuditSync.send(player, emission);
        //? else
        PacketDistributor.sendToPlayer(player, new PonderLightAuditPayload(
                player.getX(), player.getEyeY(), player.getZ(), emission));
        //? endif
        return true;
    }

    /** Use the brightest carried placeable light; a torch is the fallback when none is carried. */
    private static int lightEmission(ServerPlayer player) {
        int best = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem item) {
                BlockState state = item.getBlock().defaultBlockState();
                int emission = state.getLightEmission();
                // Candles and other lights may be placed unlit, then turned on.
                if (emission == 0 && state.hasProperty(BlockStateProperties.LIT)) {
                    emission = state.setValue(BlockStateProperties.LIT, true).getLightEmission();
                }
                best = Math.max(best, emission);
            }
        }
        return best > 0 ? best : 14;
    }
}

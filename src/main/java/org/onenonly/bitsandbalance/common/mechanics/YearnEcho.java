package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.server.level.ServerPlayer;
//? if fabric
import org.onenonly.bitsandbalance.fabric.network.FabricYearnEchoSync;
//? else
import net.neoforged.neoforge.network.PacketDistributor;
import org.onenonly.bitsandbalance.network.YearnEchoPayload;
//? endif

/** Server-authorized trigger; the receiving client finds a cave entrance in loaded terrain. */
public final class YearnEcho {
    private YearnEcho() {
    }

    public static boolean start(ServerPlayer player) {
        if (!GoatHornPurposes.isEnabled("yearn") || player.isSpectator() || !player.isAlive()) {
            return false;
        }
        //? if fabric
        FabricYearnEchoSync.send(player);
        //? else
        PacketDistributor.sendToPlayer(player, new YearnEchoPayload(
                player.getX(), player.getEyeY(), player.getZ()));
        //? endif
        return true;
    }
}

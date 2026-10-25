package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.common.client.SecondChanceToast;

public final class FabricSecondChanceToastSync {
    private FabricSecondChanceToastSync() {
    }

    public static void initClient() {
        FabricYearnEchoSync.initClient();
        FabricPonderLightAuditSync.initClient();
        ClientPlayNetworking.registerGlobalReceiver(SecondChanceToastPayload.TYPE, (payload, context) -> {
            context.client().execute(SecondChanceToast::show);
        });
    }

    public static void send(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, SecondChanceToastPayload.TYPE)) {
            return;
        }
        ServerPlayNetworking.send(player, new SecondChanceToastPayload());
    }
}

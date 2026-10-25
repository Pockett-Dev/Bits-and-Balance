package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.onenonly.bitsandbalance.common.mechanics.ArrowSelection;

/** Server-authoritative updates to each held bow or crossbow. */
public final class FabricArrowSelectionNetworking {
    private FabricArrowSelectionNetworking() {
    }

    public static void initServer() {
        ServerPlayNetworking.registerGlobalReceiver(ArrowSelectionSetPayload.TYPE, (payload, context) ->
                context.server().execute(() -> ArrowSelection.setSelectedIfHeld(
                        context.player(), payload.hand(), payload.arrow())));
    }
}

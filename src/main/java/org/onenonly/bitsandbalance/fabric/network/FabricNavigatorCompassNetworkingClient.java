package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.onenonly.bitsandbalance.fabric.client.FabricNavigatorCompassScreen;

/** Client-only networking for Navigator Compass. */
public final class FabricNavigatorCompassNetworkingClient {
    private FabricNavigatorCompassNetworkingClient() {
    }

    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(NavigatorCompassOpenGUIPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                var mc = Minecraft.getInstance();
                if (mc == null) {
                    return;
                }
                mc.setScreen(new FabricNavigatorCompassScreen(payload.x(), payload.y(), payload.z()));
            });
        });
    }
}

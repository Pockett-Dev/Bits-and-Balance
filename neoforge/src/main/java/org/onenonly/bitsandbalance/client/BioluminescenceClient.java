package org.onenonly.bitsandbalance.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;

@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public final class BioluminescenceClient {
    private BioluminescenceClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        BioluminescenceLightManager.tickClient(Minecraft.getInstance());
    }
}
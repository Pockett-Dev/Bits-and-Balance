package org.onenonly.bitsandbalance.setup;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.client.renderer.AzaleaBoatRenderer;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;
import org.onenonly.bitsandbalance.registry.ModEntityTypes;

/**
 * Client-side setup for registering renderers and materials.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public class ClientSetup {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BitsAndBalance.LOGGER.info("Registered Azalea wood type with Sheets for sign/hanging sign support");
        });
    }
    
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register boat renderers with proper constructor references
        event.registerEntityRenderer(ModEntityTypes.AZALEA_BOAT.get(), 
            context -> new AzaleaBoatRenderer(context, false));
        event.registerEntityRenderer(ModEntityTypes.AZALEA_CHEST_BOAT.get(), 
            context -> new AzaleaBoatRenderer(context, true));
        event.registerEntityRenderer(ModGlowGoo.glowGooProjectileType(), ThrownItemRenderer::new);

        BitsAndBalance.LOGGER.info("Registered Azalea boat entity renderers");
    }

}


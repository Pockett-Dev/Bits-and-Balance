package org.onenonly.bitsandbalance.setup;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.content.ModAzalea;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.onenonly.bitsandbalance.registry.ModEntityTypes;

import java.lang.reflect.Field;

/**
 * Client-side setup for registering renderers and materials.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public class ClientSetup {
    private static final Identifier AZALEA_BOAT_TEXTURE = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "textures/entity/boat/azalea.png");
    private static final Identifier AZALEA_CHEST_BOAT_TEXTURE = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "textures/entity/chest_boat/azalea.png");
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BitsAndBalance.LOGGER.info("Registered Azalea wood type with Sheets for sign/hanging sign support");
        });
    }
    
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.AZALEA_BOAT.get(), context -> bitsandbalance$createRuntimeBoatRenderer(context, ModelLayers.CHERRY_BOAT, AZALEA_BOAT_TEXTURE, "azalea boat"));
        event.registerEntityRenderer(ModEntityTypes.AZALEA_CHEST_BOAT.get(), context -> bitsandbalance$createRuntimeBoatRenderer(context, ModelLayers.CHERRY_CHEST_BOAT, AZALEA_CHEST_BOAT_TEXTURE, "azalea chest boat"));
        event.registerEntityRenderer(ModGlowGoo.glowGooProjectileType(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        BioluminescenceLightManager.tickClient(Minecraft.getInstance());
    }

    private static BoatRenderer bitsandbalance$createRuntimeBoatRenderer(
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context,
            net.minecraft.client.model.geom.ModelLayerLocation modelLayer,
            Identifier texture,
            String description
    ) {
        BoatRenderer renderer = new BoatRenderer(context, modelLayer);
        try {
            Field textureField = bitsandbalance$findField(BoatRenderer.class, "texture");
            textureField.setAccessible(true);
            textureField.set(renderer, texture);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            BitsAndBalance.LOGGER.warn("Unable to override {} texture on runtime BoatRenderer; using the vanilla layer texture instead", description, exception);
        }
        return renderer;
    }

    private static Field bitsandbalance$findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

}


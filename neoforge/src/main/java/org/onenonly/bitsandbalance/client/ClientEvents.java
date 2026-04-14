package org.onenonly.bitsandbalance.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.client.CloudFadeRenderer;
import org.onenonly.bitsandbalance.common.client.TemporaryCloudFadeRenderer;
import org.onenonly.bitsandbalance.registry.ModBlockEntities;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ClientEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Deferred during NeoForge 26.1 client bring-up.
    }

    @SubscribeEvent
    public static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        // Deferred bottle-cloud rendering reuses the vanilla translucent moving-block pipeline.
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        // Deferred during NeoForge 26.1 client bring-up.
    }

    public static void onAfterTranslucentBlocks(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        CloudFadeRenderer.renderAllDeferred();
        TemporaryCloudFadeRenderer.renderAllDeferred();
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        bitsandbalance$registerOptionalBlockEntityRenderer(
            event,
            ModBlockEntities.TEMPORARY_CLOUD.get(),
            "org.onenonly.bitsandbalance.client.renderer.TemporaryCloudBlockEntityRenderer",
            "temporary cloud"
        );
        try {
            Class<?> hooks = Class.forName("org.onenonly.bitsandbalance.client.NeoForgeEnhancedSlabClientHooks");
            Method method = hooks.getMethod("onRegisterRenderers", EntityRenderersEvent.RegisterRenderers.class);
            method.invoke(null, event);
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException exception) {
            BitsAndBalance.LOGGER.warn("Failed to register NeoForge enhanced slab renderers", exception);
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        try {
            Class<?> hooks = Class.forName("org.onenonly.bitsandbalance.client.NeoForgeEnhancedSlabClientHooks");
            hooks.getMethod("onModifyBakingResult", ModelEvent.ModifyBakingResult.class).invoke(null, event);
        } catch (ClassNotFoundException ignored) {
            // Neo slab model hooks live in the dedicated client source set.
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke NeoForge slab client hooks", e);
        }
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        if (org.onenonly.bitsandbalance.Config.enableAutoWalkKey) {
            AutoWalkKeyHandler.registerKeybinding(event);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bitsandbalance$registerOptionalBlockEntityRenderer(
        EntityRenderersEvent.RegisterRenderers event,
        net.minecraft.world.level.block.entity.BlockEntityType<?> blockEntityType,
        String rendererClassName,
        String description
    ) {
        try {
            Class<?> rendererClass = Class.forName(rendererClassName, true, Thread.currentThread().getContextClassLoader());
            Constructor<?> constructor = rendererClass.getDeclaredConstructor(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context.class);
            constructor.setAccessible(true);
            event.registerBlockEntityRenderer((net.minecraft.world.level.block.entity.BlockEntityType) blockEntityType, context -> {
                try {
                    return (net.minecraft.client.renderer.blockentity.BlockEntityRenderer) constructor.newInstance(context);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Failed to instantiate optional NeoForge block entity renderer " + rendererClassName, exception);
                }
            });
        } catch (Throwable throwable) {
            BitsAndBalance.LOGGER.warn("Skipping NeoForge {} block entity renderer on the current 26.1 runtime surface", description, throwable);
        }
    }
}

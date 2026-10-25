package org.onenonly.bitsandbalance.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.content.ModAzalea;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;
import org.onenonly.bitsandbalance.common.registry.ModWoodTypes;
import org.onenonly.bitsandbalance.fabric.client.renderer.AzaleaBoatRenderer;
import org.onenonly.bitsandbalance.fabric.client.FabricKeyBindings;
import org.onenonly.bitsandbalance.fabric.client.FabricStanceClient;
import org.onenonly.bitsandbalance.fabric.client.FabricDoorKnockClient;
import org.onenonly.bitsandbalance.fabric.client.FabricAutoWalk;
import org.onenonly.bitsandbalance.fabric.client.FabricDynamicVerticalStepModelPlugin;
import org.onenonly.bitsandbalance.fabric.client.FabricEnhancedSlabMeshModelPlugin;
import org.onenonly.bitsandbalance.fabric.client.FabricEnhancedSlabModelOffsetPlugin;
import org.onenonly.bitsandbalance.fabric.client.FabricMixedSlabModelPlugin;
import org.onenonly.bitsandbalance.fabric.client.FabricNavigatorCompass;
import org.onenonly.bitsandbalance.fabric.client.FabricBiomeItemTint;
import org.onenonly.bitsandbalance.fabric.client.FabricLeafLitterTint;

import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricGroupedConfigMigration;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricRapidFireJump;
import org.onenonly.bitsandbalance.fabric.network.FabricBioluminescenceSyncClient;
import org.onenonly.bitsandbalance.fabric.network.FabricSnowballNetworking;
import org.onenonly.bitsandbalance.fabric.network.FabricNavigatorCompassNetworkingClient;
import org.onenonly.bitsandbalance.fabric.network.FabricSittingNetworking;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricCoyoteTimeJump;
import org.onenonly.bitsandbalance.fabric.client.FabricRecoveryCompassClient;
import org.onenonly.bitsandbalance.fabric.client.FabricRecoveryCompassHudOverlay;
import org.onenonly.bitsandbalance.fabric.client.FabricUsageTickerClient;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabRendererCompat;
import org.onenonly.bitsandbalance.fabric.registry.FabricEntityTypes;
import org.onenonly.bitsandbalance.fabric.registry.FabricBlockEntities;
import org.onenonly.bitsandbalance.fabric.client.renderer.CandleBundleBlockEntityRenderer;
import org.onenonly.bitsandbalance.fabric.network.FabricCandleBundleNetworking;
import org.onenonly.bitsandbalance.fabric.network.FabricEnhancedSlabHangIntentNetworking;
import org.onenonly.bitsandbalance.fabric.network.FabricEnhancedSlabMaterialClientSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitsAndBalanceFabricClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-client");
    private static boolean lastJumpKeyPressedForCoyoteTime = false;

    @Override
    public void onInitializeClient() {
        FabricGroupedConfigMigration.migrateIfNeeded();
        FabricClientConfig.init();
        // Client reads mechanics config too (used by client-side logic like coyote time and chat mentions).
        FabricMechanicsConfig.init();
        KeybindModifierStore.init();
        ModWoodTypes.register();

        // Install log filter
        org.onenonly.bitsandbalance.common.client.FarChunkErrorFilter.install();

        // Ensure correct render layers for custom blocks that rely on cutout transparency.
        BlockRenderLayerMap.putBlock(ModAzalea.AZALEA_DOOR, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModAzalea.AZALEA_TRAPDOOR, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModGlowGoo.gooSplatter(), ChunkSectionLayer.CUTOUT);


        FabricSnowballNetworking.initClient();
        org.onenonly.bitsandbalance.fabric.network.FabricBiomeTitlesStructureSync.initClient();
        org.onenonly.bitsandbalance.fabric.network.FabricSecondChanceToastSync.initClient();
        FabricBioluminescenceSyncClient.initClient();
        FabricSittingNetworking.initClient();
        FabricNavigatorCompassNetworkingClient.initClient();
        FabricKeyBindings.initClient();
        FabricStanceClient.initClient();

        FabricAutoWalk.initClient();
        FabricDoorKnockClient.initClient();
        FabricNavigatorCompass.initClient();
        FabricRapidFireJump.initClient();

        // Initialize client-side features
        FabricRecoveryCompassClient.init();
        FabricRecoveryCompassHudOverlay.init();
        FabricUsageTickerClient.init();

        FabricLeafLitterTint.initClient();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> FabricBiomeItemTint.initClient());

        FabricMixedSlabModelPlugin.init();
        FabricDynamicVerticalStepModelPlugin.init();
        FabricEnhancedSlabMeshModelPlugin.init();
        bitsandbalance$registerEnhancedSlabRenderCacheReload();

        // Quad-level model wrapper so Enhanced Slab visual offsets still apply under Sodium/Indium.
        FabricEnhancedSlabModelOffsetPlugin.init();
        LOGGER.info("Enhanced slab chunk path: {}", EnhancedSlabRendererCompat.chunkPathStatus());

        FabricCandleBundleNetworking.initClient();
        FabricEnhancedSlabMaterialClientSync.initClient();
        // Draw deferred cloud fade geometry AFTER translucent terrain so
        // the cloud composites correctly over water and other translucent blocks.
        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.END_MAIN.register(
                context -> {
                    org.onenonly.bitsandbalance.common.client.CloudFadeRenderer.renderTrackedClouds(0.0F);
                    org.onenonly.bitsandbalance.common.client.CloudFadeRenderer.renderAllDeferred();
                }
        );

        // -- Render layers for transparent/translucent colored blocks --
        BlockRenderLayerMap.putBlock(ModBottleOfCloud.cloudBlock(), ChunkSectionLayer.TRANSLUCENT);

        BlockEntityRenderers.register(FabricBlockEntities.CANDLE_BUNDLE, CandleBundleBlockEntityRenderer::new);
        BlockEntityRenderers.register(FabricBlockEntities.VERTICAL_SLAB,
            org.onenonly.bitsandbalance.fabric.client.renderer.VerticalSlabBlockEntityRenderer::new);
        BlockEntityRenderers.register(FabricBlockEntities.STEP,
            org.onenonly.bitsandbalance.fabric.client.renderer.StepBlockEntityRenderer::new);
        BlockEntityRenderers.register(FabricBlockEntities.VERTICAL_STEP,
            org.onenonly.bitsandbalance.fabric.client.renderer.VerticalStepBlockEntityRenderer::new);
        FabricEnhancedSlabHangIntentNetworking.initClient();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            org.onenonly.bitsandbalance.common.client.YearnEchoClient.tick(client);
            org.onenonly.bitsandbalance.common.client.PonderLightAuditClient.tick(client);
            org.onenonly.bitsandbalance.common.validation.StonecutterRuntimeFixtureRunner.tick(client);
            org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager.tickClient();
            if (client.player != null) {
                handleCoyoteTimeJump(client.player);
            } else {
                lastJumpKeyPressedForCoyoteTime = false;
            }
        });

        EntityRendererRegistry.register(ModAzalea.AZALEA_BOAT_ENTITY_TYPE, context -> new AzaleaBoatRenderer(context, false));
        EntityRendererRegistry.register(ModAzalea.AZALEA_CHEST_BOAT_ENTITY_TYPE, context -> new AzaleaBoatRenderer(context, true));
        EntityRendererRegistry.register(FabricEntityTypes.SEAT, NoopRenderer::new);
        EntityRendererRegistry.register(ModGlowGoo.glowGooProjectileType(), ThrownItemRenderer::new);

        boolean usingOakFallback = ModWoodTypes.AZALEA_WOOD_TYPE == WoodType.OAK;
        LOGGER.info("BitsAndBalance client init: AZALEA wood type = {} (oakFallback={})", ModWoodTypes.AZALEA_WOOD_TYPE, usingOakFallback);
    }

    private static void bitsandbalance$registerEnhancedSlabRenderCacheReload() {
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
                .registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "enhanced_slab_render_caches");
                    }

                    @Override
                    public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
                        org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper.clearCaches();
                        org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabRenderModelCompat.clearCaches();
                        org.onenonly.bitsandbalance.fabric.client.renderer.StepBlockEntityRenderer.clearCaches();
                    }
                });
    }
    
    private static void handleCoyoteTimeJump(LocalPlayer player) {
        try {
            boolean jumpKeyPressed = Minecraft.getInstance().options.keyJump.isDown();
            boolean jumpKeyJustPressed = jumpKeyPressed && !lastJumpKeyPressedForCoyoteTime;
            lastJumpKeyPressedForCoyoteTime = jumpKeyPressed;

            if (!FabricMechanicsConfig.enableCoyoteTimeJump) return;

            FabricCoyoteTimeJump.updateClientState(player);

            // Never trigger coyote-time jumps from a held jump key; require a fresh key press.
            if (jumpKeyJustPressed && !player.onGround() && FabricCoyoteTimeJump.canUseCoyoteTime(player)) {
                FabricCoyoteTimeJump.performCoyoteTimeJump(player);
            }
        } catch (Throwable ignored) {
        }
    }

}

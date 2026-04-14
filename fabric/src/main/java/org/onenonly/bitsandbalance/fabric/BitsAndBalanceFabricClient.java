package org.onenonly.bitsandbalance.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.Identifier;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.content.ModAzalea;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.common.client.CloudFadeRenderer;
import org.onenonly.bitsandbalance.common.client.TemporaryCloudFadeRenderer;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.onenonly.bitsandbalance.common.registry.ModWoodTypes;
import org.onenonly.bitsandbalance.fabric.client.FabricAutoWalk;
import org.onenonly.bitsandbalance.fabric.client.FabricDoorKnockClient;
import org.onenonly.bitsandbalance.fabric.client.FabricKeyBindings;
import org.onenonly.bitsandbalance.fabric.client.FabricLeafLitterTint;
import org.onenonly.bitsandbalance.fabric.client.FabricNavigatorCompass;
import org.onenonly.bitsandbalance.fabric.client.FabricRecoveryCompassClient;
import org.onenonly.bitsandbalance.fabric.client.FabricRecoveryCompassHudOverlay;
import org.onenonly.bitsandbalance.fabric.client.FabricStanceClient;
import org.onenonly.bitsandbalance.fabric.client.FabricUsageTickerClient;
import org.onenonly.bitsandbalance.fabric.client.renderer.MixedSlabBlockEntityRenderer;
import org.onenonly.bitsandbalance.fabric.client.renderer.QuadStepBlockEntityRenderer;
import org.onenonly.bitsandbalance.fabric.client.renderer.QuadVerticalStepBlockEntityRenderer;
import org.onenonly.bitsandbalance.fabric.client.renderer.StepBlockEntityRenderer;
import org.onenonly.bitsandbalance.fabric.client.renderer.VerticalSlabBlockEntityRenderer;
import org.onenonly.bitsandbalance.fabric.client.renderer.VerticalStepBlockEntityRenderer;

import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricRapidFireJump;
import org.onenonly.bitsandbalance.fabric.network.FabricSnowballNetworking;
import org.onenonly.bitsandbalance.fabric.network.FabricSittingNetworking;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricCoyoteTimeJump;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;
import org.onenonly.bitsandbalance.fabric.registry.FabricEntityTypes;
import org.onenonly.bitsandbalance.fabric.registry.FabricBlockEntities;
import org.onenonly.bitsandbalance.fabric.network.FabricCandleBundleNetworking;
import org.onenonly.bitsandbalance.fabric.network.FabricBioluminescenceSync;
import org.onenonly.bitsandbalance.fabric.network.FabricNavigatorCompassNetworkingClient;
import org.onenonly.bitsandbalance.fabric.network.FabricSoulFireBurnSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BitsAndBalanceFabricClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-client");
    private static final Identifier AZALEA_BOAT_TEXTURE = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "textures/entity/boat/azalea.png");
    private static final Identifier AZALEA_CHEST_BOAT_TEXTURE = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "textures/entity/chest_boat/azalea.png");
    private static boolean lastJumpKeyPressedForCoyoteTime = false;

	private static boolean debugBool(String key) {
		return Boolean.getBoolean(key);
	}

    private static boolean bitsandbalance$classExists(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static BoatRenderer bitsandbalance$createRuntimeBoatRenderer(
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context,
            ModelLayerLocation modelLayer,
            Identifier texture,
            String description
    ) {
        BoatRenderer renderer = new BoatRenderer(context, modelLayer);
        try {
            Field textureField = bitsandbalance$findField(BoatRenderer.class, "texture");
            textureField.setAccessible(true);
            textureField.set(renderer, texture);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("Unable to override {} texture on runtime BoatRenderer; using the vanilla layer texture instead", description, exception);
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

    @Override
    public void onInitializeClient() {
        org.onenonly.bitsandbalance.fabric.config.FabricGroupedConfigMigration.migrateIfNeeded();
        FabricClientConfig.init();
        // Client reads mechanics config too (used by client-side logic like coyote time and chat mentions).
        FabricMechanicsConfig.init();
        KeybindModifierStore.init();
        ModWoodTypes.register();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> FabricLeafLitterTint.initClient());
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> bitsandbalance$disableCreateVanillaBellVisualization());

        // Install log filter
        org.onenonly.bitsandbalance.common.client.FarChunkErrorFilter.install();

        FabricSnowballNetworking.initClient();
        FabricSittingNetworking.initClient();
        FabricBioluminescenceSync.initClient();
        FabricSoulFireBurnSync.initClient();
        FabricKeyBindings.initClient();
        FabricAutoWalk.initClient();
        FabricNavigatorCompass.initClient();
        FabricRecoveryCompassClient.init();
        FabricRecoveryCompassHudOverlay.init();
        FabricUsageTickerClient.init();
        FabricStanceClient.initClient();

        FabricDoorKnockClient.initClient();
        FabricRapidFireJump.initClient();

        FabricCandleBundleNetworking.initClient();
        FabricNavigatorCompassNetworkingClient.initClient();
        LevelRenderEvents.END_MAIN.register(context -> {
            CloudFadeRenderer.renderAllDeferred();
            TemporaryCloudFadeRenderer.renderAllDeferred();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BioluminescenceLightManager.tickClient(client);
            if (client.player != null) {
                handleCoyoteTimeJump(client.player);
            } else {
                lastJumpKeyPressedForCoyoteTime = false;
            }
        });

        LOGGER.info("Registering Fabric azalea boats with the runtime BoatRenderer compatibility path");
        EntityRendererRegistry.register(
            ModAzalea.AZALEA_BOAT_ENTITY_TYPE,
            context -> bitsandbalance$createRuntimeBoatRenderer(context, ModelLayers.CHERRY_BOAT, AZALEA_BOAT_TEXTURE, "azalea boat")
        );
        EntityRendererRegistry.register(
            ModAzalea.AZALEA_CHEST_BOAT_ENTITY_TYPE,
            context -> bitsandbalance$createRuntimeBoatRenderer(context, ModelLayers.CHERRY_CHEST_BOAT, AZALEA_CHEST_BOAT_TEXTURE, "azalea chest boat")
        );
        EntityRendererRegistry.register(FabricEntityTypes.SEAT, NoopRenderer::new);
        EntityRendererRegistry.register(ModGlowGoo.glowGooProjectileType(), ThrownItemRenderer::new);

        bitsandbalance$registerOptionalBlockEntityRenderer(
            FabricBlockEntities.CANDLE_BUNDLE,
            "org.onenonly.bitsandbalance.fabric.client.renderer.CandleBundleBlockEntityRenderer",
            "candle bundle"
        );
        bitsandbalance$registerBlockEntityRenderer(FabricBlockEntities.TEMPORARY_CLOUD, org.onenonly.bitsandbalance.fabric.client.renderer.CandleBundleBlockEntityRenderer.TemporaryCloudRenderer::new);
        bitsandbalance$registerBlockEntityRenderer(FabricBlockEntities.MIXED_SLAB, MixedSlabBlockEntityRenderer::new);
        bitsandbalance$registerBlockEntityRenderer(FabricBlockEntities.VERTICAL_SLAB, VerticalSlabBlockEntityRenderer::new);
        bitsandbalance$registerBlockEntityRenderer(FabricBlockEntities.STEP, StepBlockEntityRenderer::new);
        bitsandbalance$registerBlockEntityRenderer(FabricBlockEntities.QUAD_STEP, QuadStepBlockEntityRenderer::new);
        bitsandbalance$registerBlockEntityRenderer(FabricBlockEntities.VERTICAL_STEP, VerticalStepBlockEntityRenderer::new);
        bitsandbalance$registerBlockEntityRenderer(FabricBlockEntities.QUAD_VERTICAL_STEP, QuadVerticalStepBlockEntityRenderer::new);

        boolean usingOakFallback = ModWoodTypes.AZALEA_WOOD_TYPE == WoodType.OAK;
        LOGGER.info("BitsAndBalance client init: AZALEA wood type = {} (oakFallback={})", ModWoodTypes.AZALEA_WOOD_TYPE, usingOakFallback);
    }
    
    private static void handleCoyoteTimeJump(LocalPlayer player) {
        try {
            boolean jumpKeyPressed = Minecraft.getInstance().options.keyJump.isDown();
            boolean jumpKeyJustPressed = jumpKeyPressed && !lastJumpKeyPressedForCoyoteTime;
            lastJumpKeyPressedForCoyoteTime = jumpKeyPressed;

            if (!FabricMechanicsConfig.enableCoyoteTimeJump) return;

            // Never trigger coyote-time jumps from a held jump key; require a fresh key press.
            if (jumpKeyJustPressed && !player.onGround() && FabricCoyoteTimeJump.canUseCoyoteTime(player)) {
                // Trigger the jump - the mixin will allow it through
                player.jumpFromGround();
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends BlockEntity, S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> void bitsandbalance$registerBlockEntityRenderer(
            BlockEntityType<? extends T> blockEntityType,
            BlockEntityRendererProvider<T, S> provider
    ) {
        try {
            Method method = BlockEntityRenderers.class.getDeclaredMethod("register", BlockEntityType.class, BlockEntityRendererProvider.class);
            method.setAccessible(true);
            method.invoke(null, blockEntityType, provider);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register Fabric block entity renderer for " + blockEntityType, exception);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bitsandbalance$registerOptionalBlockEntityRenderer(
            BlockEntityType<?> blockEntityType,
            String rendererClassName,
            String description
    ) {
        try {
            Class<?> rendererClass = Class.forName(rendererClassName, true, Thread.currentThread().getContextClassLoader());
            Constructor<?> constructor = rendererClass.getDeclaredConstructor(BlockEntityRendererProvider.Context.class);
            constructor.setAccessible(true);
            bitsandbalance$registerBlockEntityRenderer((BlockEntityType) blockEntityType, context -> {
                try {
                    return (net.minecraft.client.renderer.blockentity.BlockEntityRenderer) constructor.newInstance(context);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Failed to instantiate optional Fabric block entity renderer " + rendererClassName, exception);
                }
            });
        } catch (Throwable throwable) {
            LOGGER.warn("Skipping Fabric {} block entity renderer on the current 26.1 runtime surface", description, throwable);
        }
    }

    private static void bitsandbalance$disableCreateVanillaBellVisualization() {
        if (!FabricLoader.getInstance().isModLoaded("create")) {
            return;
        }

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> visualizerRegistryClass = Class.forName(
                    "com.zurrtum.create.client.flywheel.api.visualization.VisualizerRegistry",
                    false,
                    classLoader
            );
            Class<?> blockEntityVisualizerClass = Class.forName(
                    "com.zurrtum.create.client.flywheel.api.visualization.BlockEntityVisualizer",
                    false,
                    classLoader
            );
            Method setVisualizer = visualizerRegistryClass.getMethod(
                    "setVisualizer",
                    BlockEntityType.class,
                    blockEntityVisualizerClass
            );
            setVisualizer.invoke(null, BlockEntityType.BELL, null);
            LOGGER.info("Disabled Create vanilla bell visualization on Fabric so enhanced-slab bell offsets can use the live BellRenderer path");
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Failed to disable Create vanilla bell visualization on Fabric", exception);
        }
    }

}

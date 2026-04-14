package org.onenonly.bitsandbalance.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.onenonly.bitsandbalance.ModBlocks;
import org.onenonly.bitsandbalance.common.client.CloudFadeRenderer;
import org.onenonly.bitsandbalance.common.client.CloudRenderType;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabDestroyParticleHelper;
import org.onenonly.bitsandbalance.client.model.MixedSlabDynamicBlockStateModel;
import org.onenonly.bitsandbalance.registry.ModBlockEntities;
import org.onenonly.bitsandbalance.registry.ModWoodTypes;

import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.client.renderer.CandleBundleBlockEntityRenderer;
import org.onenonly.bitsandbalance.client.renderer.QuadStepBlockEntityRenderer;
import org.onenonly.bitsandbalance.client.renderer.QuadVerticalStepBlockEntityRenderer;
import org.onenonly.bitsandbalance.client.renderer.StepBlockEntityRenderer;
import org.onenonly.bitsandbalance.client.renderer.VerticalSlabBlockEntityRenderer;
import org.onenonly.bitsandbalance.client.renderer.VerticalStepBlockEntityRenderer;
import org.slf4j.Logger;

public class ClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final IClientBlockExtensions MIXED_SLAB_CLIENT_EXTENSIONS = new IClientBlockExtensions() {
        @Override
        public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
            if (!(level instanceof ClientLevel clientLevel)) return false;

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || !(target instanceof BlockHitResult blockHit)) return false;

            BlockPos pos = blockHit.getBlockPos();
            Direction direction = blockHit.getDirection();
            return EnhancedSlabDestroyParticleHelper.spawnHitForState(
                    clientLevel,
                    minecraft.player,
                    blockHit,
                    pos,
                    direction,
                    state
            );
        }
    };

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Install log filter
        org.onenonly.bitsandbalance.common.client.FarChunkErrorFilter.install();

        // Register render layers for blocks with transparency
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AZALEA_TRAPDOOR.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AZALEA_DOOR.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(ModBottleOfCloud.cloudBlock(), ChunkSectionLayer.TRANSLUCENT);
            LOGGER.info("Registered cutout render layers for Azalea trapdoor and door");

            // Register azalea wood type with Sheets to auto-handle sign textures and block entity types
            Sheets.addWoodType(ModWoodTypes.AZALEA_WOOD_TYPE);
            
            LOGGER.info("Registered Azalea wood type with Sheets for sign/hanging sign support");
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(CloudRenderType.CLOUD_PIPELINE);
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerBlock(MIXED_SLAB_CLIENT_EXTENSIONS, ModBlocks.MIXED_SLAB.get());
    }

    /**
     * Draws deferred cloud fade geometry AFTER translucent terrain so that
     * the cloud composites correctly over water and other translucent blocks.
     */
    public static void onAfterTranslucentBlocks(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        CloudFadeRenderer.renderAllDeferred();
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register custom block entity renderers for signs and hanging signs
        event.registerBlockEntityRenderer(ModBlockEntities.AZALEA_SIGN.get(), SignRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.AZALEA_HANGING_SIGN.get(), HangingSignRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CANDLE_BUNDLE.get(), CandleBundleBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.VERTICAL_SLAB.get(), VerticalSlabBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STEP.get(), StepBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.QUAD_STEP.get(), QuadStepBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.VERTICAL_STEP.get(), VerticalStepBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.QUAD_VERTICAL_STEP.get(), QuadVerticalStepBlockEntityRenderer::new);
        LOGGER.info("Registered sign and hanging sign renderers for Azalea wood type");
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        var mixedSlabBlock = ModBlocks.MIXED_SLAB.get();
        var blockStateModels = event.getBakingResult().blockStateModels();

        for (var state : mixedSlabBlock.getStateDefinition().getPossibleStates()) {
            BlockStateModel model = blockStateModels.get(state);
            if (model != null && !(model instanceof MixedSlabDynamicBlockStateModel)) {
                blockStateModels.put(state, new MixedSlabDynamicBlockStateModel(model));
            }
        }

        NeoForgeEnhancedSlabModelOffset.wrapAll(blockStateModels);
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        if (org.onenonly.bitsandbalance.Config.enableAutoWalkKey) {
            AutoWalkKeyHandler.registerKeybinding(event);
        }
    }

    /**
        * Tints leaf litter / leaf pile blocks based on the biome foliage color.
     * This is implemented as a lightweight client-side color handler and is compatible with other mods,
        * as long as their models use a tintindex in their faces.
     */
    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        if (!org.onenonly.bitsandbalance.ClientConfig.leafLitterTintEnabled) {
            LOGGER.info("Leaf litter/pile tint disabled in client config");
            return;
        }

        BlockColor biomeFoliageTint = (state, level, pos, tintIndex) -> {
            // Leaf litter should match oak leaves (foliage) tint.
            int defaultColor = FoliageColor.get(0.5D, 1.0D);

            int color;
            if (level != null && pos != null) {
                color = BiomeColors.getAverageFoliageColor(level, pos);
            } else {
                // Fallback when no world/pos is available (e.g., some model previews)
                color = defaultColor;
            }

            if (!org.onenonly.bitsandbalance.ClientConfig.leafLitterTintClampEnabled) {
                return color;
            }

            int min = org.onenonly.bitsandbalance.ClientConfig.leafLitterTintGlobalMinColorRgb;
            int max = org.onenonly.bitsandbalance.ClientConfig.leafLitterTintGlobalMaxColorRgb;

            if (level != null && pos != null) {
                if (level instanceof net.minecraft.world.level.LevelReader lr) {
                    var holder = lr.getBiome(pos);
                    boolean matched = false;

                    var keyOpt = holder.unwrapKey();
                    if (keyOpt.isPresent()) {
                        Identifier biomeId = keyOpt.get().identifier();
                        var range = org.onenonly.bitsandbalance.ClientConfig.leafLitterTintBiomeRanges.get(biomeId);
                        if (range != null && range.length >= 2) {
                            min = range[0];
                            max = range[1];
                            matched = true;
                        }
                    }

                    if (!matched) {
                        for (var tagRange : org.onenonly.bitsandbalance.ClientConfig.leafLitterTintBiomeTagRanges) {
                            if (tagRange == null || tagRange.tag() == null || tagRange.range() == null || tagRange.range().length < 2) {
                                continue;
                            }
                            if (holder.is(tagRange.tag())) {
                                min = tagRange.range()[0];
                                max = tagRange.range()[1];
                                break;
                            }
                        }
                    }
                }
            }

            if (min == -1 || max == -1) {
                return color;
            }

            return clampRgb(color, min, max);
        };

        int registered = 0;
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(entry.getValue());
            if (id == null) {
                continue;
            }

            boolean isVanillaLeafLitter = id.getNamespace().equals("minecraft") && id.getPath().equals("leaf_litter");
            boolean isLeafPile = id.getPath().contains("leaf_pile");

            if (isVanillaLeafLitter || isLeafPile) {
                event.register(biomeFoliageTint, entry.getValue());
                registered++;
            }
        }

        LOGGER.info("Registered biome tint for {} leaf litter/pile blocks", registered);

    }

    private static int clampRgb(int color, int min, int max) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int minR = (min >> 16) & 0xFF;
        int minG = (min >> 8) & 0xFF;
        int minB = min & 0xFF;

        int maxR = (max >> 16) & 0xFF;
        int maxG = (max >> 8) & 0xFF;
        int maxB = max & 0xFF;

        // Normalize if user accidentally swaps min/max components
        int loR = Math.min(minR, maxR);
        int hiR = Math.max(minR, maxR);
        int loG = Math.min(minG, maxG);
        int hiG = Math.max(minG, maxG);
        int loB = Math.min(minB, maxB);
        int hiB = Math.max(minB, maxB);

        r = Math.max(loR, Math.min(hiR, r));
        g = Math.max(loG, Math.min(hiG, g));
        b = Math.max(loB, Math.min(hiB, b));

        return (r << 16) | (g << 8) | b;
    }

    private static int lerpRgb(int a, int b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;

        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;

        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);

        return (r << 16) | (g << 8) | bl;
    }
}

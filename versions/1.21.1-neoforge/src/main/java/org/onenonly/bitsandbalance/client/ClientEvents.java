package org.onenonly.bitsandbalance.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.onenonly.bitsandbalance.ModBlocks;
import org.onenonly.bitsandbalance.common.client.CloudFadeRenderer;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabDestroyParticleHelper;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.registry.ModBlockEntities;
import org.onenonly.bitsandbalance.registry.ModWoodTypes;

import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.fabric.client.renderer.CandleBundleBlockEntityRenderer;
import org.onenonly.bitsandbalance.fabric.client.renderer.NeoForgeEnhancedSlabStaticBakedModel;
import org.onenonly.bitsandbalance.fabric.client.renderer.NeoForgeSodiumEnhancedSlabModelSupport;
import org.onenonly.bitsandbalance.fabric.client.renderer.NeoForgeEnhancedSlabStorageBakedModel;
import org.onenonly.bitsandbalance.fabric.client.renderer.StepBlockEntityRenderer;
import org.onenonly.bitsandbalance.fabric.client.renderer.VerticalSlabBlockEntityRenderer;
import org.onenonly.bitsandbalance.fabric.client.renderer.VerticalStepBlockEntityRenderer;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;

public class ClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float LEAF_LITTER_ITEM_LIGHTEN_FACTOR = 1.03F;

    private static final IClientBlockExtensions ENHANCED_SLAB_CLIENT_EXTENSIONS = new IClientBlockExtensions() {
        @Override
        public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
            if (!(level instanceof ClientLevel clientLevel)) return true;

            EnhancedSlabDestroyParticleHelper.spawnForState(
                    clientLevel,
                    Minecraft.getInstance().player,
                    pos,
                    state
            );
            return true;
        }

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
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AZALEA_TRAPDOOR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AZALEA_DOOR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModGlowGoo.gooSplatter(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBottleOfCloud.cloudBlock(), RenderType.translucent());
            LOGGER.info("Registered cutout render layers for Azalea trapdoor and door");

            // Register azalea wood type with Sheets to auto-handle sign textures and block entity types
            Sheets.addWoodType(ModWoodTypes.AZALEA_WOOD_TYPE);
            
            LOGGER.info("Registered Azalea wood type with Sheets for sign/hanging sign support");
        });
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerBlock(
                ENHANCED_SLAB_CLIENT_EXTENSIONS,
                ModBlocks.MIXED_SLAB.get(),
                ModBlocks.VERTICAL_SLAB.get(),
                ModBlocks.STEP.get(),
                ModBlocks.QUAD_STEP.get(),
                ModBlocks.VERTICAL_STEP.get(),
                ModBlocks.QUAD_VERTICAL_STEP.get()
        );
        registerEnhancedSlabExtensions(event, VerticalSlabDynamicRegistry.getAllVerticalBlocksSnapshot());
        registerEnhancedSlabExtensions(event, StepDynamicRegistry.getAllStepBlocksSnapshot());
        registerEnhancedSlabExtensions(event, VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot());
    }

    private static void registerEnhancedSlabExtensions(RegisterClientExtensionsEvent event, java.util.Collection<Block> blocks) {
        if (blocks.isEmpty()) return;
        event.registerBlock(ENHANCED_SLAB_CLIENT_EXTENSIONS, blocks.toArray(Block[]::new));
    }

    @SubscribeEvent
    public static void onAfterTranslucentBlocks(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            CloudFadeRenderer.renderAllDeferred();
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        org.onenonly.bitsandbalance.common.client.YearnEchoClient.tick(minecraft);
        org.onenonly.bitsandbalance.common.client.PonderLightAuditClient.tick(minecraft);
        org.onenonly.bitsandbalance.common.validation.StonecutterRuntimeFixtureRunner.tick(minecraft);
        if (minecraft.level != null) {
            EnhancedSlabCreateCompat.tickClientContraptionInvalidations(minecraft.level);
        }
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Regular azalea signs use vanilla BlockEntityType.SIGN; only hanging signs need a custom renderer.
        event.registerBlockEntityRenderer(ModBlockEntities.AZALEA_HANGING_SIGN.get(), HangingSignRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CANDLE_BUNDLE.get(), CandleBundleBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.VERTICAL_SLAB.get(), VerticalSlabBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STEP.get(), StepBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.VERTICAL_STEP.get(), VerticalStepBlockEntityRenderer::new);
        LOGGER.info("Registered hanging sign renderer for Azalea wood type");
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        org.onenonly.bitsandbalance.common.client.EnhancedSlabItemBakedModel.clearCache();
        org.onenonly.bitsandbalance.common.client.EnhancedSlabBlockBakedModel.clearCache();
        org.onenonly.bitsandbalance.common.client.ThatchHalfBlockBakedModel.clearCache();
        Map<ModelResourceLocation, BakedModel> models = event.getModels();

        registerStorageModel(models, ModBlocks.MIXED_SLAB.get(), NeoForgeEnhancedSlabStorageBakedModel.Kind.MIXED_SLAB);
        registerStorageModel(models, ModBlocks.VERTICAL_SLAB.get(), NeoForgeEnhancedSlabStorageBakedModel.Kind.VERTICAL_SLAB);
        registerStorageModel(models, ModBlocks.STEP.get(), NeoForgeEnhancedSlabStorageBakedModel.Kind.STEP);
        registerStorageModel(models, ModBlocks.QUAD_STEP.get(), NeoForgeEnhancedSlabStorageBakedModel.Kind.QUAD_STEP);
        registerStorageModel(models, ModBlocks.VERTICAL_STEP.get(), NeoForgeEnhancedSlabStorageBakedModel.Kind.VERTICAL_STEP);
        registerStorageModel(models, ModBlocks.QUAD_VERTICAL_STEP.get(), NeoForgeEnhancedSlabStorageBakedModel.Kind.QUAD_VERTICAL_STEP);

        registerStaticEnhancedSlabModels(models, VerticalSlabDynamicRegistry.getAllVerticalBlocksSnapshot());
        registerStaticEnhancedSlabModels(models, StepDynamicRegistry.getAllStepBlocksSnapshot());
        registerStaticEnhancedSlabModels(models, VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot());
    }

    private static void registerStorageModel(Map<ModelResourceLocation, BakedModel> models,
                                             Block block,
                                             NeoForgeEnhancedSlabStorageBakedModel.Kind kind) {
        if (models == null || block == null || kind == null) {
            return;
        }

        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
            BakedModel originalModel = models.get(location);
            if (originalModel != null) {
                models.put(location, NeoForgeSodiumEnhancedSlabModelSupport.wrapStorage(originalModel, kind));
            }
        }
    }

    private static void registerStaticEnhancedSlabModels(Map<ModelResourceLocation, BakedModel> models,
                                                        Collection<Block> blocks) {
        if (models == null || blocks == null || blocks.isEmpty()) {
            return;
        }

        for (Block block : blocks) {
            EnhancedSlabItemSupport.ModelType modelType = EnhancedSlabItemSupport.modelTypeForBlock(block);
            if (modelType == EnhancedSlabItemSupport.ModelType.NONE) {
                continue;
            }

            Block sourceSlab = EnhancedSlabItemSupport.sourceSlabForBlock(block);
            BlockState sourceState = sourceSlab == null ? null : DynamicVariantMaterialHelper.normalizeSlabState(sourceSlab.defaultBlockState());
            if (sourceState == null) {
                continue;
            }

            BlockState geometryState = SlabFamilyModelSourceSelector.thatchGeometrySourceState(sourceState, modelType);
            BakedModel sourceModel = models.get(BlockModelShaper.stateToModelLocation(geometryState));
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
                BakedModel originalModel = models.get(location);
                if (originalModel != null) {
                    models.put(location, NeoForgeSodiumEnhancedSlabModelSupport.wrapStatic(originalModel, sourceModel, sourceState, modelType));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        if (org.onenonly.bitsandbalance.Config.enableAutoWalkKey) {
            AutoWalkKeyHandler.registerKeybinding(event);
        }
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        NeoForgeBiomeItemTint.register(event);
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
                        ResourceLocation biomeId = keyOpt.get().location();
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
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(entry.getValue());
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

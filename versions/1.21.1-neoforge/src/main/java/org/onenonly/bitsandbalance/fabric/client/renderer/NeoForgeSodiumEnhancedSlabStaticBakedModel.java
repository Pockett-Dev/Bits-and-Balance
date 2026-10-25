package org.onenonly.bitsandbalance.fabric.client.renderer;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabBlockBakedModel;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabFabricRenderUtil;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.client.ThatchHalfBlockBakedModel;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport;

import java.util.List;
import java.util.function.Supplier;

public final class NeoForgeSodiumEnhancedSlabStaticBakedModel extends BakedModelWrapper<BakedModel> implements FabricBakedModel {
    private final BakedModel sourceModel;
    private final BlockState sourceSlabState;
    private final EnhancedSlabItemSupport.ModelType modelType;
    private volatile BakedModel enhancedModel;

    private NeoForgeSodiumEnhancedSlabStaticBakedModel(BakedModel originalModel,
                                                       BakedModel sourceModel,
                                                       BlockState sourceSlabState,
                                                       EnhancedSlabItemSupport.ModelType modelType) {
        super(originalModel);
        this.sourceModel = sourceModel == null ? originalModel : sourceModel;
        this.sourceSlabState = sourceSlabState;
        this.modelType = modelType;
        if (!SlabFamilyModelSourceSelector.isUpgradeAquaticThatchSlab(sourceSlabState.getBlock())) {
            this.enhancedModel = EnhancedSlabBlockBakedModel.wrap(this.sourceModel, modelType);
        }
    }

    public static BakedModel wrap(BakedModel originalModel,
                                  BakedModel sourceModel,
                                  BlockState sourceSlabState,
                                  EnhancedSlabItemSupport.ModelType modelType) {
        if (originalModel == null || sourceSlabState == null || modelType == EnhancedSlabItemSupport.ModelType.NONE) {
            return originalModel;
        }
        return new NeoForgeSodiumEnhancedSlabStaticBakedModel(originalModel, sourceModel, sourceSlabState, modelType);
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter world,
                               BlockState state,
                               BlockPos pos,
                               Supplier<RandomSource> randomSupplier,
                               RenderContext context) {
        if (!Config.enableEnhancedSlabs || context == null) {
            emitFallback(world, state, pos, randomSupplier, context);
            return;
        }

        BlockState sourceState = EnhancedSlabFabricRenderUtil.sourceRenderStateForOwner(state);
        RenderMaterial material = EnhancedSlabFabricRenderUtil.materialFor(sourceState);
        if (sourceState == null || material == null) {
            emitFallback(world, state, pos, randomSupplier, context);
            return;
        }
        RandomSource random = randomSupplier == null ? safeRandom(null) : safeRandom(randomSupplier.get());
        for (net.minecraft.client.renderer.block.model.BakedQuad quad : renderModel(state).getQuads(state, null, random)) {
            EnhancedSlabFabricRenderUtil.emitQuad(context, quad, material, world, pos, sourceState, null);
        }
    }

    @Override
    public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return renderModel(state).getQuads(state, side, random);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData modelData) {
        ChunkRenderTypeSet renderTypes = this.sourceModel.getRenderTypes(this.sourceSlabState, safeRandom(random), ModelData.EMPTY);
        return renderTypes == null || renderTypes.isEmpty()
                ? this.originalModel.getRenderTypes(state, safeRandom(random), modelData)
                : renderTypes;
    }

    @Override
    public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(BlockState state,
                                                                              Direction side,
                                                                              RandomSource random,
                                                                              ModelData modelData,
                                                                              RenderType renderType) {
        return side == null ? renderModel(state).getQuads(state, null, safeRandom(random)) : List.of();
    }

    private BakedModel renderModel(BlockState state) {
        if (this.modelType == EnhancedSlabItemSupport.ModelType.VERTICAL_SLAB
                && SlabFamilyModelSourceSelector.isUpgradeAquaticThatchSlab(this.sourceSlabState.getBlock())
                && state != null && state.hasProperty(VerticalSlabBlock.DOUBLE)
                && state.getValue(VerticalSlabBlock.DOUBLE)) {
            BlockState fullState = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(
                    this.sourceSlabState, this.sourceSlabState);
            if (fullState != null) {
                BakedModel fullModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(fullState);
                return ThatchHalfBlockBakedModel.wrapFull(fullModel, fullState);
            }
        }
        return enhancedModel();
    }

    private BakedModel enhancedModel() {
        BakedModel model = this.enhancedModel;
        if (model == null) {
            synchronized (this) {
                model = this.enhancedModel;
                if (model == null) {
                    boolean preserve = SlabFamilyModelSourceSelector.isUpgradeAquaticThatchSlab(this.sourceSlabState.getBlock());
                    model = EnhancedSlabBlockBakedModel.wrap(this.sourceModel, this.modelType, preserve, null,
                            EnhancedSlabBlockBakedModel.topStepSourceModel(this.sourceSlabState, this.modelType, preserve));
                    this.enhancedModel = model;
                }
            }
        }
        return model;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.sourceModel.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData modelData) {
        return this.sourceModel.getParticleIcon(ModelData.EMPTY);
    }

    private void emitFallback(BlockAndTintGetter world,
                              BlockState state,
                              BlockPos pos,
                              Supplier<RandomSource> randomSupplier,
                              RenderContext context) {
        if (this.originalModel instanceof FabricBakedModel fabricModel) {
            fabricModel.emitBlockQuads(world, state, pos, randomSupplier, context);
        } else if (context != null) {
            context.bakedModelConsumer().accept(this.originalModel, state);
        }
    }

    private static RandomSource safeRandom(RandomSource random) {
        if (random != null) {
            return random;
        }
        RandomSource seeded = RandomSource.create();
        seeded.setSeed(42L);
        return seeded;
    }
}

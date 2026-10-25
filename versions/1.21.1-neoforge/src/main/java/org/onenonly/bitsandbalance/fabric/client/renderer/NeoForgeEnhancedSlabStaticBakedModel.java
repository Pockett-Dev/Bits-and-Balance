package org.onenonly.bitsandbalance.fabric.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.fml.ModList;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabBlockBakedModel;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.client.ThatchHalfBlockBakedModel;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoForgeEnhancedSlabStaticBakedModel extends BakedModelWrapper<BakedModel> {
    private final BakedModel sourceModel;
    private final BlockState sourceSlabState;
    private final EnhancedSlabItemSupport.ModelType modelType;
    private final Map<CacheKey, List<BakedQuad>> quadsByState = new ConcurrentHashMap<>();

    private NeoForgeEnhancedSlabStaticBakedModel(BakedModel originalModel,
                                                 BakedModel sourceModel,
                                                 BlockState sourceSlabState,
                                                 EnhancedSlabItemSupport.ModelType modelType) {
        super(originalModel);
        this.sourceModel = sourceModel;
        this.sourceSlabState = sourceSlabState;
        this.modelType = modelType;
    }

    public static BakedModel wrap(BakedModel originalModel,
                                  BakedModel sourceModel,
                                  BlockState sourceSlabState,
                                  EnhancedSlabItemSupport.ModelType modelType) {
        if (originalModel == null || sourceSlabState == null || modelType == EnhancedSlabItemSupport.ModelType.NONE) {
            return originalModel;
        }
        return new NeoForgeEnhancedSlabStaticBakedModel(
                originalModel,
                sourceModel == null ? originalModel : sourceModel,
                sourceSlabState,
                modelType
        );
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        if (!Config.enableEnhancedSlabs) {
            return List.of();
        }
        List<BakedQuad> quads = quadsFor(state, null);
        return useSodiumFaceBuckets() ? filteredQuads(quads, side) : side == null ? quads : List.of();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData modelData, RenderType renderType) {
        if (!Config.enableEnhancedSlabs || !rendersIn(renderType, random)) {
            return List.of();
        }
        List<BakedQuad> quads = quadsFor(state, renderType);
        return useSodiumFaceBuckets() ? filteredQuads(quads, side) : side == null ? quads : List.of();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData modelData) {
        ChunkRenderTypeSet renderTypes = this.sourceModel.getRenderTypes(this.sourceSlabState, safeRandom(random), ModelData.EMPTY);
        return renderTypes == null || renderTypes.isEmpty()
                ? this.originalModel.getRenderTypes(state, safeRandom(random), modelData)
                : renderTypes;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.sourceModel.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData modelData) {
        return this.sourceModel.getParticleIcon(ModelData.EMPTY);
    }

    private List<BakedQuad> quadsFor(BlockState state, RenderType renderType) {
        BlockState ownerState = state == null ? null : state;
        return this.quadsByState.computeIfAbsent(new CacheKey(ownerState, renderType), ignored -> buildQuads(ownerState));
    }

    private List<BakedQuad> buildQuads(BlockState ownerState) {
        boolean preserveSourceGeometry = SlabFamilyModelSourceSelector.isUpgradeAquaticThatchSlab(this.sourceSlabState.getBlock());
        if (preserveSourceGeometry && this.modelType == EnhancedSlabItemSupport.ModelType.VERTICAL_SLAB
                && ownerState != null && ownerState.hasProperty(VerticalSlabBlock.DOUBLE)
                && ownerState.getValue(VerticalSlabBlock.DOUBLE)) {
            BlockState fullState = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(
                    this.sourceSlabState, this.sourceSlabState);
            if (fullState != null) {
                BakedModel fullModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(fullState);
                return ThatchHalfBlockBakedModel.wrapFull(fullModel, fullState).getQuads(null, null, seededRandom());
            }
        }
        BakedModel partModel = EnhancedSlabBlockBakedModel.wrap(
                this.sourceModel,
                this.modelType,
                preserveSourceGeometry,
                null,
                EnhancedSlabBlockBakedModel.topStepSourceModel(this.sourceSlabState, this.modelType, preserveSourceGeometry)
        );
        if (partModel == null) {
            return List.of();
        }
        List<BakedQuad> quads = partModel.getQuads(ownerState, null, seededRandom());
        return quads == null || quads.isEmpty() ? List.of() : List.copyOf(quads);
    }

    private boolean rendersIn(RenderType renderType, RandomSource random) {
        if (renderType == null) {
            return true;
        }
        ChunkRenderTypeSet renderTypes = this.sourceModel.getRenderTypes(this.sourceSlabState, safeRandom(random), ModelData.EMPTY);
        return renderTypes == null || renderTypes.isEmpty() || renderTypes.contains(renderType);
    }

    private static List<BakedQuad> filteredQuads(List<BakedQuad> quads, Direction side) {
        if (quads == null || quads.isEmpty() || side == null) {
            return List.of();
        }

        List<BakedQuad> filtered = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            if (quad != null && quad.getDirection() == side) {
                filtered.add(quad);
            }
        }
        return filtered.isEmpty() ? List.of() : List.copyOf(filtered);
    }

    private static boolean useSodiumFaceBuckets() {
        return ModList.get().isLoaded("sodium");
    }

    private static RandomSource safeRandom(RandomSource random) {
        return random == null ? seededRandom() : random;
    }

    private static RandomSource seededRandom() {
        RandomSource random = RandomSource.create();
        random.setSeed(42L);
        return random;
    }

    private record CacheKey(BlockState state, RenderType renderType) {
    }
}

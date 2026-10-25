package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabFabricRenderUtil;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FabricMixedSlabModelPlugin {
    private FabricMixedSlabModelPlugin() {
    }

    public static void init() {
        ModelLoadingPlugin.register(pluginContext -> {
            Block mixedSlabBlock = CommonBlocks.MIXED_SLAB;
            if (mixedSlabBlock == null) {
                return;
            }

            ResourceLocation fallbackModelId = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "block/mixed_slab");
            pluginContext.registerBlockStateResolver(mixedSlabBlock, context -> {
                UnbakedModel model = new MixedSlabBlockUnbakedModel(fallbackModelId);
                for (BlockState state : mixedSlabBlock.getStateDefinition().getPossibleStates()) {
                    context.setModel(state, model);
                }
            });
        });
    }

    private record MixedSlabBlockUnbakedModel(ResourceLocation fallbackModelId) implements UnbakedModel {
        @Override
        public Collection<ResourceLocation> getDependencies() {
            return List.of(this.fallbackModelId);
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
            UnbakedModel fallbackModel = resolver.apply(this.fallbackModelId);
            if (fallbackModel != null) {
                fallbackModel.resolveParents(resolver);
            }
        }

        @Override
        public BakedModel bake(ModelBaker baker,
                               Function<Material, TextureAtlasSprite> textureGetter,
                               ModelState modelState) {
            return MixedSlabBlockBakedModel.wrap(baker.bake(this.fallbackModelId, modelState));
        }
    }

    private static final class MixedSlabBlockBakedModel extends ForwardingBakedModel implements FabricBakedModel {
        private MixedSlabBlockBakedModel(BakedModel delegate) {
            this.wrapped = delegate;
        }

        static BakedModel wrap(BakedModel delegate) {
            if (delegate == null) {
                return null;
            }
            return new MixedSlabBlockBakedModel(delegate);
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
            if (!FabricTweaksConfig.enableEnhancedSlabs) {
                super.emitBlockQuads(world, state, pos, randomSupplier, context);
                return;
            }

            Object slabs = EnhancedSlabTerrainRenderHelper.materialSource(world, pos, state);
            BlockState bottom = halfState(slot(slabs, 0), SlabType.BOTTOM);
            BlockState top = halfState(slot(slabs, 1), SlabType.TOP);
            if (bottom == null && top == null) {
                super.emitBlockQuads(world, state, pos, randomSupplier, context);
                return;
            }

            BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
            boolean emitted = false;
            if (bottom != null) {
                emitted |= emitHalf(dispatcher, bottom, world, pos, randomSupplier.get(), context, EnhancedSlabTerrainRenderHelper.mixedBottomHiddenFace(dispatcher, bottom, top));
            }
            if (top != null) {
                emitted |= emitHalf(dispatcher, top, world, pos, randomSupplier.get(), context, bottom != null ? Direction.DOWN : null);
            }

            if (!emitted) {
                super.emitBlockQuads(world, state, pos, randomSupplier, context);
            }
        }

        private static boolean emitHalf(BlockRenderDispatcher dispatcher,
                                        BlockState halfState,
                                        BlockAndTintGetter world,
                                        BlockPos pos,
                                        RandomSource random,
                                        RenderContext context,
                                        Direction hiddenFace) {
            RenderMaterial material = EnhancedSlabFabricRenderUtil.materialFor(halfState);
            if (material == null) {
                return false;
            }

            BakedModel model = dispatcher.getBlockModel(halfState);
            if (model == null) {
                return false;
            }

            boolean emitted = false;
            emitted |= emitQuads(model.getQuads(halfState, null, random), context, material, world, pos, halfState, hiddenFace);
            for (Direction direction : Direction.values()) {
                if (direction == hiddenFace) {
                    continue;
                }
                emitted |= emitQuads(model.getQuads(halfState, direction, random), context, material, world, pos, halfState, hiddenFace);
            }
            return emitted;
        }

        private static boolean emitQuads(List<BakedQuad> quads,
                                         RenderContext context,
                                         RenderMaterial material,
                                         BlockAndTintGetter world,
                                         BlockPos pos,
                                         BlockState tintState,
                                         Direction hiddenFace) {
            if (quads == null || quads.isEmpty()) {
                return false;
            }

            boolean emitted = false;
            for (BakedQuad quad : quads) {
                if (quad == null || quad.getDirection() == hiddenFace) {
                    continue;
                }
                emitted |= EnhancedSlabFabricRenderUtil.emitQuad(context, quad, material, world, pos, tintState, null);
            }
            return emitted;
        }

        private static BlockState halfState(BlockState state, SlabType half) {
            if (state == null || state.isAir()) {
                return null;
            }

            BlockState result = state;
            if (result.hasProperty(SlabBlock.TYPE)) {
                result = result.setValue(SlabBlock.TYPE, half);
            }
            if (result.hasProperty(SlabBlock.WATERLOGGED)) {
                result = result.setValue(SlabBlock.WATERLOGGED, false);
            }
            return result;
        }

        private static BlockState slot(Object slabs, int index) {
            return EnhancedSlabTerrainRenderHelper.materialSlot(slabs, index);
        }
    }
}

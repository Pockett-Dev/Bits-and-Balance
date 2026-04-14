package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.client.MixedSlabClientModelUtil;

import java.util.List;
import java.util.function.Predicate;

public final class FabricMixedSlabModelPlugin {

    private FabricMixedSlabModelPlugin() {
    }

    public static void init() {
        ModelLoadingPlugin.register(pluginContext ->
                pluginContext.modifyBlockModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE,
                        (model, context) -> {
                            BlockState state = context.state();
                            if (state != null && state.getBlock() instanceof MixedSlabBlock) {
                                return new MixedSlabBlockStateModel(model);
                            }
                            return model;
                        }));
    }

    private static final class MixedSlabBlockStateModel implements BlockStateModel {
        private final BlockStateModel delegate;

        private MixedSlabBlockStateModel(BlockStateModel delegate) {
            this.delegate = delegate;
        }

        @Override
        public void collectParts(RandomSource random, List<BlockModelPart> parts) {
            delegate.collectParts(random, parts);
        }

        @Override
        public TextureAtlasSprite particleIcon() {
            return delegate.particleIcon();
        }

        @Override
        public void emitQuads(QuadEmitter emitter,
                              BlockAndTintGetter world,
                              BlockPos pos,
                              BlockState state,
                              RandomSource random,
                              Predicate<Direction> faceFilter) {
            MixedSlabClientModelUtil.Halves halves = MixedSlabClientModelUtil.halvesFromBlockEntity(world, pos);
            if (halves == null) {
                delegate.emitQuads(emitter, world, pos, state, random, faceFilter);
                return;
            }

            long seed = pos.asLong();
            BlockStateModel bottomModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(halves.bottom());
            BlockStateModel topModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(halves.top());
            bottomModel.emitQuads(emitter, world, pos, halves.bottom(), RandomSource.create(seed), faceFilter);
            topModel.emitQuads(emitter, world, pos, halves.top(), RandomSource.create(MixedSlabClientModelUtil.topSeed(seed)), faceFilter);
        }

        @Override
        public Object createGeometryKey(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random) {
            MixedSlabClientModelUtil.Halves halves = MixedSlabClientModelUtil.halvesFromBlockEntity(world, pos);
            return halves != null ? MixedSlabClientModelUtil.geometryKey(halves) : delegate.createGeometryKey(world, pos, state, random);
        }

        @Override
        public TextureAtlasSprite particleSprite(BlockAndTintGetter world, BlockPos pos, BlockState state) {
            MixedSlabClientModelUtil.Halves halves = MixedSlabClientModelUtil.halvesFromBlockEntity(world, pos);
            if (halves == null) {
                return delegate.particleSprite(world, pos, state);
            }
            return Minecraft.getInstance().getBlockRenderer().getBlockModel(halves.bottom()).particleSprite(world, pos, halves.bottom());
        }
    }
}
package org.onenonly.bitsandbalance.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.onenonly.bitsandbalance.common.client.MixedSlabClientModelUtil;

import java.util.List;

public final class MixedSlabDynamicBlockStateModel extends DelegateBlockStateModel implements DynamicBlockStateModel {
    public MixedSlabDynamicBlockStateModel(BlockStateModel delegate) {
        super(delegate);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts) {
        MixedSlabClientModelUtil.Halves halves = MixedSlabModelData.halvesFromModelData(level, pos);
        if (halves == null) {
            halves = MixedSlabClientModelUtil.halvesFromBlockEntity(level, pos);
        }
        if (halves == null) {
            delegate.collectParts(level, pos, state, random, parts);
            return;
        }

        long seed = pos.asLong();
        MixedSlabClientModelUtil.appendParts(parts, halves.bottom(), seed);
        MixedSlabClientModelUtil.appendParts(parts, halves.top(), MixedSlabClientModelUtil.topSeed(seed));
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        MixedSlabClientModelUtil.Halves halves = MixedSlabModelData.halvesFromModelData(level, pos);
        if (halves == null) {
            halves = MixedSlabClientModelUtil.halvesFromBlockEntity(level, pos);
        }
        return halves != null ? MixedSlabClientModelUtil.geometryKey(halves) : delegate.createGeometryKey(level, pos, state, random);
    }

    @Override
    public TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        MixedSlabClientModelUtil.Halves halves = MixedSlabModelData.halvesFromModelData(level, pos);
        if (halves == null) {
            halves = MixedSlabClientModelUtil.halvesFromBlockEntity(level, pos);
        }
        if (halves == null) {
            return delegate.particleIcon(level, pos, state);
        }
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(halves.bottom()).particleIcon(level, pos, halves.bottom());
    }
}
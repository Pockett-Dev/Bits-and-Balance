package org.onenonly.bitsandbalance.client.model;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.client.MixedSlabClientModelUtil;

public final class MixedSlabModelData {
    public static final ModelProperty<BlockState> BOTTOM_SLAB = new ModelProperty<>();
    public static final ModelProperty<BlockState> TOP_SLAB = new ModelProperty<>();

    private MixedSlabModelData() {
    }

    public static ModelData of(BlockState bottomSlab, BlockState topSlab) {
        return ModelData.builder()
                .with(BOTTOM_SLAB, bottomSlab)
                .with(TOP_SLAB, topSlab)
                .build();
    }

    public static @Nullable MixedSlabClientModelUtil.Halves halvesFromModelData(BlockAndTintGetter level, BlockPos pos) {
        if (level == null || pos == null) return null;

        ModelData modelData = level.getModelData(pos);
        if (!modelData.has(BOTTOM_SLAB) || !modelData.has(TOP_SLAB)) {
            return null;
        }

        BlockState bottomSlab = modelData.get(BOTTOM_SLAB);
        BlockState topSlab = modelData.get(TOP_SLAB);
        if (bottomSlab == null || topSlab == null) {
            return null;
        }

        return new MixedSlabClientModelUtil.Halves(bottomSlab, topSlab);
    }
}
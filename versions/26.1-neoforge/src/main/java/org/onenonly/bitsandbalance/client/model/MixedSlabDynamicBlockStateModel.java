package org.onenonly.bitsandbalance.client.model;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;

public final class MixedSlabDynamicBlockStateModel extends DelegateBlockStateModel {
    public MixedSlabDynamicBlockStateModel(BlockStateModel delegate) {
        super(delegate);
    }
}

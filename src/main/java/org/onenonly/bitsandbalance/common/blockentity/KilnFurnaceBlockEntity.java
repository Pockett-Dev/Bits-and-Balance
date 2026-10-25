package org.onenonly.bitsandbalance.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Uses the vanilla furnace inventory, menu, automation, fuel, and XP logic. */
public final class KilnFurnaceBlockEntity extends FurnaceBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("container.bitsandbalance.kiln");

    public KilnFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }
}

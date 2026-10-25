package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
//? if mc != 1.21.1
import net.minecraft.util.ProblemReporter;
//? endif
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if mc != 1.21.1
import net.minecraft.world.level.storage.TagValueInput;
//? endif
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;

/** Shared standard block-entity NBT path used by schematic/chunk-copy regression tests. */
public final class EnhancedSlabPortableNbt {
    private EnhancedSlabPortableNbt() {
    }

    public static @Nullable CompoundTag capture(Level level, BlockPos pos) {
        BlockEntity blockEntity = level == null || pos == null ? null : level.getBlockEntity(pos);
        return blockEntity == null ? null : blockEntity.saveWithoutMetadata(level.registryAccess());
    }

    public static boolean apply(Level level, BlockPos pos, BlockState state, CompoundTag tag) {
        if (level == null || pos == null || state == null || tag == null || tag.isEmpty()
                || !(state.getBlock() instanceof EntityBlock entityBlock)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || !blockEntity.isValidBlockState(state)) {
            if (blockEntity != null) {
                level.removeBlockEntity(pos);
            }
            blockEntity = entityBlock.newBlockEntity(pos, state);
            if (blockEntity == null) {
                return false;
            }
            blockEntity.setLevel(level);
            level.setBlockEntity(blockEntity);
        }

        //? if mc == 1.21.1
        blockEntity.loadWithComponents(tag.copy(), level.registryAccess());
        //? else
        blockEntity.loadWithComponents(TagValueInput.create(
                ProblemReporter.DISCARDING, level.registryAccess(), tag.copy()));
        //? endif
        blockEntity.setChanged();
        EnhancedSlabMaterialSavedData.restoreFromBlockEntity(level, blockEntity);
        if (!level.isClientSide()) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
        return true;
    }
}

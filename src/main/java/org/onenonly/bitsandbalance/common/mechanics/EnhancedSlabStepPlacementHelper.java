package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;

public final class EnhancedSlabStepPlacementHelper {
    private EnhancedSlabStepPlacementHelper() {
    }

    public static BlockState buildStepPlacingSlabState(Block slabBlock) {
        return normalizePlacingSlabState(slabBlock.defaultBlockState());
    }

    public static BlockState buildVerticalStepPlacingSlabState(Block sourceVerticalSlab) {
        Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
        BlockState state = sourceSlab != null ? sourceSlab.defaultBlockState() : sourceVerticalSlab.defaultBlockState();
        return normalizePlacingSlabState(state);
    }

    /** Query the real source material at placement time, not a registration-time sound copy. */
    public static BlockState placementSoundState(BlockState state) {
        if (state.getBlock() instanceof FixedStepBlock step && step.getSourceSlab() != null) {
            return buildStepPlacingSlabState(step.getSourceSlab());
        }
        if (state.getBlock() instanceof FixedVerticalStepBlock step && step.getSourceVerticalSlab() != null) {
            return buildVerticalStepPlacingSlabState(step.getSourceVerticalSlab());
        }
        return state;
    }

    public static @Nullable BlockState[] getStoredSlots(Level level, BlockPos pos) {
        return EnhancedSlabMaterialSavedData.get(level, pos.asLong());
    }

    public static BlockState[] copySlots(@Nullable BlockState[] stored) {
        BlockState[] copy = new BlockState[4];
        if (stored != null) {
            System.arraycopy(stored, 0, copy, 0, Math.min(4, stored.length));
        }
        return copy;
    }

    public static BlockState[] copyOrCreateStepSlots(@Nullable BlockState[] stored, @Nullable BlockEntity blockEntity) {
        BlockState[] copy = copySlots(stored);
        if (blockEntity instanceof QuadStepBlockEntity quad) {
            for (int i = 0; i < 4; i++) {
                if (copy[i] == null) {
                    copy[i] = quad.getSlabAt(i);
                }
            }
        }
        return copy;
    }

    public static BlockState[] copyOrCreateVerticalStepSlots(@Nullable BlockState[] stored, @Nullable BlockEntity blockEntity) {
        BlockState[] copy = copySlots(stored);
        if (blockEntity instanceof QuadVerticalStepBlockEntity quad) {
            for (int i = 0; i < 4; i++) {
                if (copy[i] == null) {
                    copy[i] = quad.getSlabAt(i);
                }
            }
        }
        return copy;
    }

    public static int countSlots(BlockState[] slots) {
        int count = 0;
        for (BlockState slot : slots) {
            if (slot != null) {
                count++;
            }
        }
        return count;
    }

    public static @Nullable BlockState getStepSlot(@Nullable BlockState[] stored, @Nullable BlockEntity blockEntity, int index) {
        if (index < 0 || index >= 4) {
            return null;
        }
        if (stored != null && index < stored.length) {
            return stored[index];
        }
        return blockEntity instanceof QuadStepBlockEntity quad ? quad.getSlabAt(index) : null;
    }

    public static @Nullable BlockState getVerticalStepSlot(@Nullable BlockState[] stored, @Nullable BlockEntity blockEntity, int index) {
        if (index < 0 || index >= 4) {
            return null;
        }
        if (stored != null && index < stored.length) {
            return stored[index];
        }
        return blockEntity instanceof QuadVerticalStepBlockEntity quad ? quad.getSlabAt(index) : null;
    }

    public static void playPlaceSound(Level level, BlockPos pos, @Nullable BlockState preferredState, @Nullable BlockState fallbackState) {
        try {
            BlockState soundState = preferredState != null ? preferredState : fallbackState;
            if (soundState == null) {
                return;
            }
            SoundType sound = placementSoundState(soundState).getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(),
                    SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        } catch (Throwable ignored) {
        }
    }

    private static BlockState normalizePlacingSlabState(BlockState state) {
        if (state.hasProperty(SlabBlock.TYPE)) {
            state = state.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (state.hasProperty(SlabBlock.WATERLOGGED)) {
            state = state.setValue(SlabBlock.WATERLOGGED, false);
        }
        return state;
    }
}

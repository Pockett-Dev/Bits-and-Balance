package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

public final class EnhancedSlabLegacyTagMigration {
    private static final String TAG_ID = "id";
    private static final String TAG_NAME = "Name";
    private static final String TAG_PROPERTIES = "Properties";
    private static final String TAG_BOTTOM_SLAB = "BottomSlab";
    private static final String TAG_TOP_SLAB = "TopSlab";
    private static final String TAG_FACING_SLAB = "FacingSlab";
    private static final String TAG_OPPOSITE_SLAB = "OppositeSlab";
    private static final String TAG_SLAB = "Slab";
    private static final String[] SLOT_TAGS = {"Slot0", "Slot1", "Slot2", "Slot3"};

    private static final String ID_MIXED_SLAB = "bitsandbalance:mixed_slab";
    private static final String ID_VERTICAL_SLAB = "bitsandbalance:vertical_slab";
    private static final String ID_STEP = "bitsandbalance:step";
    private static final String ID_QUAD_STEP = "bitsandbalance:quad_step";
    private static final String ID_VERTICAL_STEP = "bitsandbalance:vertical_step";
    private static final String ID_QUAD_VERTICAL_STEP = "bitsandbalance:quad_vertical_step";

    private static final Block DEFAULT_SLAB = Blocks.OAK_SLAB;

    private EnhancedSlabLegacyTagMigration() {
    }

    public static boolean isEnhancedSlabBlockEntityTag(@Nullable CompoundTag tag) {
        return tag != null && isEnhancedSlabBlockEntityId(tag.getString(TAG_ID));
    }

    public static boolean isEnhancedSlabState(@Nullable BlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        return block instanceof MixedSlabBlock
                || block instanceof VerticalSlabBlock
                || block instanceof StepBlock
                || block instanceof VerticalStepBlock
                || block instanceof QuadStepBlock
                || block instanceof QuadVerticalStepBlock;
    }

    public static boolean remember(ServerLevel level, BlockPos pos, CompoundTag tag) {
        if (level == null || pos == null || tag == null) {
            return false;
        }

        BlockState[] slots = readSlots(tag);
        if (slots == null) {
            return false;
        }

        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), slots);
        return true;
    }

    public static @Nullable BlockState[] readSlots(CompoundTag tag) {
        if (tag == null) {
            return null;
        }

        String id = tag.getString(TAG_ID);
        if (ID_MIXED_SLAB.equals(id)) {
            return new BlockState[]{
                    readRequiredSlab(tag, TAG_BOTTOM_SLAB, SlabType.BOTTOM),
                    readRequiredSlab(tag, TAG_TOP_SLAB, SlabType.TOP)
            };
        }
        if (ID_VERTICAL_SLAB.equals(id)) {
            return new BlockState[]{
                    readRequiredSlab(tag, TAG_FACING_SLAB, SlabType.BOTTOM),
                    readRequiredSlab(tag, TAG_OPPOSITE_SLAB, SlabType.BOTTOM)
            };
        }
        if (ID_STEP.equals(id) || ID_VERTICAL_STEP.equals(id)) {
            return new BlockState[]{readRequiredSlab(tag, TAG_SLAB, SlabType.BOTTOM)};
        }
        if (ID_QUAD_STEP.equals(id) || ID_QUAD_VERTICAL_STEP.equals(id)) {
            BlockState[] slots = new BlockState[4];
            for (int i = 0; i < SLOT_TAGS.length; i++) {
                slots[i] = readOptionalSlab(tag, SLOT_TAGS[i], SlabType.BOTTOM);
            }
            return slots;
        }

        return null;
    }

    private static boolean isEnhancedSlabBlockEntityId(String id) {
        return ID_MIXED_SLAB.equals(id)
                || ID_VERTICAL_SLAB.equals(id)
                || ID_STEP.equals(id)
                || ID_QUAD_STEP.equals(id)
                || ID_VERTICAL_STEP.equals(id)
                || ID_QUAD_VERTICAL_STEP.equals(id);
    }

    private static BlockState readRequiredSlab(CompoundTag ownerTag, String key, SlabType type) {
        BlockState state = readSlab(ownerTag, key, type);
        return state != null ? state : defaultSlabState(type);
    }

    private static @Nullable BlockState readOptionalSlab(CompoundTag ownerTag, String key, SlabType type) {
        return readSlab(ownerTag, key, type);
    }

    private static @Nullable BlockState readSlab(CompoundTag ownerTag, String key, SlabType type) {
        if (ownerTag.contains(key, Tag.TAG_COMPOUND)) {
            CompoundTag slabTag = ownerTag.getCompound(key);
            return slabTag.isEmpty() ? null : loadSlabState(slabTag, type);
        }
        if (ownerTag.contains(key, Tag.TAG_STRING)) {
            String serialized = ownerTag.getString(key);
            return serialized == null || serialized.isEmpty() ? null : resolveLegacySlabState(serialized, type);
        }
        return null;
    }

    private static BlockState loadSlabState(CompoundTag slabTag, SlabType type) {
        Block block = resolveBlock(slabTag.getString(TAG_NAME));
        BlockState state = block != null ? block.defaultBlockState() : DEFAULT_SLAB.defaultBlockState();
        if (slabTag.contains(TAG_PROPERTIES, Tag.TAG_COMPOUND)) {
            CompoundTag propertiesTag = slabTag.getCompound(TAG_PROPERTIES);
            for (Property<?> property : state.getProperties()) {
                if (propertiesTag.contains(property.getName(), Tag.TAG_STRING)) {
                    state = applyPropertyValue(state, property, propertiesTag.getString(property.getName()));
                }
            }
        }
        return normalizeOrDefault(state, type);
    }

    private static @Nullable BlockState resolveLegacySlabState(String serialized, SlabType type) {
        Block block = resolveBlock(serialized);
        return block == null ? null : normalizeOptional(block.defaultBlockState(), type);
    }

    private static @Nullable Block resolveBlock(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return null;
        }
        try {
            String blockId = serialized;
            int bracketIdx = serialized.indexOf('[');
            if (bracketIdx >= 0) {
                blockId = serialized.substring(0, bracketIdx);
            }
            ResourceLocation id = ResourceLocation.tryParse(blockId);
            return id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BlockState normalizeOrDefault(@Nullable BlockState state, SlabType type) {
        BlockState normalized = normalizeOptional(state, type);
        return normalized != null ? normalized : defaultSlabState(type);
    }

    private static @Nullable BlockState normalizeOptional(@Nullable BlockState state, SlabType type) {
        Block slabBlock = sourceSlabBlock(state);
        if (slabBlock == null) {
            return null;
        }

        BlockState normalized = state != null && state.getBlock() == slabBlock && slabBlock instanceof SlabBlock
                ? state
                : slabBlock.defaultBlockState();
        if (normalized.hasProperty(SlabBlock.TYPE)) {
            normalized = normalized.setValue(SlabBlock.TYPE, type);
        }
        if (normalized.hasProperty(SlabBlock.WATERLOGGED)) {
            normalized = normalized.setValue(SlabBlock.WATERLOGGED, false);
        }
        return normalized;
    }

    private static BlockState defaultSlabState(SlabType type) {
        BlockState state = DEFAULT_SLAB.defaultBlockState();
        if (state.hasProperty(SlabBlock.TYPE)) {
            state = state.setValue(SlabBlock.TYPE, type);
        }
        if (state.hasProperty(SlabBlock.WATERLOGGED)) {
            state = state.setValue(SlabBlock.WATERLOGGED, false);
        }
        return state;
    }

    private static @Nullable Block sourceSlabBlock(@Nullable BlockState state) {
        if (state == null) {
            return null;
        }

        Block block = state.getBlock();
        if (block instanceof SlabBlock) {
            return block;
        }
        if (block instanceof FixedStepBlock fixedStep) {
            return fixedStep.getSourceSlab();
        }
        if (block instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            return fixedVerticalSlab.getSourceSlab();
        }
        if (block instanceof FixedVerticalStepBlock fixedVerticalStep) {
            return sourceSlabForVerticalSlab(fixedVerticalStep.getSourceVerticalSlab());
        }

        Block sourceSlab = StepDynamicRegistry.getSlabForStep(block);
        if (sourceSlab != null) {
            return sourceSlab;
        }

        Block sourceVertical = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(block);
        if (sourceVertical != null) {
            sourceSlab = sourceSlabForVerticalSlab(sourceVertical);
            if (sourceSlab != null) {
                return sourceSlab;
            }
        }

        return VerticalSlabDynamicRegistry.getSlabForVertical(block);
    }

    private static @Nullable Block sourceSlabForVerticalSlab(@Nullable Block verticalSlab) {
        if (verticalSlab == null) {
            return null;
        }
        Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(verticalSlab);
        if (sourceSlab != null) {
            return sourceSlab;
        }
        return verticalSlab instanceof SlabBlock ? verticalSlab : null;
    }

    private static <T extends Comparable<T>> BlockState applyPropertyValue(BlockState state,
                                                                           Property<T> property,
                                                                           String serializedValue) {
        return property.getValue(serializedValue)
                .map(value -> state.setValue(property, value))
                .orElse(state);
    }
}

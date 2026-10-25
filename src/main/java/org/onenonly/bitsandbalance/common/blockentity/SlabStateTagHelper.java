package org.onenonly.bitsandbalance.common.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
//? if mc == 1.21.1
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
//? endif
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;

public final class SlabStateTagHelper {

    private static final String TAG_BLOCK_NAME = "Name";
    private static final String TAG_BLOCK_PROPERTIES = "Properties";

    private SlabStateTagHelper() {
    }

    public static CompoundTag saveSlabState(@Nullable BlockState state, Block defaultSlab) {
        BlockState normalized = normalizeStoredSlabState(state, defaultSlab);
        CompoundTag slabTag = new CompoundTag();
        slabTag.putString(TAG_BLOCK_NAME, blockId(normalized.getBlock(), defaultSlab));

        CompoundTag propertiesTag = new CompoundTag();
        for (Property<?> property : normalized.getProperties()) {
            propertiesTag.putString(property.getName(), propertyValueName(normalized, property));
        }
        if (!propertiesTag.isEmpty()) {
            slabTag.put(TAG_BLOCK_PROPERTIES, propertiesTag);
        }
        return slabTag;
    }

    public static BlockState loadStoredSlabState(CompoundTag slabTag, Block defaultSlab) {
        if (slabTag == null || slabTag.isEmpty()) {
            return normalizeStoredSlabState(defaultSlab.defaultBlockState(), defaultSlab);
        }

        //? if mc == 1.21.1
        Block block = resolveSlabBlock(slabTag.getString(TAG_BLOCK_NAME), defaultSlab);
        BlockState state = block.defaultBlockState();
        if (slabTag.contains(TAG_BLOCK_PROPERTIES, Tag.TAG_COMPOUND)) {
            CompoundTag propertiesTag = slabTag.getCompound(TAG_BLOCK_PROPERTIES);
            for (Property<?> property : state.getProperties()) {
                if (propertiesTag.contains(property.getName(), Tag.TAG_STRING)) {
                    state = applyPropertyValue(state, property, propertiesTag.getString(property.getName()));
                }
            }
        }
        //? else
        Block block = resolveSlabBlock(slabTag.getStringOr(TAG_BLOCK_NAME, ""), defaultSlab);
        BlockState state = block.defaultBlockState();
        CompoundTag propertiesTag = slabTag.getCompoundOrEmpty(TAG_BLOCK_PROPERTIES);
        for (Property<?> property : state.getProperties()) {
            String serializedValue = propertiesTag.getStringOr(property.getName(), null);
            if (serializedValue != null) {
                state = applyPropertyValue(state, property, serializedValue);
            }
        }
        //? endif
        return normalizeStoredSlabState(state, defaultSlab);
    }

    public static @Nullable BlockState loadOptionalStoredSlabState(CompoundTag slabTag, Block defaultSlab) {
        if (slabTag == null || slabTag.isEmpty()) {
            return null;
        }

        //? if mc == 1.21.1
        String serialized = slabTag.contains(TAG_BLOCK_NAME, Tag.TAG_STRING) ? slabTag.getString(TAG_BLOCK_NAME) : "";
        Block block = resolveOptionalSlabBlock(serialized);
        if (block == null) {
            return null;
        }
        BlockState state = block.defaultBlockState();
        if (slabTag.contains(TAG_BLOCK_PROPERTIES, Tag.TAG_COMPOUND)) {
            CompoundTag propertiesTag = slabTag.getCompound(TAG_BLOCK_PROPERTIES);
            for (Property<?> property : state.getProperties()) {
                if (propertiesTag.contains(property.getName(), Tag.TAG_STRING)) {
                    state = applyPropertyValue(state, property, propertiesTag.getString(property.getName()));
                }
            }
        }
        //? else
        String serialized = slabTag.getStringOr(TAG_BLOCK_NAME, "");
        Block block = resolveOptionalSlabBlock(serialized);
        if (block == null) {
            return null;
        }
        BlockState state = block.defaultBlockState();
        CompoundTag propertiesTag = slabTag.getCompoundOrEmpty(TAG_BLOCK_PROPERTIES);
        for (Property<?> property : state.getProperties()) {
            String serializedValue = propertiesTag.getStringOr(property.getName(), null);
            if (serializedValue != null) {
                state = applyPropertyValue(state, property, serializedValue);
            }
        }
        //? endif
        return normalizeStoredSlabState(state, defaultSlab);
    }

    //? if mc != 1.21.1
    static BlockState readStoredSlabState(ValueInput input, String key, Block defaultSlab) {
        return input.child(key)
                .map(child -> loadStoredSlabState(child, defaultSlab))
                .orElseGet(() -> input.getString(key)
                        .map(serialized -> resolveLegacyStoredSlabState(serialized, defaultSlab))
                        .orElseGet(() -> normalizeStoredSlabState(defaultSlab.defaultBlockState(), defaultSlab)));
    }

    static @Nullable BlockState readOptionalStoredSlabState(ValueInput input, String key, Block defaultSlab) {
        return input.child(key)
                .map(child -> loadStoredSlabState(child, defaultSlab))
                .orElseGet(() -> input.getString(key)
                        .map(serialized -> resolveOptionalLegacyStoredSlabState(serialized, defaultSlab))
                        .orElse(null));
    }

    static boolean hasLegacyStoredSlabState(ValueInput input, String key) {
        return input.child(key).isEmpty() && input.getString(key).isPresent();
    }

    //? endif
    static BlockState resolveLegacyStoredSlabState(String serialized, Block defaultSlab) {
        return normalizeStoredSlabState(resolveSlabBlock(serialized, defaultSlab).defaultBlockState(), defaultSlab);
    }

    static @Nullable BlockState resolveOptionalLegacyStoredSlabState(String serialized, Block defaultSlab) {
        if (serialized == null || serialized.isEmpty()) {
            return null;
        }

        try {
            String blockIdStr = serialized;
            int bracketIdx = serialized.indexOf('[');
            if (bracketIdx >= 0) {
                blockIdStr = serialized.substring(0, bracketIdx);
            }
            //? if mc == 1.21.1
            ResourceLocation id = ResourceLocation.parse(blockIdStr);
            //? else
            Identifier id = Identifier.tryParse(blockIdStr);
            if (id == null) {
                return null;
            }
            //? endif
            Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            if (!(block instanceof SlabBlock)) {
                return null;
            }
            return normalizeStoredSlabState(block.defaultBlockState(), defaultSlab);
        } catch (Exception e) {
            return null;
        }
    }

    public static BlockState normalizeStoredSlabState(@Nullable BlockState state, Block defaultSlab) {
        BlockState normalized = (state != null && state.getBlock() instanceof SlabBlock)
                ? state
                : defaultSlab.defaultBlockState();
        if (normalized.hasProperty(SlabBlock.TYPE)) {
            normalized = normalized.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (normalized.hasProperty(SlabBlock.WATERLOGGED)) {
            normalized = normalized.setValue(SlabBlock.WATERLOGGED, false);
        }
        return normalized;
    }

    private static String blockId(@Nullable Block block, Block defaultSlab) {
        return BuiltInRegistries.BLOCK.getKey(block != null ? block : defaultSlab).toString();
    }

    private static Block resolveSlabBlock(String serialized, Block defaultSlab) {
        if (serialized == null || serialized.isEmpty()) {
            return defaultSlab;
        }

        try {
            String blockIdStr = serialized;
            int bracketIdx = serialized.indexOf('[');
            if (bracketIdx >= 0) {
                blockIdStr = serialized.substring(0, bracketIdx);
            }
            //? if mc == 1.21.1
            ResourceLocation identifier = ResourceLocation.parse(blockIdStr);
            //? else
            Identifier identifier = Identifier.tryParse(blockIdStr);
            if (identifier == null) {
                return defaultSlab;
            }
            //? endif
            Block block = BuiltInRegistries.BLOCK.getOptional(identifier).orElse(null);
            return block instanceof SlabBlock ? block : defaultSlab;
        } catch (Exception e) {
            return defaultSlab;
        }
    }

    private static @Nullable Block resolveOptionalSlabBlock(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return null;
        }

        try {
            String blockIdStr = serialized;
            int bracketIdx = serialized.indexOf('[');
            if (bracketIdx >= 0) {
                blockIdStr = serialized.substring(0, bracketIdx);
            }
            //? if mc == 1.21.1
            ResourceLocation identifier = ResourceLocation.parse(blockIdStr);
            //? else
            Identifier identifier = Identifier.tryParse(blockIdStr);
            if (identifier == null) {
                return null;
            }
            //? endif
            Block block = BuiltInRegistries.BLOCK.getOptional(identifier).orElse(null);
            return block instanceof SlabBlock ? block : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static <T extends Comparable<T>> String propertyValueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static <T extends Comparable<T>> BlockState applyPropertyValue(BlockState state,
                                                                           Property<T> property,
                                                                           String serializedValue) {
        return property.getValue(serializedValue)
                .map(value -> state.setValue(property, value))
                .orElse(state);
    }

    //? if mc != 1.21.1
    private static BlockState loadStoredSlabState(ValueInput slabInput, Block defaultSlab) {
        Block block = resolveSlabBlock(slabInput.getStringOr(TAG_BLOCK_NAME, ""), defaultSlab);
        BlockState state = block.defaultBlockState();
        ValueInput propertiesInput = slabInput.childOrEmpty(TAG_BLOCK_PROPERTIES);
        for (Property<?> property : state.getProperties()) {
            String serializedValue = propertiesInput.getStringOr(property.getName(), null);
            if (serializedValue != null) {
                state = applyPropertyValue(state, property, serializedValue);
            }
        }
        return normalizeStoredSlabState(state, defaultSlab);
    }
    //? endif
}

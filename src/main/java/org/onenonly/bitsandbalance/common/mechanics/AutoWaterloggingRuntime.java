package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AutoWaterloggingRuntime {
    public static final String MODE_WHITELIST = "WHITELIST";
    public static final String MODE_BLACKLIST = "BLACKLIST";
    public static final String MODE_ALLOWLIST = MODE_WHITELIST;
    public static final String MODE_DENYLIST = MODE_BLACKLIST;

    public static volatile boolean enabled = true;
    public static volatile boolean doorWaterlogging = true;
    public static volatile boolean fenceGateWaterlogging = true;
    public static volatile boolean pressurePlateWaterlogging = true;
    public static volatile boolean worldgenWaterlogging = false;
    public static volatile String mode = MODE_BLACKLIST;

    private static volatile Set<String> configuredBlockIds = Set.of();

    private AutoWaterloggingRuntime() {
    }

    public static void applyConfig(boolean enabledValue, @Nullable String modeValue, @Nullable List<String> blockIds,
                                   boolean doorWaterloggingValue, boolean fenceGateWaterloggingValue,
                                   boolean pressurePlateWaterloggingValue, boolean worldgenWaterloggingValue) {
        enabled = enabledValue;
        doorWaterlogging = doorWaterloggingValue;
        fenceGateWaterlogging = fenceGateWaterloggingValue;
        pressurePlateWaterlogging = pressurePlateWaterloggingValue;
        worldgenWaterlogging = worldgenWaterloggingValue;
        mode = normalizeMode(modeValue);
        configuredBlockIds = Set.copyOf(normalizeBlockIds(blockIds));
    }

    public static List<String> defaultBlockIds() {
        return List.of();
    }

    public static BlockState prepareAddedWaterloggingPlacementState(Level level, BlockPos pos, BlockState state) {
        if (!AutoWaterloggingBlockSupport.isAddedWaterloggingBlockFamily(state.getBlock())) {
            return state;
        }

        return preparePlacementState(level, pos, AutoWaterloggingBlockSupport.asDry(state));
    }

    public static BlockState prepareSetBlockState(Level level, BlockPos pos, BlockState state) {
        // Block-item placement is handled before Level#setBlock. The global Level hook exists for
        // structure/runtime placement and must obey the separately opt-in worldgen setting.
        if (!worldgenWaterlogging) {
            return state;
        }
        // Bucket pickup writes the same block back dry while its old water fluid is still visible.
        if (isExplicitWaterRemoval(level, pos, state)) {
            return state;
        }
        return prepareState(level, pos, state);
    }

    public static BlockState preparePlacementState(Level level, BlockPos pos, BlockState state) {
        return prepareState(level, pos, state);
    }

    public static BlockState prepareWorldgenSetBlockState(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!worldgenWaterlogging || isExplicitWaterRemoval(level, pos, state)) {
            return state;
        }
        return prepareState(level, pos, state);
    }

    private static boolean isExplicitWaterRemoval(BlockGetter level, BlockPos pos, BlockState state) {
        BlockState current = level.getBlockState(pos);
        return current.getBlock() == state.getBlock()
                && AutoWaterloggingBlockSupport.isWaterlogged(current)
                && state.hasProperty(BlockStateProperties.WATERLOGGED)
                && !state.getValue(BlockStateProperties.WATERLOGGED);
    }

    private static BlockState prepareState(BlockGetter level, BlockPos pos, BlockState state) {
        boolean addedWaterloggingFamily = AutoWaterloggingBlockSupport.isAddedWaterloggingBlockFamily(state.getBlock());
        if (AutoWaterloggingBlockSupport.isWaterlogged(state)) {
            return state;
        }
        if (addedWaterloggingFamily && !isAddedWaterloggingEnabled(state.getBlock())) {
            return AutoWaterloggingBlockSupport.asDry(state);
        }

        BlockState candidate = addedWaterloggingFamily
                ? AutoWaterloggingBlockSupport.asDry(state)
                : state;
        if (!enabled
                || !shouldAutoWaterlog(candidate)
                || !isWater(level.getFluidState(pos))) {
            return candidate;
        }

        return candidate.setValue(BlockStateProperties.WATERLOGGED, true);
    }

    private static boolean shouldAutoWaterlog(BlockState state) {
        if (!state.hasProperty(BlockStateProperties.WATERLOGGED) || state.getValue(BlockStateProperties.WATERLOGGED)) {
            return false;
        }
        if (!shouldAutoWaterlogBlockFamily(state.getBlock())) {
            return false;
        }

        Identifier key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key == null) {
            return false;
        }

        Set<String> blockIds = configuredBlockIds;
        if (blockIds.isEmpty()) {
            return !MODE_WHITELIST.equals(mode);
        }

        boolean listed = blockIds.contains(key.toString().toLowerCase(Locale.ROOT));
        return MODE_WHITELIST.equals(mode) ? listed : !listed;
    }

    private static boolean shouldAutoWaterlogBlockFamily(Block block) {
        if (AutoWaterloggingBlockSupport.isAddedWaterloggingBlockFamily(block)) {
            return isAddedWaterloggingEnabled(block);
        }
        return true;
    }

    public static boolean isAddedWaterloggingEnabled(Block block) {
        if (!enabled) {
            return false;
        }
        return (block instanceof DoorBlock && doorWaterlogging)
                || (block instanceof FenceGateBlock && fenceGateWaterlogging)
                || (block instanceof BasePressurePlateBlock && pressurePlateWaterlogging);
    }

    public static boolean isAddedWaterloggingActive(BlockState state) {
        return AutoWaterloggingBlockSupport.isAddedWaterloggingBlockFamily(state.getBlock())
                && AutoWaterloggingBlockSupport.isWaterlogged(state);
    }

    private static boolean isWater(FluidState fluid) {
        return fluid.getType() == Fluids.WATER;
    }

    private static List<String> normalizeBlockIds(@Nullable List<String> blockIds) {
        if (blockIds == null || blockIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String blockId : blockIds) {
            if (blockId == null) {
                continue;
            }
            String cleaned = blockId.trim().toLowerCase(Locale.ROOT);
            if (!cleaned.isEmpty()) {
                normalized.add(cleaned);
            }
        }
        return List.copyOf(normalized);
    }

    public static String normalizeMode(@Nullable String modeValue) {
        if (modeValue == null) {
            return MODE_BLACKLIST;
        }
        String normalized = modeValue.trim().toUpperCase(Locale.ROOT);
        if (MODE_WHITELIST.equals(normalized) || "ALLOWLIST".equals(normalized)) {
            return MODE_WHITELIST;
        }
        return MODE_BLACKLIST;
    }
}

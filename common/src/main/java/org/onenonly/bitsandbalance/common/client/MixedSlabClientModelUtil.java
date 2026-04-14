package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;

import java.util.List;

public final class MixedSlabClientModelUtil {
    private static final long TOP_SEED_SALT = 0x9E3779B97F4A7C15L;

    private MixedSlabClientModelUtil() {
    }

    public static @Nullable Halves halvesFromBlockEntity(BlockAndTintGetter level, BlockPos pos) {
        if (level == null || pos == null) return null;

        try {
            if (level.getBlockEntity(pos) instanceof MixedSlabBlockEntity mixedSlabBlockEntity) {
                return new Halves(mixedSlabBlockEntity.getBottomSlab(), mixedSlabBlockEntity.getTopSlab());
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public static void appendParts(List<BlockModelPart> parts, BlockState slabState, long seed) {
        BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(slabState);
        model.collectParts(RandomSource.create(seed), parts);
    }

    public static Object geometryKey(Halves halves) {
        return new GeometryKey(halves.bottom(), halves.top());
    }

    public static long topSeed(long baseSeed) {
        return baseSeed ^ TOP_SEED_SALT;
    }

    public record Halves(BlockState bottom, BlockState top) {
    }

    private record GeometryKey(BlockState bottom, BlockState top) {
    }
}
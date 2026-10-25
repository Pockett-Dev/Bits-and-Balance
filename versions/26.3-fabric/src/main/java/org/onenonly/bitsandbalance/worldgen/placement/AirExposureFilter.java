package org.onenonly.bitsandbalance.worldgen.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

/** 26.3 placement filters are codec-bearing interfaces rather than typed base classes. */
public final class AirExposureFilter implements PlacementFilter {
    public static final MapCodec<AirExposureFilter> CODEC = MapCodec.unit(AirExposureFilter::new);

    private AirExposureFilter() {
    }

    @Override
    public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        if (!BitsAndBalanceCommon.isDisableNetherLavaSprings()) {
            return true;
        }
        if (!BitsAndBalanceCommon.isKeepExposedNetherLavaSprings()) {
            return false;
        }

        for (Direction direction : Direction.values()) {
            if (context.getBlockState(pos.relative(direction)).isAir()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MapCodec<AirExposureFilter> codec() {
        return CODEC;
    }
}

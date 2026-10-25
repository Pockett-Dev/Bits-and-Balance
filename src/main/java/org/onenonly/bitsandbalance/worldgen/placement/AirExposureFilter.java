package org.onenonly.bitsandbalance.worldgen.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

public class AirExposureFilter extends PlacementFilter {
    public static final MapCodec<AirExposureFilter> CODEC = MapCodec.unit(AirExposureFilter::new);
    public static final PlacementModifierType<AirExposureFilter> TYPE = () -> CODEC;

    private AirExposureFilter() {
    }

    @Override
    protected boolean shouldPlace(PlacementContext ctx, RandomSource random, BlockPos pos) {
        // This modifier is only attached to Nether spring placed-features in our datapack override.
        // Behavior:
        // - If springs are NOT disabled: don't filter anything (vanilla behavior)
        // - If springs ARE disabled:
        //     - keepExposed=true: allow only springs that are adjacent to air
        //     - keepExposed=false: reject all springs

        if (!BitsAndBalanceCommon.isDisableNetherLavaSprings()) {
            return true;
        }

        if (!BitsAndBalanceCommon.isKeepExposedNetherLavaSprings()) {
            return false;
        }

        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.relative(dir);
            if (ctx.getBlockState(adjacent).isAir()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public PlacementModifierType<?> type() {
        return TYPE;
    }
}

package org.onenonly.bitsandbalance.worldgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

/** Datapack-friendly placement gate backed by the grouped worldgen configuration. */
public record ConfigEnabledFilter(Identifier key) implements PlacementFilter {
    public static final MapCodec<ConfigEnabledFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("key").forGetter(ConfigEnabledFilter::key)
    ).apply(instance, ConfigEnabledFilter::new));

    @Override
    public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return BitsAndBalanceCommon.isWorldgenEnabled(key);
    }

    @Override
    public MapCodec<ConfigEnabledFilter> codec() {
        return CODEC;
    }
}

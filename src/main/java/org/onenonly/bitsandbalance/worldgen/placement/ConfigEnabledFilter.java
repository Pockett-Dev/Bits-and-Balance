package org.onenonly.bitsandbalance.worldgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

/**
 * Datapack-friendly filter for placed features.
 *
 * JSON format:
 * { "type": "bitsandbalance:config_enabled", "key": "bitsandbalance:nether_gold_vein" }
 */
public final class ConfigEnabledFilter extends PlacementFilter {
    public static final MapCodec<ConfigEnabledFilter> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
            Identifier.CODEC.fieldOf("key").forGetter(ConfigEnabledFilter::key)
            ).apply(instance, ConfigEnabledFilter::new)
    );

    public static final PlacementModifierType<ConfigEnabledFilter> TYPE = () -> CODEC;

    private final Identifier key;

    public ConfigEnabledFilter(Identifier key) {
        this.key = key;
    }

    public Identifier key() {
        return key;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, net.minecraft.core.BlockPos pos) {
        return BitsAndBalanceCommon.isWorldgenEnabled(this.key);
    }

    @Override
    public PlacementModifierType<?> type() {
        return TYPE;
    }
}

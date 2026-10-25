package org.onenonly.bitsandbalance.fabric.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import org.onenonly.bitsandbalance.common.content.ModAzalea;
import org.onenonly.bitsandbalance.fabric.config.FabricWorldgenConfig;

/**
 * Selects the azalea trunk at placement time so config reloads remain effective in 26.3.
 */
public final class ConfigAwareAzaleaTrunkProvider implements BlockStateProvider {
    public static final MapCodec<ConfigAwareAzaleaTrunkProvider> CODEC =
            MapCodec.unit(ConfigAwareAzaleaTrunkProvider::new);

    private ConfigAwareAzaleaTrunkProvider() {
    }

    @Override
    public MapCodec<ConfigAwareAzaleaTrunkProvider> codec() {
        return CODEC;
    }

    @Override
    public BlockState getState(LevelAccessor level, RandomSource random, BlockPos pos) {
        if (FabricWorldgenConfig.isAzaleaWoodsetEnabled()
                && FabricWorldgenConfig.isAzaleaWoodGenerationEnabled()) {
            return ModAzalea.AZALEA_LOG.defaultBlockState();
        }
        return Blocks.OAK_LOG.defaultBlockState();
    }
}

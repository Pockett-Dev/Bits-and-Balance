package org.onenonly.bitsandbalance.recipe;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.onenonly.bitsandbalance.Config;

/**
 * Recipe condition that checks if the azalea woodset is enabled in the config.
 * This allows recipes to be conditionally loaded based on the enableAzaleaWoodset setting.
 */
public record AzaleaWoodsetEnabledCondition() implements ICondition {
    public static final AzaleaWoodsetEnabledCondition INSTANCE = new AzaleaWoodsetEnabledCondition();
    public static final MapCodec<AzaleaWoodsetEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(ICondition.IContext context) {
        return Config.enableAzaleaWoodset;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}

package org.onenonly.bitsandbalance.recipe;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.onenonly.bitsandbalance.Config;

/**
 * Recipe condition that checks if Glow Goo is enabled in the content config.
 */
public record GlowGooEnabledCondition() implements ICondition {
    public static final GlowGooEnabledCondition INSTANCE = new GlowGooEnabledCondition();
    public static final MapCodec<GlowGooEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(ICondition.IContext context) {
        return Config.enableGlowGoo;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
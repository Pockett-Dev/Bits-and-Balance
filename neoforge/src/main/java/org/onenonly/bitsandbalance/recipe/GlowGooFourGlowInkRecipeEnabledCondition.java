package org.onenonly.bitsandbalance.recipe;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.onenonly.bitsandbalance.Config;

/**
 * Recipe condition that checks if the 4-glow-ink Glow Goo recipe is enabled.
 */
public record GlowGooFourGlowInkRecipeEnabledCondition() implements ICondition {
    public static final GlowGooFourGlowInkRecipeEnabledCondition INSTANCE = new GlowGooFourGlowInkRecipeEnabledCondition();
    public static final MapCodec<GlowGooFourGlowInkRecipeEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(ICondition.IContext context) {
        return Config.glowGooFourGlowInkRecipeEnabled;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
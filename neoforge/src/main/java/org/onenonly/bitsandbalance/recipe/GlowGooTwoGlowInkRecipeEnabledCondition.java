package org.onenonly.bitsandbalance.recipe;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.onenonly.bitsandbalance.Config;

public record GlowGooTwoGlowInkRecipeEnabledCondition() implements ICondition {
    public static final GlowGooTwoGlowInkRecipeEnabledCondition INSTANCE = new GlowGooTwoGlowInkRecipeEnabledCondition();
    public static final MapCodec<GlowGooTwoGlowInkRecipeEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(ICondition.IContext context) {
        return Config.glowGooTwoGlowInkRecipeEnabled;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
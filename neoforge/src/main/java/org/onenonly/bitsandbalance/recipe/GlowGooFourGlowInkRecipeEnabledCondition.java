package org.onenonly.bitsandbalance.recipe;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.onenonly.bitsandbalance.Config;

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
package org.onenonly.bitsandbalance.recipe;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.onenonly.bitsandbalance.BitsAndBalance;

/**
 * Registers custom recipe conditions for Bits and Balance.
 */
public class ModConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = 
        DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, BitsAndBalance.MODID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<AzaleaWoodsetEnabledCondition>> AZALEA_WOODSET_ENABLED =
        CONDITION_CODECS.register("azalea_woodset_enabled", () -> AzaleaWoodsetEnabledCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<GlowGooEnabledCondition>> GLOW_GOO_ENABLED =
        CONDITION_CODECS.register("glow_goo_enabled", () -> GlowGooEnabledCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<GlowGooTwoGlowInkRecipeEnabledCondition>> GLOW_GOO_TWO_GLOW_INK_RECIPE_ENABLED =
        CONDITION_CODECS.register("glow_goo_two_glow_ink_recipe_enabled", () -> GlowGooTwoGlowInkRecipeEnabledCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<GlowGooFourGlowInkRecipeEnabledCondition>> GLOW_GOO_FOUR_GLOW_INK_RECIPE_ENABLED =
        CONDITION_CODECS.register("glow_goo_four_glow_ink_recipe_enabled", () -> GlowGooFourGlowInkRecipeEnabledCondition.CODEC);

    public static void register(IEventBus bus) {
        CONDITION_CODECS.register(bus);
    }
}

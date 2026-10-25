package org.onenonly.bitsandbalance.fabric.recipe;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.fabric.config.FabricContentConfig;

public final class FabricRecipeConditions {
    private static boolean initialized;

    private static final GlowGooEnabledCondition GLOW_GOO_ENABLED_CONDITION = new GlowGooEnabledCondition();
    private static final GlowGooTwoGlowInkRecipeEnabledCondition GLOW_GOO_TWO_GLOW_INK_RECIPE_ENABLED_CONDITION = new GlowGooTwoGlowInkRecipeEnabledCondition();
    private static final GlowGooFourGlowInkRecipeEnabledCondition GLOW_GOO_FOUR_GLOW_INK_RECIPE_ENABLED_CONDITION = new GlowGooFourGlowInkRecipeEnabledCondition();

    private static final ResourceConditionType<GlowGooEnabledCondition> GLOW_GOO_ENABLED =
            ResourceConditionType.create(
                    Identifier.parse(BitsAndBalanceCommon.MOD_ID + ":glow_goo_enabled"),
                    MapCodec.unit(GLOW_GOO_ENABLED_CONDITION)
            );
    private static final ResourceConditionType<GlowGooTwoGlowInkRecipeEnabledCondition> GLOW_GOO_TWO_GLOW_INK_RECIPE_ENABLED =
            ResourceConditionType.create(
                    Identifier.parse(BitsAndBalanceCommon.MOD_ID + ":glow_goo_two_glow_ink_recipe_enabled"),
                    MapCodec.unit(GLOW_GOO_TWO_GLOW_INK_RECIPE_ENABLED_CONDITION)
            );
    private static final ResourceConditionType<GlowGooFourGlowInkRecipeEnabledCondition> GLOW_GOO_FOUR_GLOW_INK_RECIPE_ENABLED =
            ResourceConditionType.create(
                    Identifier.parse(BitsAndBalanceCommon.MOD_ID + ":glow_goo_four_glow_ink_recipe_enabled"),
                    MapCodec.unit(GLOW_GOO_FOUR_GLOW_INK_RECIPE_ENABLED_CONDITION)
            );

    private FabricRecipeConditions() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        ResourceConditions.register(GLOW_GOO_ENABLED);
        ResourceConditions.register(GLOW_GOO_TWO_GLOW_INK_RECIPE_ENABLED);
        ResourceConditions.register(GLOW_GOO_FOUR_GLOW_INK_RECIPE_ENABLED);
    }

    private record GlowGooEnabledCondition() implements ResourceCondition {
        @Override
        public ResourceConditionType<?> getType() {
            return GLOW_GOO_ENABLED;
        }

        @Override
        public boolean test(RegistryOps.RegistryInfoLookup registryInfo) {
            return FabricContentConfig.enableGlowGoo;
        }
    }

    private record GlowGooTwoGlowInkRecipeEnabledCondition() implements ResourceCondition {
        @Override
        public ResourceConditionType<?> getType() {
            return GLOW_GOO_TWO_GLOW_INK_RECIPE_ENABLED;
        }

        @Override
        public boolean test(RegistryOps.RegistryInfoLookup registryInfo) {
            return FabricContentConfig.glowGooTwoGlowInkRecipeEnabled;
        }
    }

    private record GlowGooFourGlowInkRecipeEnabledCondition() implements ResourceCondition {
        @Override
        public ResourceConditionType<?> getType() {
            return GLOW_GOO_FOUR_GLOW_INK_RECIPE_ENABLED;
        }

        @Override
        public boolean test(RegistryOps.RegistryInfoLookup registryInfo) {
            return FabricContentConfig.glowGooFourGlowInkRecipeEnabled;
        }
    }
}

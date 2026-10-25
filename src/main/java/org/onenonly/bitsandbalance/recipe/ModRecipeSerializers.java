package org.onenonly.bitsandbalance.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.recipe.BaseBlockRecoveryRecipe;
import org.onenonly.bitsandbalance.common.recipe.VerticalSlabConvertRecipe;
import org.onenonly.bitsandbalance.common.recipe.VerticalSlabFromPlanksRecipe;
import org.onenonly.bitsandbalance.common.recipe.StepConvertRecipe;
import org.onenonly.bitsandbalance.common.recipe.StepFromSlabRecipe;
import org.onenonly.bitsandbalance.common.recipe.VerticalStepFromVerticalSlabRecipe;

public final class ModRecipeSerializers {
    private ModRecipeSerializers() {
    }

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BitsAndBalance.MODID);

            public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> VERTICAL_SLAB_FROM_PLANKS =
                RECIPE_SERIALIZERS.register("vertical_slab_from_planks", () -> (RecipeSerializer<?>) VerticalSlabFromPlanksRecipe.Serializer.INSTANCE);

            public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> VERTICAL_SLAB_CONVERT =
                RECIPE_SERIALIZERS.register("vertical_slab_convert", () -> (RecipeSerializer<?>) VerticalSlabConvertRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> STEP_FROM_SLAB =
        RECIPE_SERIALIZERS.register("step_from_slab", () -> (RecipeSerializer<?>) StepFromSlabRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> VERTICAL_STEP_FROM_VERTICAL_SLAB =
        RECIPE_SERIALIZERS.register("vertical_step_from_vertical_slab", () -> (RecipeSerializer<?>) VerticalStepFromVerticalSlabRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> STEP_CONVERT =
        RECIPE_SERIALIZERS.register("step_convert", () -> (RecipeSerializer<?>) StepConvertRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> BASE_BLOCK_RECOVERY =
        RECIPE_SERIALIZERS.register("base_block_recovery", () -> (RecipeSerializer<?>) BaseBlockRecoveryRecipe.Serializer.INSTANCE);

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }
}

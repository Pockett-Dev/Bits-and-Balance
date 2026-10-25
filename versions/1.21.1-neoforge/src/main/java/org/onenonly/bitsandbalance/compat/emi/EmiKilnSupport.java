package org.onenonly.bitsandbalance.compat.emi;

import com.mojang.logging.LogUtils;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.onenonly.bitsandbalance.common.content.ModKiln;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

/** Uses the live smelting recipes and Kiln input tag to show exactly what the Kiln accepts. */
final class EmiKilnSupport {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("bitsandbalance", "kiln"),
            EmiStack.of(ModKiln.kiln())
    );
    private static final EmiIngredient DISPLAY_FUELS = EmiIngredient.of(List.of(
            EmiStack.of(Items.COAL), EmiStack.of(Items.CHARCOAL),
            EmiStack.of(Items.COAL_BLOCK), EmiStack.of(Items.BLAZE_ROD),
            EmiStack.of(Items.DRIED_KELP_BLOCK)
    ));

    private EmiKilnSupport() {
    }

    static void register(EmiRegistry registry) {
        if (Minecraft.getInstance().level == null) {
            return;
        }

        registry.addCategory(CATEGORY);
        registry.addWorkstation(CATEGORY, EmiStack.of(ModKiln.kiln()));

        int registered = 0;
        for (RecipeHolder<SmeltingRecipe> holder : registry.getRecipeManager()
                .getAllRecipesFor(RecipeType.SMELTING)) {
            SmeltingRecipe recipe = holder.value();
            List<EmiStack> kilnInputs = Arrays.stream(recipe.getIngredients().getFirst().getItems())
                    .filter(stack -> stack.is(ModKiln.KILN_SMELTABLES))
                    .map(EmiStack::of)
                    .toList();
            if (kilnInputs.isEmpty()) {
                continue;
            }

            ItemStack output = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
            if (output.isEmpty()) {
                continue;
            }

            ResourceLocation sourceId = holder.id();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("bitsandbalance",
                    "/emi_kiln/" + sourceId.getNamespace() + "/" + sourceId.getPath());
            registry.addRecipe(new KilnDisplayRecipe(
                    EmiIngredient.of(kilnInputs), EmiStack.of(output.copy()), id,
                    Math.max(1, recipe.getCookingTime() / 2)
            ));
            registered++;
        }
        LOGGER.info("[bitsandbalance] Registered {} EMI Kiln recipes", registered);
    }

    private record KilnDisplayRecipe(EmiIngredient input, EmiStack output,
                                     ResourceLocation id, int cookTime) implements EmiRecipe {
        @Override
        public EmiRecipeCategory getCategory() {
            return CATEGORY;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public List<EmiIngredient> getInputs() {
            return List.of(input);
        }

        @Override
        public List<EmiStack> getOutputs() {
            return List.of(output);
        }

        @Override
        public int getDisplayWidth() {
            return 82;
        }

        @Override
        public int getDisplayHeight() {
            return 54;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addSlot(input, 0, 0);
            widgets.addSlot(DISPLAY_FUELS, 0, 36);
            widgets.addTexture(EmiTexture.EMPTY_FLAME, 2, 18);
            widgets.addAnimatedTexture(EmiTexture.FULL_FLAME, 2, 18, 300, false, true, false);
            widgets.addFillingArrow(25, 18, cookTime * 50);
            widgets.addSlot(output, 60, 18).recipeContext(this);
        }
    }
}

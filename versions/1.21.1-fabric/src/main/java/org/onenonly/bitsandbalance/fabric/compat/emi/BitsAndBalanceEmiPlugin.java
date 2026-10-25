package org.onenonly.bitsandbalance.fabric.compat.emi;

import com.mojang.logging.LogUtils;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.fabric.potions.FabricCustomBrewing;
import org.slf4j.Logger;

import java.util.List;

/** Makes Bits and Balance's data-driven Fabric brewing rules visible to EMI. */
public final class BitsAndBalanceEmiPlugin implements EmiPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void register(EmiRegistry registry) {
        EmiSyntheticCraftingSupport.register(registry);
        EmiKilnSupport.register(registry);

        int registered = 0;
        for (FabricCustomBrewing.Rule rule : FabricCustomBrewing.rules()) {
            Potion inputPotion = BuiltInRegistries.POTION.getOptional(rule.inputPotion()).orElse(null);
            Item reagentItem = BuiltInRegistries.ITEM.getOptional(rule.reagentItem()).orElse(null);
            Potion resultPotion = BuiltInRegistries.POTION.getOptional(rule.resultPotion()).orElse(null);
            if (inputPotion == null || reagentItem == null || reagentItem == Items.AIR || resultPotion == null) {
                continue;
            }

            Holder<Potion> inputPotionHolder = BuiltInRegistries.POTION.wrapAsHolder(inputPotion);
            Holder<Potion> resultPotionHolder = BuiltInRegistries.POTION.wrapAsHolder(resultPotion);
            EmiStack ingredient = EmiStack.of(reagentItem);

            register(registry, rule, ingredient, Items.POTION, inputPotionHolder, resultPotionHolder, "potion");
            register(registry, rule, ingredient, Items.SPLASH_POTION, inputPotionHolder, resultPotionHolder, "splash");
            register(registry, rule, ingredient, Items.LINGERING_POTION, inputPotionHolder, resultPotionHolder, "lingering");
            registered += 3;
        }

        LOGGER.info("[{}] Registered {} custom EMI brewing recipes", BitsAndBalanceCommon.MOD_ID, registered);
    }

    private static void register(
            EmiRegistry registry,
            FabricCustomBrewing.Rule rule,
            EmiStack ingredient,
            Item potionContainer,
            Holder<Potion> inputPotion,
            Holder<Potion> resultPotion,
            String bottle
    ) {
        ItemStack inputStack = PotionContents.createItemStack(potionContainer, inputPotion);
        ItemStack outputStack = PotionContents.createItemStack(potionContainer, resultPotion);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                BitsAndBalanceCommon.MOD_ID,
                "/emi_brewing/" + idPart(rule.inputPotion())
                        + "_with_" + idPart(rule.reagentItem())
                        + "_to_" + idPart(rule.resultPotion())
                        + "/" + bottle
        );
        registry.addRecipe(new BrewingDisplayRecipe(
                EmiStack.of(inputStack), ingredient, EmiStack.of(outputStack), id
        ));
    }

    private static String idPart(ResourceLocation id) {
        return id.getNamespace() + "_" + id.getPath().replace('/', '_');
    }

    private static final class BrewingDisplayRecipe implements EmiRecipe {
        private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
                "minecraft", "textures/gui/container/brewing_stand.png"
        );
        private static final EmiStack BLAZE_POWDER = EmiStack.of(Items.BLAZE_POWDER);

        private final EmiStack input;
        private final EmiStack ingredient;
        private final EmiStack output;
        private final EmiStack inputBatch;
        private final EmiStack outputBatch;
        private final ResourceLocation id;

        private BrewingDisplayRecipe(EmiStack input, EmiStack ingredient, EmiStack output, ResourceLocation id) {
            this.input = input;
            this.ingredient = ingredient;
            this.output = output;
            this.inputBatch = input.copy().setAmount(3);
            this.outputBatch = output.copy().setAmount(3);
            this.id = id;
        }

        @Override
        public dev.emi.emi.api.recipe.EmiRecipeCategory getCategory() {
            return VanillaEmiRecipeCategories.BREWING;
        }

        @Override
        public @Nullable ResourceLocation getId() {
            return id;
        }

        @Override
        public List<dev.emi.emi.api.stack.EmiIngredient> getInputs() {
            return List.of(inputBatch, ingredient, BLAZE_POWDER);
        }

        @Override
        public List<EmiStack> getOutputs() {
            return List.of(outputBatch);
        }

        @Override
        public int getDisplayWidth() {
            return 103;
        }

        @Override
        public int getDisplayHeight() {
            return 61;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(BACKGROUND, 0, 0, 103, 61, 56, 15);
            widgets.addSlot(ingredient, 43, 0).drawBack(false);
            widgets.addSlot(BLAZE_POWDER, 4, 0).drawBack(false);
            widgets.addSlot(input, 4, 42).drawBack(false);
            widgets.addSlot(input, 26, 42).drawBack(false);
            widgets.addSlot(input, 48, 42).drawBack(false);
            widgets.addAnimatedTexture(EmiTexture.FULL_FLAME, 6, 19, 4000, false, true, false);
            widgets.addSlot(output, 81, 42).recipeContext(this);
        }
    }
}

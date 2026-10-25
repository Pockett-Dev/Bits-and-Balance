package org.onenonly.bitsandbalance.common.compat.jei;

import com.mojang.logging.LogUtils;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.placement.VerticalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.onenonly.bitsandbalance.common.content.ModKiln;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/** Registers an accurate, Kiln-specific JEI category backed by the Kiln's accepted recipes. */
public final class JeiKilnSupport {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int KILN_COOK_TIME = 100;
    private static final Ingredient DISPLAY_FUELS = Ingredient.of(
            Items.COAL,
            Items.CHARCOAL,
            Items.COAL_BLOCK,
            Items.BLAZE_ROD,
            Items.DRIED_KELP_BLOCK
    );
    public static final RecipeType<JeiKilnRecipe> RECIPE_TYPE =
            RecipeType.create("bitsandbalance", "kiln", JeiKilnRecipe.class);

    private JeiKilnSupport() {
    }

    public static void registerCategory(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new KilnCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RECIPE_TYPE, buildFallbackRecipes());
    }

    public static void registerRuntimeRecipes(IJeiRuntime runtime) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        List<RecipeHolder<SmeltingRecipe>> smeltingRecipes = runtime.getRecipeManager()
                .createRecipeLookup(RecipeTypes.SMELTING)
                .get()
                .toList();
        List<JeiKilnRecipe> moddedRecipes = discoverModdedSmeltingRecipes(minecraft, smeltingRecipes);
        if (!moddedRecipes.isEmpty()) {
            runtime.getRecipeManager().addRecipes(RECIPE_TYPE, moddedRecipes);
        }
        LOGGER.info("[bitsandbalance] Added {} modded Kiln recipes to JEI", moddedRecipes.size());
    }

    public static void registerCatalyst(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModKiln.kiln(), RECIPE_TYPE);
    }

    public static void registerTransferHandler(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                FurnaceMenu.class,
                MenuType.FURNACE,
                RECIPE_TYPE,
                0,
                1,
                3,
                36
        );
    }

    /**
     * Converts modded entries from JEI's live smelting registry after all recipes and
     * item tags have loaded. Vanilla recipes are already supplied by the fallback list.
     */
    private static List<JeiKilnRecipe> discoverModdedSmeltingRecipes(
            Minecraft minecraft,
            List<RecipeHolder<SmeltingRecipe>> smeltingRecipes) {
        List<JeiKilnRecipe> recipes = new ArrayList<>();

        for (RecipeHolder<SmeltingRecipe> holder : smeltingRecipes) {
            SmeltingRecipe recipe = holder.value();
            Ingredient smeltingInput = recipeInput(recipe);

            Item[] kilnInputs = BuiltInRegistries.ITEM.stream()
                    .filter(item -> {
                        ItemStack stack = new ItemStack(item);
                        return stack.is(ModKiln.KILN_SMELTABLES) && smeltingInput.test(stack);
                    })
                    .toArray(Item[]::new);
            if (kilnInputs.length == 0) {
                continue;
            }
            boolean hasModdedInput = false;
            for (Item item : kilnInputs) {
                String id = String.valueOf(BuiltInRegistries.ITEM.getKey(item));
                if (!id.startsWith("minecraft:")) {
                    hasModdedInput = true;
                    break;
                }
            }
            if (!hasModdedInput) {
                continue;
            }

            ItemStack representativeInput = new ItemStack(kilnInputs[0]);
            ItemStack output = recipeOutput(minecraft, recipe, representativeInput);
            float experience = recipeExperience(recipe);
            int cookingTime = recipeCookingTime(recipe);

            if (!output.isEmpty()) {
                recipes.add(new JeiKilnRecipe(
                        Ingredient.of(kilnInputs),
                        output.copy(),
                        experience,
                        Math.max(1, cookingTime / 2)
                ));
            }
        }

        return List.copyOf(recipes);
    }

    private static Ingredient recipeInput(SmeltingRecipe recipe) {
        //? if mc == 1.21.1
        return recipe.getIngredients().getFirst();
        //? else
        return recipe.input();
        //? endif
    }

    private static ItemStack recipeOutput(
            Minecraft minecraft,
            SmeltingRecipe recipe,
            ItemStack representativeInput) {
        //? if mc == 1.21.1
        return recipe.getResultItem(minecraft.level.registryAccess()).copy();
        //? endif
        //? if mc == 1.21.11
        return recipe.assemble(new SingleRecipeInput(representativeInput), minecraft.level.registryAccess());
        //? endif
        //? if mc != 1.21.1 && mc != 1.21.11
        return recipe.assemble(new SingleRecipeInput(representativeInput));
        //? endif
    }

    private static float recipeExperience(SmeltingRecipe recipe) {
        //? if mc == 1.21.1
        return recipe.getExperience();
        //? else
        return recipe.experience();
        //? endif
    }

    private static int recipeCookingTime(SmeltingRecipe recipe) {
        //? if mc == 1.21.1
        return recipe.getCookingTime();
        //? else
        return recipe.cookingTime();
        //? endif
    }

    private static List<JeiKilnRecipe> buildFallbackRecipes() {
        List<JeiKilnRecipe> recipes = new ArrayList<>();

        add(recipes, "clay_ball", "brick", 0.3F);
        add(recipes, "clay", "terracotta", 0.35F);
        for (String color : List.of(
                "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            add(recipes, color + "_terracotta", color + "_glazed_terracotta");
        }

        add(recipes, ingredientOfTag(ItemTags.SMELTS_TO_GLASS), "glass");
        add(recipes, "cactus", "green_dye", 1.0F);
        add(recipes, "sea_pickle", "lime_dye", 0.1F);
        add(recipes, "cobblestone", "stone");
        add(recipes, "stone", "smooth_stone");
        add(recipes, "stone_bricks", "cracked_stone_bricks");
        add(recipes, "cobbled_deepslate", "deepslate");
        add(recipes, "deepslate_bricks", "cracked_deepslate_bricks");
        add(recipes, "deepslate_tiles", "cracked_deepslate_tiles");
        add(recipes, "sandstone", "smooth_sandstone");
        add(recipes, "red_sandstone", "smooth_red_sandstone");
        add(recipes, "quartz_block", "smooth_quartz");
        add(recipes, "basalt", "smooth_basalt");
        add(recipes, "netherrack", "nether_brick");
        add(recipes, "nether_bricks", "cracked_nether_bricks");
        add(recipes, "polished_blackstone_bricks", "cracked_polished_blackstone_bricks");
        add(recipes, "chorus_fruit", "popped_chorus_fruit");
        add(recipes, "wet_sponge", "sponge", 0.15F);
        add(recipes, ingredientOfTag(ItemTags.LOGS_THAT_BURN), "charcoal", 0.15F);

        Item leafLitter = findMinecraftItem("leaf_litter");
        if (leafLitter != Items.AIR) {
            add(recipes, ingredientOfTag(ItemTags.LEAVES), leafLitter, 0.1F);
        }

        add(recipes, "resin_clump", "resin_brick");

        return List.copyOf(recipes);
    }

    private static void add(List<JeiKilnRecipe> recipes, String inputPath, String outputPath) {
        add(recipes, inputPath, outputPath, 0.1F);
    }

    private static void add(
            List<JeiKilnRecipe> recipes,
            String inputPath,
            String outputPath,
            float experience) {
        Item input = findMinecraftItem(inputPath);
        if (input != Items.AIR) {
            add(recipes, Ingredient.of(input), outputPath, experience);
        }
    }

    private static void add(List<JeiKilnRecipe> recipes, Ingredient input, String outputPath) {
        add(recipes, input, outputPath, 0.1F);
    }

    private static void add(
            List<JeiKilnRecipe> recipes,
            Ingredient input,
            String outputPath,
            float experience) {
        add(recipes, input, findMinecraftItem(outputPath), experience);
    }

    private static void add(
            List<JeiKilnRecipe> recipes,
            Ingredient input,
            Item output,
            float experience) {
        if (output != Items.AIR) {
            recipes.add(new JeiKilnRecipe(input, new ItemStack(output), experience, KILN_COOK_TIME));
        }
    }

    private static Ingredient ingredientOfTag(TagKey<Item> tag) {
        Item[] taggedItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> new ItemStack(item).is(tag))
                .toArray(Item[]::new);
        return Ingredient.of(taggedItems);
    }

    private static Item findMinecraftItem(String path) {
        String expected = "minecraft:" + path;
        for (Item item : BuiltInRegistries.ITEM) {
            if (expected.equals(String.valueOf(BuiltInRegistries.ITEM.getKey(item)))) {
                return item;
            }
        }
        return Items.AIR;
    }

    private static final class KilnCategory implements IRecipeCategory<JeiKilnRecipe> {
        private static final Component TITLE = Component.translatable("jei.bitsandbalance.kiln");
        private final IDrawable icon;

        private KilnCategory(IGuiHelper guiHelper) {
            this.icon = guiHelper.createDrawableItemLike(ModKiln.kiln());
        }

        @Override
        public RecipeType<JeiKilnRecipe> getRecipeType() {
            return RECIPE_TYPE;
        }

        @Override
        public Component getTitle() {
            return TITLE;
        }

        @Override
        public int getWidth() {
            return 82;
        }

        @Override
        public int getHeight() {
            return 54;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, JeiKilnRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(1, 1)
                    .setStandardSlotBackground()
                    .addIngredients(recipe.input());

            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 1, 37)
                    .setStandardSlotBackground()
                    .addIngredients(DISPLAY_FUELS);

            builder.addOutputSlot(61, 19)
                    .setOutputSlotBackground()
                    .addItemStack(recipe.output());
        }

        @Override
        public void createRecipeExtras(
                IRecipeExtrasBuilder builder,
                JeiKilnRecipe recipe,
                IFocusGroup focuses) {
            builder.addAnimatedRecipeArrow(recipe.cookTime()).setPosition(26, 17);
            builder.addAnimatedRecipeFlame(300).setPosition(1, 20);

            if (recipe.experience() > 0) {
                Component experience = Component.translatable(
                        "gui.jei.category.smelting.experience",
                        recipe.experience()
                );
                builder.addText(experience, getWidth() - 20, 10)
                        .setPosition(0, 0, getWidth(), getHeight(), HorizontalAlignment.RIGHT, VerticalAlignment.TOP)
                        .setTextAlignment(HorizontalAlignment.RIGHT)
                        .setColor(0xFF808080);
            }

            Component cookTime = Component.translatable(
                    "gui.jei.category.smelting.time.seconds",
                    recipe.cookTime() / 20
            );
            builder.addText(cookTime, getWidth() - 20, 10)
                    .setPosition(0, 0, getWidth(), getHeight(), HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM)
                    .setTextAlignment(HorizontalAlignment.RIGHT)
                    .setTextAlignment(VerticalAlignment.BOTTOM)
                    .setColor(0xFF808080);
        }
    }
}

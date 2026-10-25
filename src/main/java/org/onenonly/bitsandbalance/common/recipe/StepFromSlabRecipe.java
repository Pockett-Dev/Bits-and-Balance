package org.onenonly.bitsandbalance.common.recipe;

import com.mojang.serialization.MapCodec;
//? if mc != 26.1 && mc != 26.2 && mc != 26.3
import net.minecraft.core.HolderLookup;
//? endif
//? if mc != 1.21.1
import net.minecraft.core.registries.BuiltInRegistries;
//? endif
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
//? if mc != 1.21.1
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
//? endif
import net.minecraft.world.item.crafting.RecipeSerializer;
//? if mc != 1.21.1
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
//? endif
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

//? if mc != 1.21.1
import java.util.LinkedHashSet;
import java.util.List;
//? endif

/**
 * 2 slabs in a horizontal row -> 4 matching steps,
 * and 2 matching steps in a horizontal row -> 1 slab.
 */
public final class StepFromSlabRecipe extends CustomRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "step_from_slab");
    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private final CraftingBookCategory category;
    //? endif

    public StepFromSlabRecipe(CraftingBookCategory category) {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        this.category = category;
        //? else
        super(category);
        //? endif
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !resultFor(input).isEmpty();
    }

    @Override
    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    public ItemStack assemble(CraftingInput input) {
        return resultFor(input);
    }
    //? else
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return resultFor(input);
    }
    //? endif

    @Override
    //? if mc == 1.21.1
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 1;
    }
    //? else
    public PlacementInfo placementInfo() {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        Ingredient ingredient = placementIngredient();
        return ingredient == null ? PlacementInfo.create(List.of()) : RecipeBookDisplayHelper.repeatedPlacement(ingredient, 2);
        //? else
        return PlacementInfo.create(List.of());
        //? endif
    }

    @Override
    public List<RecipeDisplay> display() {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        java.util.ArrayList<RecipeDisplay> displays = new java.util.ArrayList<>();

        Ingredient slabIngredient = slabPlacementIngredient();
        ItemStack stepResult = representativeStepResult();
        if (slabIngredient != null && !stepResult.isEmpty()) {
            displays.add(RecipeBookDisplayHelper.shapedFilledDisplay(2, 1, slabIngredient, stepResult));
        }

        Ingredient stepIngredient = stepPlacementIngredient();
        ItemStack slabResult = representativeSlabResult();
        if (stepIngredient != null && !slabResult.isEmpty()) {
            displays.add(RecipeBookDisplayHelper.shapedFilledDisplay(2, 1, stepIngredient, slabResult));
        }

        return displays;
        //? else
        return List.of();
        //? endif
    }
    //? endif

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    @Override
    public CraftingBookCategory category() {
        return this.category;
    }
    //? endif

    @Override
    public boolean isSpecial() {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        return false;
        //? else
        return true;
        //? endif
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    private static ItemStack resultFor(CraftingInput input) {
        if (input == null || input.isEmpty()) return ItemStack.EMPTY;
        if (input.height() != 1 || input.width() != 2) return ItemStack.EMPTY;

        ItemStack first = input.getItem(0);
        ItemStack second = input.getItem(1);
        if (first.isEmpty() || second.isEmpty()) return ItemStack.EMPTY;
        if (!ItemStack.isSameItemSameComponents(first, second)) return ItemStack.EMPTY;

        Item item = first.getItem();
        if (!(item instanceof BlockItem blockItem)) return ItemStack.EMPTY;

        Block block = blockItem.getBlock();
        if (block == null) return ItemStack.EMPTY;

        Block step = EnhancedSlabRecipeLookup.stepForSlab(block);
        if (step != null) {
            Item out = step.asItem();
            return isRealItem(out) ? new ItemStack(out, 4) : ItemStack.EMPTY;
        }

        Block slab = EnhancedSlabRecipeLookup.slabForStep(block);
        if (slab == null) return ItemStack.EMPTY;
        Item out = slab.asItem();
        return isRealItem(out) ? new ItemStack(out, 1) : ItemStack.EMPTY;
    }

    private static ItemStack pairedResult(Block block) {
        if (block == null) return ItemStack.EMPTY;

        Block step = EnhancedSlabRecipeLookup.stepForSlab(block);
        if (step != null) {
            Item out = step.asItem();
            return isRealItem(out) ? new ItemStack(out, 4) : ItemStack.EMPTY;
        }

        Block slab = EnhancedSlabRecipeLookup.slabForStep(block);
        if (slab == null) return ItemStack.EMPTY;
        Item out = slab.asItem();
        return isRealItem(out) ? new ItemStack(out, 1) : ItemStack.EMPTY;
    }

    private static boolean isRealItem(Item item) {
        return item != null && item != Items.AIR;
    }

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private static ItemStack stack(String id) {
        return stack(id, 1);
    }

    private static ItemStack stack(String id, int count) {
        Identifier itemId = Identifier.parse(id);
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    private static LinkedHashSet<Item> slabPlacementItems() {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Block step = EnhancedSlabRecipeLookup.stepForSlab(block);
            if (step == null) continue;
            Item item = block.asItem();
            if (isRealItem(item)) {
                items.add(item);
            }
        }
        return items;
    }

    private static LinkedHashSet<Item> stepPlacementItems() {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Block slab = EnhancedSlabRecipeLookup.slabForStep(block);
            if (slab == null) continue;
            Item item = block.asItem();
            if (isRealItem(item)) {
                items.add(item);
            }
        }
        return items;
    }

    private static Ingredient slabPlacementIngredient() {
        return ingredientOfItems(slabPlacementItems());
    }

    private static Ingredient stepPlacementIngredient() {
        return ingredientOfItems(stepPlacementItems());
    }
    //? endif

    //? if mc != 1.21.1
    private static Ingredient placementIngredient() {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        items.addAll(slabPlacementItems());
        items.addAll(stepPlacementItems());
        //? else
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!pairedResult(block).isEmpty()) {
                Item item = block.asItem();
                if (isRealItem(item)) {
                    items.add(item);
                }
            }
        }
        //? endif
        return ingredientOfItems(items);
    }

    private static Ingredient ingredientOfItems(LinkedHashSet<Item> items) {
        if (items.isEmpty()) return null;
        return Ingredient.of(items.toArray(Item[]::new));
    }

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private static ItemStack representativeStepResult() {
        ItemStack result = stack("bitsandbalance:oak_step", 4);
        if (!result.isEmpty()) {
            return result;
        }

        for (Block block : BuiltInRegistries.BLOCK) {
            Block step = EnhancedSlabRecipeLookup.stepForSlab(block);
            if (step == null) continue;
            Item item = step.asItem();
            if (isRealItem(item)) {
                return new ItemStack(item, 4);
            }
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack representativeSlabResult() {
        for (Block block : BuiltInRegistries.BLOCK) {
            Block slab = EnhancedSlabRecipeLookup.slabForStep(block);
            if (slab == null) continue;
            Item item = slab.asItem();
            if (isRealItem(item)) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }
    //? endif

    private static RecipeDisplay shapedDisplay(int width, int height, List<SlotDisplay> grid, ItemStack result) {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        SlotDisplay resultSlot = new SlotDisplay.ItemStackSlotDisplay(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(result));
        //? else
        SlotDisplay resultSlot = new SlotDisplay.ItemStackSlotDisplay(result);
        //? endif
        SlotDisplay station = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);
        return new ShapedCraftingRecipeDisplay(width, height, grid, resultSlot, station);
    }
    //? endif

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    public static final class Serializer {
    //? else
    public static final class Serializer implements RecipeSerializer<StepFromSlabRecipe> {
    //? endif
        //? if mc != 26.1 && mc != 26.2 && mc != 26.3
        public static final Serializer INSTANCE = new Serializer();
        //? endif

        private static final StepFromSlabRecipe TEMPLATE = new StepFromSlabRecipe(CraftingBookCategory.BUILDING);
        private static final MapCodec<StepFromSlabRecipe> CODEC = MapCodec.unit(TEMPLATE);
        private static final StreamCodec<RegistryFriendlyByteBuf, StepFromSlabRecipe> STREAM_CODEC = StreamCodec.unit(TEMPLATE);

        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        public static final RecipeSerializer<StepFromSlabRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);
        //? endif

        private Serializer() {
        }

        //? if mc != 26.1 && mc != 26.2 && mc != 26.3
        @Override
        public MapCodec<StepFromSlabRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StepFromSlabRecipe> streamCodec() {
            return STREAM_CODEC;
        }
        //? endif
    }
}

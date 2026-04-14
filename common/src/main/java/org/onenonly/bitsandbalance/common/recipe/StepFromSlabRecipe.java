package org.onenonly.bitsandbalance.common.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 2 slabs in a horizontal row -> 4 matching steps,
 * and 2 matching steps in a horizontal row -> 1 slab.
 */
public final class StepFromSlabRecipe extends CustomRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "step_from_slab");

    public StepFromSlabRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !resultFor(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return resultFor(input);
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(List.of());
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of();
    }

    @Override
    public boolean isSpecial() {
        return true;
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
            return out == null ? ItemStack.EMPTY : new ItemStack(out, 4);
        }

        Block slab = EnhancedSlabRecipeLookup.slabForStep(block);
        if (slab == null) return ItemStack.EMPTY;
        Item out = slab.asItem();
        return out == null ? ItemStack.EMPTY : new ItemStack(out, 1);
    }

    private static ItemStack pairedResult(Block block) {
        if (block == null) return ItemStack.EMPTY;

        Block step = EnhancedSlabRecipeLookup.stepForSlab(block);
        if (step != null) {
            Item out = step.asItem();
            return out == null ? ItemStack.EMPTY : new ItemStack(out, 4);
        }

        Block slab = EnhancedSlabRecipeLookup.slabForStep(block);
        if (slab == null) return ItemStack.EMPTY;
        Item out = slab.asItem();
        return out == null ? ItemStack.EMPTY : new ItemStack(out, 1);
    }

    private static Ingredient placementIngredient() {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!pairedResult(block).isEmpty()) {
                Item item = block.asItem();
                if (item != null && item != Items.AIR) {
                    items.add(item);
                }
            }
        }
        if (items.isEmpty()) {
            return Ingredient.of(Items.OAK_SLAB);
        }
        return Ingredient.of(items.toArray(Item[]::new));
    }

    private static RecipeDisplay shapedDisplay(int width, int height, List<SlotDisplay> grid, ItemStack result) {
        SlotDisplay resultSlot = new SlotDisplay.ItemStackSlotDisplay(result);
        SlotDisplay station = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);
        return new ShapedCraftingRecipeDisplay(width, height, grid, resultSlot, station);
    }

    public static final class Serializer implements RecipeSerializer<StepFromSlabRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final StepFromSlabRecipe TEMPLATE = new StepFromSlabRecipe(CraftingBookCategory.BUILDING);
        private static final MapCodec<StepFromSlabRecipe> CODEC = MapCodec.unit(TEMPLATE);
        private static final StreamCodec<RegistryFriendlyByteBuf, StepFromSlabRecipe> STREAM_CODEC = StreamCodec.unit(TEMPLATE);

        private Serializer() {
        }

        @Override
        public MapCodec<StepFromSlabRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StepFromSlabRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

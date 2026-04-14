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
import net.minecraft.world.level.block.Blocks;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Rebuilds the source full block from 2 slabs in a vertical column.
 */
public final class BaseBlockRecoveryRecipe extends CustomRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "base_block_recovery");

    public BaseBlockRecoveryRecipe(CraftingBookCategory category) {
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

        int width = input.width();
        int height = input.height();
        if (!isSupportedShape(width, height)) return ItemStack.EMPTY;

        ItemStack first = input.getItem(0);
        if (first.isEmpty()) return ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            if (!ItemStack.isSameItemSameComponents(first, stack)) return ItemStack.EMPTY;
        }

        Item item = first.getItem();
        if (!(item instanceof BlockItem blockItem)) return ItemStack.EMPTY;

        Block baseBlock = resolveBaseBlock(blockItem.getBlock(), width, height);
        if (baseBlock == null) return ItemStack.EMPTY;

        Item output = baseBlock.asItem();
        if (output == null || output == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(output, 1);
    }

    private static boolean isSupportedShape(int width, int height) {
        return width == 1 && height == 2;
    }

    private static Block resolveBaseBlock(Block inputBlock, int width, int height) {
        if (width != 1 || height != 2) return null;
        return SlabFamilyBlockLookup.getBaseBlockForSlab(inputBlock);
    }

    private static Ingredient placementIngredient() {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (SlabFamilyBlockLookup.getBaseBlockForSlab(block) != null) {
                Item item = block.asItem();
                if (item != null && item != Items.AIR) {
                    items.add(item);
                }
            }
        }
        if (items.isEmpty()) {
            return Ingredient.of(Blocks.OAK_SLAB.asItem());
        }
        return Ingredient.of(items.toArray(Item[]::new));
    }

    private static RecipeDisplay shapedDisplay(int width, int height, List<SlotDisplay> grid, ItemStack result) {
        SlotDisplay resultSlot = new SlotDisplay.ItemStackSlotDisplay(result);
        SlotDisplay station = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);
        return new ShapedCraftingRecipeDisplay(width, height, grid, resultSlot, station);
    }

    public static final class Serializer implements RecipeSerializer<BaseBlockRecoveryRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final BaseBlockRecoveryRecipe TEMPLATE = new BaseBlockRecoveryRecipe(CraftingBookCategory.BUILDING);
        private static final MapCodec<BaseBlockRecoveryRecipe> CODEC = MapCodec.unit(TEMPLATE);
        private static final StreamCodec<RegistryFriendlyByteBuf, BaseBlockRecoveryRecipe> STREAM_CODEC = StreamCodec.unit(TEMPLATE);

        private Serializer() {
        }

        @Override
        public MapCodec<BaseBlockRecoveryRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BaseBlockRecoveryRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
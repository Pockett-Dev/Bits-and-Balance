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
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 1 slab -> 1 vertical slab, and 1 vertical slab -> 1 slab.
 */
public final class VerticalSlabConvertRecipe extends CustomRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_slab_convert");

    public VerticalSlabConvertRecipe(CraftingBookCategory category) {
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

        ItemStack found = ItemStack.EMPTY;
        int nonEmpty = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            nonEmpty++;
            if (nonEmpty > 1) return ItemStack.EMPTY;
            found = s;
        }
        if (nonEmpty != 1 || found.isEmpty()) return ItemStack.EMPTY;

        Item item = found.getItem();
        if (!(item instanceof BlockItem blockItem)) return ItemStack.EMPTY;

        Block block = blockItem.getBlock();
        if (block == null) return ItemStack.EMPTY;

        if (block instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            Item out = fixedVerticalSlab.getSourceSlab().asItem();
            return out == null ? ItemStack.EMPTY : new ItemStack(out, 1);
        }

        Block vertical = EnhancedSlabRecipeLookup.verticalForSlab(block);
        if (vertical != null) {
            Item out = vertical.asItem();
            if (out == null) return ItemStack.EMPTY;
            return new ItemStack(out, 1);
        }

        Block slab = EnhancedSlabRecipeLookup.slabForVertical(block);
        if (slab != null) {
            Item out = slab.asItem();
            if (out == null) return ItemStack.EMPTY;
            return new ItemStack(out, 1);
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack singleItemResult(Block block) {
        if (block == null) return ItemStack.EMPTY;

        if (block instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            Item out = fixedVerticalSlab.getSourceSlab().asItem();
            return out == null ? ItemStack.EMPTY : new ItemStack(out, 1);
        }

        Block vertical = EnhancedSlabRecipeLookup.verticalForSlab(block);
        if (vertical != null) {
            Item out = vertical.asItem();
            return out == null ? ItemStack.EMPTY : new ItemStack(out, 1);
        }

        Block slab = EnhancedSlabRecipeLookup.slabForVertical(block);
        if (slab != null) {
            Item out = slab.asItem();
            return out == null ? ItemStack.EMPTY : new ItemStack(out, 1);
        }

        return ItemStack.EMPTY;
    }

    private static Ingredient placementIngredient() {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!singleItemResult(block).isEmpty()) {
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

    private static RecipeDisplay shapelessDisplay(SlotDisplay ingredient, ItemStack result) {
        SlotDisplay resultSlot = new SlotDisplay.ItemStackSlotDisplay(result);
        SlotDisplay station = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);
        return new ShapelessCraftingRecipeDisplay(List.of(ingredient), resultSlot, station);
    }

    public static final class Serializer implements RecipeSerializer<VerticalSlabConvertRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final VerticalSlabConvertRecipe TEMPLATE = new VerticalSlabConvertRecipe(CraftingBookCategory.BUILDING);

        private static final MapCodec<VerticalSlabConvertRecipe> CODEC = MapCodec.unit(TEMPLATE);
        private static final StreamCodec<RegistryFriendlyByteBuf, VerticalSlabConvertRecipe> STREAM_CODEC = StreamCodec.unit(TEMPLATE);

        private Serializer() {
        }

        @Override
        public MapCodec<VerticalSlabConvertRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VerticalSlabConvertRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

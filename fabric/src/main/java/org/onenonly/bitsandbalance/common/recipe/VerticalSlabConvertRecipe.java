package org.onenonly.bitsandbalance.fabric.recipe.impl;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.recipe.EnhancedSlabRecipeLookup;

import java.util.List;

public final class VerticalSlabConvertRecipe extends CustomRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_slab_convert");
    private final CraftingBookCategory category;

    public VerticalSlabConvertRecipe(CraftingBookCategory category) {
        super(category);
        this.category = category;
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
    public CraftingBookCategory category() {
        return this.category;
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
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            nonEmpty++;
            if (nonEmpty > 1) return ItemStack.EMPTY;
            found = stack;
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
            return out == null ? ItemStack.EMPTY : new ItemStack(out, 1);
        }

        Block slab = EnhancedSlabRecipeLookup.slabForVertical(block);
        if (slab != null) {
            Item out = slab.asItem();
            return out == null ? ItemStack.EMPTY : new ItemStack(out, 1);
        }

        return ItemStack.EMPTY;
    }

    public static final class Serializer {
        private static final VerticalSlabConvertRecipe TEMPLATE = new VerticalSlabConvertRecipe(CraftingBookCategory.BUILDING);
        private static final MapCodec<VerticalSlabConvertRecipe> CODEC = MapCodec.unit(TEMPLATE);
        private static final StreamCodec<RegistryFriendlyByteBuf, VerticalSlabConvertRecipe> STREAM_CODEC = StreamCodec.unit(TEMPLATE);

        public static final RecipeSerializer<VerticalSlabConvertRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);

        private Serializer() {
        }
    }
}

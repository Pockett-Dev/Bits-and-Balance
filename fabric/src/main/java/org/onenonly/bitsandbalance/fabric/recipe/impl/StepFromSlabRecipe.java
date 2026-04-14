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
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.fabric.recipe.FabricRecipeSerializerFactory;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.recipe.EnhancedSlabRecipeLookup;

import java.util.List;

public final class StepFromSlabRecipe implements CraftingRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "step_from_slab");
    private final CraftingBookCategory category;

    public StepFromSlabRecipe(CraftingBookCategory category) {
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

    public ItemStack assemble(RecipeInput input) {
        return input instanceof CraftingInput craftingInput ? resultFor(craftingInput) : ItemStack.EMPTY;
    }

    public ItemStack assemble(CraftingInput input) {
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
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeType<CraftingRecipe> getType() {
        return RecipeType.CRAFTING;
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

    public static final class Serializer {
        private static final StepFromSlabRecipe TEMPLATE = new StepFromSlabRecipe(CraftingBookCategory.BUILDING);
        private static final MapCodec<StepFromSlabRecipe> CODEC = MapCodec.unit(TEMPLATE);
        private static final StreamCodec<RegistryFriendlyByteBuf, StepFromSlabRecipe> STREAM_CODEC = StreamCodec.unit(TEMPLATE);

        public static final RecipeSerializer<StepFromSlabRecipe> INSTANCE = FabricRecipeSerializerFactory.create(CODEC, STREAM_CODEC);

        private Serializer() {
        }
    }
}
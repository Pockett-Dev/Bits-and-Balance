package org.onenonly.bitsandbalance.fabric.recipe.impl;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.recipe.EnhancedSlabRecipeLookup;
import org.onenonly.bitsandbalance.common.recipe.VerticalSlabRecipeInputs;

import java.util.List;

public final class VerticalSlabFromPlanksRecipe extends CustomRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_slab_from_planks");
    private final CraftingBookCategory category;

    public VerticalSlabFromPlanksRecipe(CraftingBookCategory category) {
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
        if (input == null) return ItemStack.EMPTY;
        int width = input.width();
        int height = input.height();
        if (height != 3) return ItemStack.EMPTY;

        for (int col = 0; col < width; col++) {
            ItemStack a = input.getItem(col);
            ItemStack b = input.getItem(col + width);
            ItemStack c = input.getItem(col + 2 * width);

            if (a.isEmpty() || b.isEmpty() || c.isEmpty()) continue;
            if (!ItemStack.isSameItem(a, b) || !ItemStack.isSameItem(a, c)) continue;

            boolean othersClear = true;
            for (int i = 0; i < width * height; i++) {
                if (i == col || i == col + width || i == col + 2 * width) continue;
                if (!input.getItem(i).isEmpty()) {
                    othersClear = false;
                    break;
                }
            }
            if (!othersClear) continue;

            Item verticalItem = findVerticalSlabItemFor(a.getItem());
            if (verticalItem != null) return new ItemStack(verticalItem, 6);
            break;
        }

        return ItemStack.EMPTY;
    }

    static @Nullable Item bitsandbalance$displayInputForSlab(Block slabBlock) {
        return VerticalSlabRecipeInputs.displayInputForSlab(slabBlock);
    }

    private static @Nullable Item findVerticalSlabItemFor(Item inputItem) {
        Identifier id = BuiltInRegistries.ITEM.getKey(inputItem);
        if (id == null) return null;
        String ns = id.getNamespace();
        String path = id.getPath();

        java.util.List<Identifier> candidates = new java.util.ArrayList<>();
        if (path.endsWith("_planks")) {
            candidates.add(Identifier.fromNamespaceAndPath(ns, path.substring(0, path.length() - "_planks".length()) + "_slab"));
        }
        if (path.endsWith("_bricks")) {
            candidates.add(Identifier.fromNamespaceAndPath(ns, path.substring(0, path.length() - "_bricks".length()) + "_brick_slab"));
        }
        if (path.endsWith("_block")) {
            candidates.add(Identifier.fromNamespaceAndPath(ns, path.substring(0, path.length() - "_block".length()) + "_slab"));
        }
        candidates.add(Identifier.fromNamespaceAndPath(ns, path + "_slab"));

        for (Identifier candidate : candidates) {
            Block slabBlock = BuiltInRegistries.BLOCK.getOptional(candidate).orElse(null);
            if (slabBlock == null) continue;
            Block verticalBlock = EnhancedSlabRecipeLookup.verticalForSlab(slabBlock);
            if (verticalBlock == null) continue;
            Item verticalItem = verticalBlock.asItem();
            if (verticalItem != null && verticalItem != Items.AIR) return verticalItem;
        }
        return null;
    }

    public static final class Serializer {
        private static final VerticalSlabFromPlanksRecipe TEMPLATE = new VerticalSlabFromPlanksRecipe(CraftingBookCategory.BUILDING);
        private static final MapCodec<VerticalSlabFromPlanksRecipe> CODEC = MapCodec.unit(TEMPLATE);
        private static final StreamCodec<RegistryFriendlyByteBuf, VerticalSlabFromPlanksRecipe> STREAM_CODEC = StreamCodec.unit(TEMPLATE);

        public static final RecipeSerializer<VerticalSlabFromPlanksRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);

        private Serializer() {
        }
    }
}

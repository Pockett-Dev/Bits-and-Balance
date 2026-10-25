package org.onenonly.bitsandbalance.fabric.recipe.impl;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
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
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

import java.util.List;

public final class BaseBlockRecoveryRecipe implements CraftingRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "base_block_recovery");
    private final CraftingBookCategory category;

    public BaseBlockRecoveryRecipe(CraftingBookCategory category) {
        this.category = category;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return false;
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
        return ItemStack.EMPTY;
    }

    public static final class Serializer {
        private static final BaseBlockRecoveryRecipe TEMPLATE = new BaseBlockRecoveryRecipe(CraftingBookCategory.BUILDING);
        private static final MapCodec<BaseBlockRecoveryRecipe> CODEC = MapCodec.unit(TEMPLATE);
        private static final StreamCodec<RegistryFriendlyByteBuf, BaseBlockRecoveryRecipe> STREAM_CODEC = StreamCodec.unit(TEMPLATE);

        public static final RecipeSerializer<BaseBlockRecoveryRecipe> INSTANCE = FabricRecipeSerializerFactory.create(CODEC, STREAM_CODEC);

        private Serializer() {
        }
    }
}

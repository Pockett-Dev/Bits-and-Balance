package org.onenonly.bitsandbalance.common.recipe;

import com.mojang.serialization.MapCodec;
//? if mc != 26.1 && mc != 26.2 && mc != 26.3
import net.minecraft.core.HolderLookup;
//? endif
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
//? if mc != 1.21.1
import net.minecraft.world.item.crafting.PlacementInfo;
//? endif
import net.minecraft.world.item.crafting.RecipeSerializer;
//? if mc != 1.21.1
import net.minecraft.world.item.crafting.display.RecipeDisplay;
//? endif
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

//? if mc != 1.21.1
import java.util.List;
//? endif

/**
 * Inert compatibility serializer for obsolete slab-to-full-block recipes.
 *
 * <p>The serializer remains registered so an upgraded world can parse and then
 * delete stale generated datapack files without failing the first reload. No
 * current datapack registers this recipe, and this tombstone never matches or
 * produces an output.</p>
 */
public final class BaseBlockRecoveryRecipe extends CustomRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "base_block_recovery");
    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private final CraftingBookCategory category;
    //? endif

    public BaseBlockRecoveryRecipe(CraftingBookCategory category) {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        this.category = category;
        //? else
        super(category);
        //? endif
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return false;
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
        return false;
    }
    //? else
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(List.of());
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of();
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
        return true;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    private static ItemStack resultFor(CraftingInput input) {
        return ItemStack.EMPTY;
    }

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    public static final class Serializer {
    //? else
    public static final class Serializer implements RecipeSerializer<BaseBlockRecoveryRecipe> {
    //? endif
        //? if mc != 26.1 && mc != 26.2 && mc != 26.3
        public static final Serializer INSTANCE = new Serializer();
        //? endif

        private static final BaseBlockRecoveryRecipe TEMPLATE = new BaseBlockRecoveryRecipe(CraftingBookCategory.BUILDING);
        private static final MapCodec<BaseBlockRecoveryRecipe> CODEC = MapCodec.unit(TEMPLATE);
        private static final StreamCodec<RegistryFriendlyByteBuf, BaseBlockRecoveryRecipe> STREAM_CODEC = StreamCodec.unit(TEMPLATE);

        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        public static final RecipeSerializer<BaseBlockRecoveryRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);
        //? endif

        private Serializer() {
        }

        //? if mc != 26.1 && mc != 26.2 && mc != 26.3
        @Override
        public MapCodec<BaseBlockRecoveryRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BaseBlockRecoveryRecipe> streamCodec() {
            return STREAM_CODEC;
        }
        //? endif
    }
}

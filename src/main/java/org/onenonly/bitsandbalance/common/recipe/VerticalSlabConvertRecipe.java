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
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
//? endif
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;

//? if mc != 1.21.1
import java.util.LinkedHashSet;
import java.util.List;
//? endif

/**
 * 1 slab -> 1 vertical slab, and 1 vertical slab -> 1 slab.
 */
public final class VerticalSlabConvertRecipe extends CustomRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_slab_convert");
    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private final CraftingBookCategory category;
    //? endif

    public VerticalSlabConvertRecipe(CraftingBookCategory category) {
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
        return width >= 1 && height >= 1;
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
            return isRealItem(out) ? new ItemStack(out, 1) : ItemStack.EMPTY;
        }

        Block vertical = EnhancedSlabRecipeLookup.verticalForSlab(block);
        if (vertical != null) {
            Item out = vertical.asItem();
            return isRealItem(out) ? new ItemStack(out, 1) : ItemStack.EMPTY;
        }

        Block slab = EnhancedSlabRecipeLookup.slabForVertical(block);
        if (slab != null) {
            Item out = slab.asItem();
            return isRealItem(out) ? new ItemStack(out, 1) : ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }

    private static boolean isRealItem(Item item) {
        return item != null && item != Items.AIR;
    }

    //? if mc != 1.21.1
    private static ItemStack singleItemResult(Block block) {
        if (block == null) return ItemStack.EMPTY;

        if (block instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            Item out = fixedVerticalSlab.getSourceSlab().asItem();
            return isRealItem(out) ? new ItemStack(out, 1) : ItemStack.EMPTY;
        }

        Block vertical = EnhancedSlabRecipeLookup.verticalForSlab(block);
        if (vertical != null) {
            Item out = vertical.asItem();
            return isRealItem(out) ? new ItemStack(out, 1) : ItemStack.EMPTY;
        }

        Block slab = EnhancedSlabRecipeLookup.slabForVertical(block);
        if (slab != null) {
            Item out = slab.asItem();
            return isRealItem(out) ? new ItemStack(out, 1) : ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }

    private static Ingredient placementIngredient() {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!singleItemResult(block).isEmpty()) {
                Item item = block.asItem();
                if (isRealItem(item)) {
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
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        SlotDisplay resultSlot = new SlotDisplay.ItemStackSlotDisplay(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(result));
        //? else
        SlotDisplay resultSlot = new SlotDisplay.ItemStackSlotDisplay(result);
        //? endif
        SlotDisplay station = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);
        return new ShapelessCraftingRecipeDisplay(List.of(ingredient), resultSlot, station);
    }
    //? endif

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    public static final class Serializer {
    //? else
    public static final class Serializer implements RecipeSerializer<VerticalSlabConvertRecipe> {
    //? endif
        //? if mc != 26.1 && mc != 26.2 && mc != 26.3
        public static final Serializer INSTANCE = new Serializer();
        //? endif

        private static final VerticalSlabConvertRecipe TEMPLATE = new VerticalSlabConvertRecipe(CraftingBookCategory.BUILDING);
        private static final MapCodec<VerticalSlabConvertRecipe> CODEC = MapCodec.unit(TEMPLATE);
        private static final StreamCodec<RegistryFriendlyByteBuf, VerticalSlabConvertRecipe> STREAM_CODEC = StreamCodec.unit(TEMPLATE);

        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        public static final RecipeSerializer<VerticalSlabConvertRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);
        //? endif

        private Serializer() {
        }

        //? if mc != 26.1 && mc != 26.2 && mc != 26.3
        @Override
        public MapCodec<VerticalSlabConvertRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VerticalSlabConvertRecipe> streamCodec() {
            return STREAM_CODEC;
        }
        //? endif
    }
}
